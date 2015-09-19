package be.kuleuven.cs.ucsystem;

import be.kuleuven.cs.pbs.PBSMessage;
import be.kuleuven.cs.pbs.PBSSignature;
import be.kuleuven.cs.pbs.PBSUser;
import be.kuleuven.cs.priman.connection.Connection;
import be.kuleuven.cs.priman.exception.ConnectionException;
import be.kuleuven.cs.ucsystem.UCSystemMessage.UCSCmdVals;

import com.google.common.base.Stopwatch;
import com.ibm.zurich.idmx.dm.CommitmentOpening;
import com.ibm.zurich.idmx.dm.Credential;
import com.ibm.zurich.idmx.dm.MasterSecret;
import com.ibm.zurich.idmx.key.IssuerPublicKey;
import com.ibm.zurich.idmx.showproof.*;
import com.ibm.zurich.idmx.showproof.predicates.CommitmentPredicate;
import com.ibm.zurich.idmx.showproof.predicates.Predicate;
import com.ibm.zurich.idmx.utils.SystemParameters;

import java.math.BigInteger;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/*
This is basically a wrapper class for PBUser to implement UCSystem
*/
public class UCSystemClient {

	private PBSUser pbsUser;
    private int counterEarning;
    private int counterRedeeming;

	public UCSystemClient(PBSUser pbsUser) {
		this.pbsUser = pbsUser;
        this.counterEarning =0;
        this.counterRedeeming=0;
	}
	
	
	/**
	 * PBS global parameters as BigIntegers
	 * @param p
	 * @param q
	 * @param g
	 * @param y
	 */
	public UCSystemClient(BigInteger p, BigInteger q, BigInteger g, BigInteger y){
		this.pbsUser = new PBSUser( p, q, g, y );
        this.counterEarning =0;
	}

	// This method is executed after a successful payment by the user. The server
	// returns the amount of loyalty points.  
	// We also need some concept of state, based on the shopping card id. We need to keep the connection alive.
	// What if the TLS connection fails. A new one should be established, shopping card ID can be used as a cookie. 


    public List<LoyaltyPointClient> getLoyaltyPoint(Connection conn,
                                                    Credential cred,
                                                    List<Integer> amountList,
                                                    boolean sendFirstMessage)
            throws ConnectionException{



        List<LoyaltyPointClient> lpClientList = new ArrayList<LoyaltyPointClient>();
        List<CommitmentOpening> commOpeningList = new ArrayList<CommitmentOpening>();
        List<String> commStrList = new ArrayList<String>();

        // Send the first message with the amount list only if it has not been sent
        // just before calling this method!
        if(sendFirstMessage){
            UCSystemMessage ucsMsg0 = new UCSystemMessage(UCSCmdVals.GET_POINTS, amountList);
            conn.send(ucsMsg0);
        }

        List<PBSMessage> pbsMsg1List = (List<PBSMessage>) conn.receive();

        Stopwatch stopwatch = Stopwatch.createStarted();
        for (PBSMessage pbsMsg1 : pbsMsg1List){
            CommitmentOpening commOpening = genCommitment(cred);
            commStrList.add(commOpening.getCommitment().toString());
            commOpeningList.add(commOpening);
        }
        stopwatch.stop();
        String  tComm = stopwatch.toString();
        stopwatch.reset();

        // We assume the same epoch for all the points to be issued
        String epoch = getEpoch(pbsMsg1List.get(0));

        stopwatch.start();
        List<PBSMessage> pbsMsg2List = pbsUser.step1(pbsMsg1List, commStrList);

        stopwatch.stop();
        String tStep1 = stopwatch.toString();
        stopwatch.reset();

        conn.send(pbsMsg2List);

        List<PBSMessage>  pbsMsg3List = (List<PBSMessage>) conn.receive();

        stopwatch.start();
        List<PBSSignature> pbsSignatureList = pbsUser.step2(pbsMsg3List);

        stopwatch.stop();
        String tStep2 = stopwatch.toString();

        for (int i=0; i<pbsSignatureList.size(); ++i){

            lpClientList.add(new LoyaltyPointClient(
                    pbsSignatureList.get(i),
                    commOpeningList.get(i),
                    amountList.get(i),
                    epoch
            ));
        }

        Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).info("Earning: " + counterEarning + ", "+ tComm
        + ", " + tStep1 + ", " + tStep2);
        
