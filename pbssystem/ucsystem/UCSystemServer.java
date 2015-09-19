package be.kuleuven.cs.ucsystem;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.zurich.idmx.dm.Commitment;
import com.ibm.zurich.idmx.showproof.Proof;
import com.ibm.zurich.idmx.showproof.ProofSpec;
import com.ibm.zurich.idmx.showproof.Verifier;

import be.kuleuven.cs.pbs.PBSMessage;
import be.kuleuven.cs.pbs.PBSSigner;
import be.kuleuven.cs.priman.connection.Connection;
import be.kuleuven.cs.priman.exception.ConnectionException;
import com.ibm.zurich.idmx.utils.SystemParameters;

public class UCSystemServer {

	private PBSSigner pbsSigner;
	private HashMap <String, UCSystemUtil.redeemRespVals> lpNoRedeemed;
    private String epoch;

	public UCSystemServer(BigInteger p, BigInteger q, BigInteger g, BigInteger x, String epoch){
		this.pbsSigner = new PBSSigner(p, q, g, x);
        this.lpNoRedeemed = new HashMap<String, UCSystemUtil.redeemRespVals>();
        this.epoch = epoch;
	}
	

    public void issueLoyaltyPoints(Connection conn, List<Integer> amountList, String epoch)
            throws ConnectionException{


        long start = System.currentTimeMillis();

        //info = amount;epoch
        List<PBSMessage> pbsMsg1List = pbsSigner.step1(amountList, epoch);
        conn.send(pbsMsg1List);
        List<PBSMessage> pbsMsg2List = (List<PBSMessage>) conn.receive();
        List<PBSMessage> pbsMsg3List = pbsSigner.step2(pbsMsg2List);

        for(PBSMessage pbsMsg : pbsMsg3List)
            Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).
                    log(Level.INFO, pbsMsg.getInfo() + " loyalty points issued!");

        long end = System.currentTimeMillis();
        System.out.println("T1MER Server issued points:,"+(end-start));
        