        //added:        
        Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).info("TIMER Earning: " + counterEarning + ", tComm, tStep1, tStep2, "+ tComm
        + ", " + tStep1 + ", " + tStep2);
        
        counterEarning++;
        //added:
        System.out.println("T1MER Earning: " + counterEarning + ", tComm, tStep1, tStep2, "+ tComm
        + ", " + tStep1 + ", " + tStep2);


        return lpClientList;

    }


    /**
     *
     * @param conn
     * @param cred
     * @param spec
     * @param lpClientList
     * @param amountToRedeem
     * @param ms
     * @return  Returns a list of newly issued loyalty points if change back is needed, null otherwise
     * @throws ConnectionException
     */
    public List<LoyaltyPointClient> redeemLoyaltyPointSingleProof(Connection conn, Credential cred,
                                                      ProofSpec spec,
                                                      List<LoyaltyPointClient> lpClientList,
                                                      int amountToRedeem,
                                                      MasterSecret ms)
            throws ConnectionException {

        SystemParameters sp  = cred.getPublicKey().getGroupParams()
                .getSystemParams();
        BigInteger nonce = Verifier.getNonce(sp);

        return doRedeemSingleProof(conn, cred, spec, lpClientList, amountToRedeem,
                ms, nonce, false);
    }


    /**
     *
     * @param conn
     * @param cred
     * @param spec
     * @param lpClientList
     * @param amountToRedeem
     * @param ms
     * @return Returns a list of newly issued loyalty points if change back is needed, null otherwise
     * @throws ConnectionException
     */
    public List<LoyaltyPointClient> redeemLoyaltyPointSingleProofNonce(Connection conn, Credential cred,
                                              ProofSpec spec,
                                              List<LoyaltyPointClient> lpClientList,
                                              int amountToRedeem,
                                              MasterSecret ms,
                                              boolean sendFirtsMessage)
            throws ConnectionException {

        if(sendFirtsMessage){
            UCSystemMessage ucsMsg0 = new UCSystemMessage(
                    UCSCmdVals.REDEEM_POINTS_SINGLE_PROOF_NONCE,
                    spec, lpClientList.size());
            conn.send(ucsMsg0);
        }

        // Get nonce from the server
        BigInteger nonce = (BigInteger) conn.receive();

        return doRedeemSingleProof(conn, cred, spec, lpClientList, amountToRedeem,
                ms, nonce, true);
    }


    private List<LoyaltyPointClient> doRedeemSingleProof(Connection conn, Credential cred,
                                        ProofSpec spec,
                                        List<LoyaltyPointClient> lpClientList,
                                        int amountToRedeem,
                                        MasterSecret ms, BigInteger nonce,
                                        boolean useSrvNonce)
            throws ConnectionException {

       // Stopwatch stopwatch = Stopwatch.createStarted();
        //attribute identifier used for adding commitment predicates. See the
        // proof spec file
        final String predIdentifierName = "ID3";

        List<LoyaltyPointRedeem> lpRedeemList = new ArrayList<LoyaltyPointRedeem>();
        String credTempName = UCSystemUtil.getCredTempNameFromProofSpec(spec);
        String commName =  UCSystemUtil.getCommNameFromProofSpec(spec);


        //Add commitment predicates. Proof spec have one by default
        addCommitmentPredicates(spec, commName, predIdentifierName,
                lpClientList.size()-1);

        // Create loyalty points without proof and nonce
        for(int i = 0; i< lpClientList.size(); ++i) {

            lpRedeemList.add( new LoyaltyPointRedeem(
                    lpClientList.get(i).getLoyaltyPointObject(),
                    null, null ) );
        }

        // Now, create a single proof for all the commitments

        //We assume a single loyalty credential
        HashMap<String, Credential> creds;
        creds = new HashMap<String, Credential>();
        creds.put(credTempName, cred);

        HashMap<String, CommitmentOpening> pCommitments;
        pCommitments = new HashMap<String, CommitmentOpening>();

        for(int i=0; i<lpClientList.size(); ++i){
            if(i == 0) // default comm name does not have index
                pCommitments.put(commName, lpClientList.get(i).getCommOpening());
            else
                pCommitments.put(commName+i, lpClientList.get(i).getCommOpening());
        }

        Stopwatch stopwatch = Stopwatch.createStarted();

        long start = System.currentTimeMillis();
        
        Prover prover = new Prover(ms, creds, null, pCommitments, null, null);

        Proof proof = prover.buildProof(nonce, spec);

        stopwatch.stop();
        
        long end = System.currentTimeMillis();
        System.out.println("T1MER (proof creation) :"+(end-start));
        
        //added:
        Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).info("TIMER Proof creation: " + (end-start) );

        
        UCSCmdVals cmd = null;

        if( useSrvNonce )
            cmd = UCSCmdVals.REDEEM_POINTS_SINGLE_PROOF_NONCE;
        else
            cmd = UCSCmdVals.REDEEM_POINTS_SINGLE_PROOF;

        UCSystemMessage ucsMsg1 = new UCSystemMessage(cmd, spec, proof, nonce,
                lpRedeemList, amountToRedeem, commName);

       // stopwatch.stop();

        Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).info("Redeeming (single): " + counterRedeeming + ", "+ stopwatch);
        counterRedeeming++;

        conn.send(ucsMsg1);

        UCSystemRespMsg redeemResp = (UCSystemRespMsg) conn.receive();

        if(redeemResp.areRedeemErrors()){

            HashMap <String, UCSystemUtil.redeemRespVals> lpNoRedeemed =
                    redeemResp.getLpNoRedeemed();

            Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).
                    log(Level.SEVERE, "Errors while redeeming LP");
            return null;
        }
        Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).info("All LP successfully redeemed");

        //If there are not errors during redeeming, then check if change is needed

        int amountInRedeemList = UCSystemUtil.getLpListTotalAmount(lpRedeemList);

        if(  amountInRedeemList > amountToRedeem){
            //Request change back. The server should know already that change is due to the user
            Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).info("Change back required:" +
                    (amountInRedeemList-amountToRedeem)+ " points" );
            List<Integer> amountChangeList = UCSystemUtil.getLpAmountList(
                    amountInRedeemList-amountToRedeem);
            return  getLoyaltyPoint(conn, cred, amountChangeList, true);
        }

        return null;
    }

    /**
     *  Redeems loyalty points using a single, client-generated nonce. Also, one
     *  proof is generated per point.
     *
     * @param conn
     * @param cred
     * @param spec
     * @param spec
     * @param lpClientList
     * @param ms
     * @return
     * @throws ConnectionException
     */
    public List<LoyaltyPointClient> redeemLoyaltyPoint(Connection conn, Credential cred,
                                   ProofSpec spec,
                                   List<LoyaltyPointClient> lpClientList,
                                   int amountToRedeem, MasterSecret ms)
            throws ConnectionException{

        SystemParameters sp  = cred.getPublicKey().getGroupParams()
                .getSystemParams();
        UCSystemNonce nonceObj = new UCSystemNonce(Verifier.getNonce(sp));


        return doRedeem(conn, cred, spec, lpClientList, amountToRedeem, ms, nonceObj);
    }

    /**
     * Redeems loyalty points using a list of server-generated nonces; once nonce
     * per point. Also, one proof is generated per point.
     *
     * @param conn
     * @param cred
     * @param spec
     * @param spec
     * @param lpClientList
     * @param ms
     * @return
     * @throws ConnectionException
     */
    public List<LoyaltyPointClient> redeemLoyaltyPointNonce(Connection conn, Credential cred,
                                      ProofSpec spec,
                                      List<LoyaltyPointClient> lpClientList,
                                      int amountToRedeem, MasterSecret ms, boolean sendFirtsMessage)
            throws ConnectionException{

        if(sendFirtsMessage){
            // Get nonce list from the server
            UCSystemMessage ucsMsg0 = new UCSystemMessage(UCSCmdVals.REDEEM_POINTS_NONCE,
                    spec, lpClientList.size());
            conn.send(ucsMsg0);
        }

        //List<BigInteger> nonceList = (List<BigInteger>) conn.receive();
        BigInteger nonce = (BigInteger) conn.receive();
        UCSystemNonce nonceObj = new UCSystemNonce(nonce);

        return doRedeem(conn, cred, spec, lpClientList, amountToRedeem, ms, nonceObj);
    }


    /**
     *
     * Perform the operations to redeem a list of points once the nonce
     * has been generated. It generates a proof per loyalty point
     *
     * @param conn
     * @param cred
     * @param spec
     * @param spec
     * @param lpClientList
     * @param ms
     * @param nonceObj
     * @return
     * @throws ConnectionException
     */
    private List<LoyaltyPointClient> doRedeem(Connection conn, Credential cred,
                             ProofSpec spec,
                             List<LoyaltyPointClient> lpClientList,
                             int amountToRedeem,
                             MasterSecret ms, UCSystemNonce nonceObj)
            throws ConnectionException {

        Stopwatch stopwatch = Stopwatch.createStarted();

        List<LoyaltyPointRedeem> lpRedeemList = new ArrayList<LoyaltyPointRedeem>();

        String credTempName = UCSystemUtil.getCredTempNameFromProofSpec(spec);
        String commName =  UCSystemUtil.getCommNameFromProofSpec(spec);

        //Note: we could pre-compute the proofs for better performance
        for(int i = 0; i< lpClientList.size(); ++i) {

            Proof proof = genProof(cred, spec, ms, nonceObj.getNonce(i),
                    lpClientList.get(i), credTempName, commName);

            lpRedeemList.add( new LoyaltyPointRedeem(
                    lpClientList.get(i).getLoyaltyPointObject(),
                    proof, nonceObj.getNonce(i) ) );
        }

        UCSCmdVals cmd = null;

        if( nonceObj.isList())
            cmd = UCSCmdVals.REDEEM_POINTS_NONCE;
        else
            cmd = UCSCmdVals.REDEEM_POINTS;

        UCSystemMessage ucsMsg1 = new UCSystemMessage(cmd, spec, null, nonceObj.getNonce(0),
                lpRedeemList, amountToRedeem, commName);
        //UCSystemMessage ucsMsg1 = new UCSystemMessage(cmd, lpRedeemList, amountToRedeem, spec);

        stopwatch.stop();

        Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).info("Redeeming (multi): " + counterRedeeming + ", "+ stopwatch);
        counterRedeeming++;

        conn.send(ucsMsg1);

        UCSystemRespMsg redeemResp = (UCSystemRespMsg) conn.receive();

        if(redeemResp.areRedeemErrors()){

            HashMap <String, UCSystemUtil.redeemRespVals> lpNoRedeemed =
                    redeemResp.getLpNoRedeemed();

            Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).
                    log(Level.SEVERE, "Errors while redeeming LP");
            return null;
        }
        Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).info("All LP successfully redeemed");

        //If there are not errors during redeeming, then check if change is needed

        int amountInRedeemList = UCSystemUtil.getLpListTotalAmount(lpRedeemList);

        if(  amountInRedeemList > amountToRedeem){
            //Request change back. The server should know already that change is due to the user
            Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).info("Change back required:" +
                    (amountInRedeemList-amountToRedeem)+ " points" );
            List<Integer> amountChangeList = UCSystemUtil.getLpAmountList(
                    amountInRedeemList-amountToRedeem);
            return getLoyaltyPoint(conn, cred, amountChangeList, true);
        }

        return null;
    }

    //Note: proofs could be pre-computed to improve performance if nonces are disabled

    private Proof genProof(Credential cred, ProofSpec spec, MasterSecret ms,
                           BigInteger nonce, LoyaltyPointClient lp,
                           String credTempName, String commName){

        HashMap<String, Credential> creds;
        creds = new HashMap<String, Credential>();
        creds.put(credTempName, cred);

        HashMap<String, CommitmentOpening> pCommitments;
        pCommitments = new HashMap<String, CommitmentOpening>();
        pCommitments.put(commName, lp.getCommOpening());

        Prover prover = new Prover(ms, creds, null, pCommitments, null, null);

        return prover.buildProof(nonce, spec);
    }


    /**
	 * Generates commitment based on the LS attribute of a loyalty
	 * credential
	 * 
	 * @param cred:  Idemix loyalty credential
	 * @return CommitmentOpening: commitment opening (commitment value and opening values)
	 */
	public CommitmentOpening genCommitment(Credential cred){
		
		IssuerPublicKey publicKey = cred.getPublicKey();
		
        BigInteger n = publicKey.getN();
        BigInteger capS = publicKey.getCapS();
        BigInteger capZ = publicKey.getCapZ();
        BigInteger LS = cred.getAttribute("LS").getValue();
        SystemParameters sp = publicKey.getGroupParams().getSystemParams();                                
        BigInteger r = CommitmentOpening.genRandom(n, sp.getL_n());
 
        CommitmentOpening commOpening = new CommitmentOpening(capZ, LS,
                capS, r, n, sp.getL_n());
        
        if (!commOpening.verifyCommitment()) {
        	Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.SEVERE, "Commitment verification failed");
        	return null;
        }	
        
        return commOpening;         
	}


    /**
     * Returns the epoch string from a PBSMessage object. It basically parses
     * the info value:  <tt>info = amount-epoch</tt>
     *
     * @param pbsMsg
     * @return  epoch string
     */
	 public String getEpoch(PBSMessage pbsMsg){

         return pbsMsg.getInfo().split("-")[1];
     }

    /**
     *  Return a list of loyalty point client objects to be redeemed
     *
     * @param loyaltyPointClientList The total list of points available
     * @param redeemAmount  The amount of points to be redeemed
     * @return  The list of points to be redeemed
     */
	 public List<LoyaltyPointClient> getLoyaltyPointClientRedeemList(
             List<LoyaltyPointClient> loyaltyPointClientList, int redeemAmount){


         return loyaltyPointClientList;

     }


    /**
     * Adds commitments predicates to the proof. Initially, the proof specification
     * only defines one commitment predicated. This method allows to add more
     * commitment predicates dynamically.
     *
     * @param spec The proof spec
     * @param n  The number of commitment predicates to add to the proof spec
     * @return   True on success, False otherwise
     *
     */
    private  boolean addCommitmentPredicates(ProofSpec spec, String commName,
                                             String predIdentifier, int n){

        Collection<Identifier> identifiers = spec.getIdentifiers();

        for(Identifier identifier : identifiers){

            if( identifier.getName().matches(predIdentifier) ){

                Vector<Identifier> commIdentifier = new Vector<Identifier>();
                commIdentifier.add(identifier);
                Vector<Predicate> predicates = spec.getPredicates();

                for(int i=0; i<n; ++i){
                    predicates.add( new CommitmentPredicate(commName+(i+1),
                            commIdentifier) );
                }

                return true;
            }
        }
        return false;
    }

    public int getCounterEarning() {
        return counterEarning;
    }

    public void setCounterEarning(int counterEarning) {
        this.counterEarning = counterEarning;
    }

    public int getCounterRedeeming() {
        return counterRedeeming;
    }

    public void setCounterRedeeming(int counterRedeeming) {
        this.counterRedeeming = counterRedeeming;
    }
}