        //added:
        Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).info("TIMER Server issued points:,"+(end-start));

        
        conn.send(pbsMsg3List);

    }

    /**
     *
     * @param conn
     * @param spec
     * @return
     * @throws ConnectionException
     */
    public boolean redeemLoyaltyPointsSingleProofNonce(Connection conn, ProofSpec spec)
            throws ConnectionException {

        SystemParameters sp = spec.getGroupParams().getSystemParams();

        // Generate and send a single nonce
        BigInteger nonce = Verifier.getNonce(sp);
        conn.send(nonce);

        UCSystemMessage ucsMsg1 = (UCSystemMessage)conn.receive();


        // Check that the nonce is correct
        if ( !ucsMsg1.getNonce().equals(nonce) ){
            String commStr = null;
            for(LoyaltyPointRedeem lp : ucsMsg1.getLpRedeemList()){
                commStr = lp.getComm().toString();
                lpNoRedeemed.put(commStr, UCSystemUtil.redeemRespVals.BAD_NONCE_SINGLE_PROOF);
            }
            try {
                conn.send(new UCSystemRespMsg(lpNoRedeemed,true));
                lpNoRedeemed.clear();
                return false;
            } catch (ConnectionException e) {
                Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.SEVERE,
                        "ConnectionException");
            }
        }

        return redeemLoyaltyPointsSingleProof(conn, ucsMsg1.getLpRedeemList(),
                ucsMsg1.getAmountToRedeem(), spec,
                ucsMsg1.getProof(), nonce, ucsMsg1.getCommName());
    }



    public boolean redeemLoyaltyPointsSingleProof(Connection conn,
                                                  List<LoyaltyPointRedeem> lpRedeemList,
                                                  int amountToRedeem,
                                                  ProofSpec spec, Proof proof,
                                                  BigInteger nonce, String commName){

        Boolean redeemErrors = false;
        String commStr=null;
        Boolean singleProofFailure = false;

        // Check single ownership proof

        if(!lpCheckOwnershipSingleProof(lpRedeemList, spec, proof, nonce, commName))
            singleProofFailure = true;

        // Check individual points
        for(LoyaltyPointRedeem lp : lpRedeemList){

            commStr = lp.getComm().toString();

            if (singleProofFailure){
                lpNoRedeemed.put(commStr, UCSystemUtil.redeemRespVals.BAD_OWNERSHIP_SINGLE_PROOF);
                redeemErrors = true;
                continue;
            }

            else if (!lpCheckRedeemedDB(lp)){
                // Already redeemed
                lpNoRedeemed.put(commStr, UCSystemUtil.redeemRespVals.ALREADY_USED);
                redeemErrors = true;
                Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).
                        log(Level.SEVERE, "LP already used");
                continue;
            }
            else if (!lpCheckSignature(lp)){
                //Bad signature
                lpNoRedeemed.put(commStr, UCSystemUtil.redeemRespVals.BAD_SIGNATURE);
                redeemErrors = true;
                Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).
                        log(Level.SEVERE, "Bad PBS signature");
                continue;
            }
            else if(!lpCheckEpoch(lp)){
                //Bad epoch
                lpNoRedeemed.put(commStr, UCSystemUtil.redeemRespVals.BAD_EPOCH);
                redeemErrors= true;
                Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).
                        log(Level.SEVERE, "Bad epoch");
                continue;
            }else {
                // loyalty point OK

                lpAddtoRedeemedDB(lp);
                redeemErrors = false;
             }
        }


        try {
            conn.send(new UCSystemRespMsg(lpNoRedeemed,redeemErrors));
            lpNoRedeemed.clear();

            int amountInRedeemList = UCSystemUtil.getLpListTotalAmount(lpRedeemList);

            // Provide change back if needed and if there are not redeeming errors
            if(!redeemErrors && amountInRedeemList>amountToRedeem ){
                Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).info("Change back required:" +
                        (amountInRedeemList-amountToRedeem)+ " points" );
                UCSystemMessage ucsMsg = (UCSystemMessage) conn.receive();
                issueLoyaltyPoints(conn, ucsMsg.getAmountList(), epoch);
            }

        } catch (ConnectionException e) {
            Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.SEVERE,
                    "ConnectionException", e);
        }

        return redeemErrors;

    }




    /**
     *  Redeem points but server first generates and sends a list of nonces for each
     *  loyalty point proof. One proof per loyalty point.
     *
     * @param conn
     * @param spec
     * @param nonceListSize
     * @return
     * @throws ConnectionException
     */
    public boolean redeemLoyaltyPointsNonce(Connection conn, ProofSpec spec,
                                       int nonceListSize)
            throws ConnectionException {

        SystemParameters sp = spec.getGroupParams().getSystemParams();
        List<BigInteger> nonceList = new ArrayList<BigInteger>();

        for(int i=0; i< nonceListSize; ++i)
            nonceList.add(Verifier.getNonce(sp));

        conn.send(nonceList);

        UCSystemMessage ucsMsg1 = (UCSystemMessage)conn.receive();

        lpCheckNonces(ucsMsg1.getLpRedeemList(), nonceList);

        return redeemLoyaltyPoints(conn, ucsMsg1.getLpRedeemList(),
                ucsMsg1.getAmountToRedeem(), spec);

    }

    /**
     *  Redeem points without server generated nonce. One proof
     *  per loyalty point. The server relies only on the
     *  redeemed points log to detect double-spending. This approach
     *  allows pre-computing and caching of loyalty point's proofs
     *  on the client for better performance.
     *
     * @param conn
     * @param lpRedeemList
     * @param spec
     * @return
     */
    public boolean redeemLoyaltyPoints(Connection conn, List<LoyaltyPointRedeem> lpRedeemList,
                                       int amountToRedeem, ProofSpec spec){

        Boolean redeemErrors = false;
        String commStr=null;

        for(LoyaltyPointRedeem lp : lpRedeemList){

            commStr = lp.getComm().toString();

            if (!lpCheckRedeemedDB(lp)){
                // Already redeemed
                lpNoRedeemed.put(commStr, UCSystemUtil.redeemRespVals.ALREADY_USED);
                redeemErrors = true;
                Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).
                        log(Level.SEVERE, "LP already used");
                continue;
            }
            else if (!lpCheckSignature(lp)){
                //Bad signature
                lpNoRedeemed.put(commStr, UCSystemUtil.redeemRespVals.BAD_SIGNATURE);
                redeemErrors = true;
                Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).
                        log(Level.SEVERE, "Bad PBS signature");
                continue;
            }
            else if(!lpCheckEpoch(lp)){
                //Bad epoch
                lpNoRedeemed.put(commStr, UCSystemUtil.redeemRespVals.BAD_EPOCH);
                redeemErrors= true;
                Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).
                        log(Level.SEVERE, "Bad epoch");
                continue;
            }
            else if( !lpCheckOwnership(lp, spec) ){
                // Ownership proof failed
                lpNoRedeemed.put(commStr, UCSystemUtil.redeemRespVals.BAD_OWNERSHIP);
                redeemErrors = true;
                Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).
                        log(Level.SEVERE, "Bad ownership proof");
                continue;
            }else {
                // loyalty point OK

                lpAddtoRedeemedDB(lp);
                redeemErrors = false;
                Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).info("LP successfully redeemed");
            }
        }

        try {
            conn.send(new UCSystemRespMsg(lpNoRedeemed,redeemErrors));
            lpNoRedeemed.clear();

            int amountInRedeemList = UCSystemUtil.getLpListTotalAmount(lpRedeemList);

            // Provide change back if needed and if there are not redeeming errors
            if(!redeemErrors && amountInRedeemList>amountToRedeem ){
                Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).info("Change back required:" +
                        (amountInRedeemList-amountToRedeem)+ " points" );
                UCSystemMessage ucsMsg = (UCSystemMessage) conn.receive();
                issueLoyaltyPoints(conn, ucsMsg.getAmountList(), epoch);
            }
        } catch (ConnectionException e) {
            Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.SEVERE,
                    "ConnectionException", e);
        }

        return redeemErrors;
    }

    private void lpAddtoRedeemedDB(LoyaltyPoint lp){


    }

    private boolean lpCheckNonces(List<LoyaltyPointRedeem> lpRedeemList,
                                  List<BigInteger> nonceList){

        boolean nonceListOK = true;

        for( LoyaltyPointRedeem lp : lpRedeemList ){

            if( !nonceList.contains(lp.getNonce()) ){
                nonceListOK = false;
                lpNoRedeemed.put(lp.getComm().toString(),
                        UCSystemUtil.redeemRespVals.BAD_NONCE);
                Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).
                        log(Level.SEVERE, "Bad nonce detected");
            }
        }
        return nonceListOK;
    }

    private boolean lpCheckOwnership(LoyaltyPointRedeem lp, ProofSpec spec){

        String commName = UCSystemUtil.getCommNameFromProofSpec(spec);

        // Verify proof: ownership of valid credential and commitment
        HashMap<String, Commitment> vCommitments = new HashMap<String, Commitment>();
        vCommitments.put(commName, lp.getComm());
        Verifier verifier = new Verifier(spec, lp.getProof(), lp.getNonce(), null,
                vCommitments, null, null);

        if (!verifier.verify()){
            Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).
                    log(Level.SEVERE, "Idemix proof verification failed");
            return false;
        }

        return true;
    }

    private boolean lpCheckOwnershipSingleProof(List<LoyaltyPointRedeem> lpRedeemList,
                                                ProofSpec spec, Proof proof,
                                                BigInteger nonce, String commName){

        // Verify proof: ownership of valid credential and commitment
        HashMap<String, Commitment> vCommitments = new HashMap<String, Commitment>();

        for(int i=0; i<lpRedeemList.size(); ++i){
            if(i ==0) // default comm does not have index
                vCommitments.put(commName, lpRedeemList.get(i).getComm());
            else
                vCommitments.put(commName+i, lpRedeemList.get(i).getComm());
        }

        long start = System.currentTimeMillis();
        Verifier verifier = new Verifier(spec, proof, nonce, null,
                vCommitments, null, null);

        if (!verifier.verify()){
            Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).
                    log(Level.SEVERE, "Idemix single-proof verification failed");
            long end = System.currentTimeMillis();
            System.out.println("Verify Idemix FAILED:"+(end-start));
            return false;
        }else {
            Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).
                    log(Level.SEVERE, "Idemix single-proof verification succeeded");
        }
        long end = System.currentTimeMillis();
        System.out.println("T1MER Verify Idemix:"+(end-start));
        
        //added:
        Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).info("TIMER Verify Idemix:"+(end-start));

        return true;
    }

    private boolean lpCheckSignature(LoyaltyPointRedeem lp){
     	long start = System.currentTimeMillis();
        //Verify if PBS system parameters are the same
     	if( !lp.getSignature().verifyParams(pbsSigner.getP(), pbsSigner.getQ(),
                pbsSigner.getG(), pbsSigner.getY()) ){
            long end = System.currentTimeMillis();
            System.out.println("Check sigFailed:"+(end-start));
            return false;
        }

        if ( !lp.getSignature().verifySignature() ){
            Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).
                    log(Level.SEVERE, "PBS signature verification failed");

            long end = System.currentTimeMillis();
            System.out.println("Check sig FAILED:"+(end-start));
            return false;
        }
        long end = System.currentTimeMillis();
        System.out.println("Check sig:"+(end-start));

        return true;
    }

    public String getEpoch() {
        return epoch;
    }

    public boolean lpCheckEpoch(LoyaltyPoint lp){
        return true;
    }

     public boolean lpCheckRedeemedDB(LoyaltyPoint lp){

         return true;
     }



}
