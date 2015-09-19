package be.kuleuven.cs.ucsystem;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.List;

import com.ibm.zurich.idmx.showproof.Proof;
import com.ibm.zurich.idmx.showproof.ProofSpec;

import be.kuleuven.cs.primanprovider.credential.idemix.utils.PrimanParser;
import be.kuleuven.cs.primanprovider.credential.idemix.utils.PrimanXMLSerializer;

public class UCSystemMessage implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	// Do we need also response values?
	public enum UCSCmdVals {
		GET_POINTS,
		REDEEM_POINTS,
        REDEEM_POINTS_NONCE,
        REDEEM_POINTS_SINGLE_PROOF,
        REDEEM_POINTS_SINGLE_PROOF_NONCE
	}
	
	private UCSCmdVals cmd;
	// Idemix proof and proof spec objects can't be serialized,
	// thus we store them as XML strings
    private String proofStr;
	private String specStr;
    // amount of points to be issued divided according to the defined denominations
    // This can be used for issuing new points or getting points back after redeeming
    // a non-exact amount of points
    private List<Integer> amountList;
    // list of points to be redeemed
	private List<LoyaltyPointRedeem> lpRedeemList;
    // the number of loyalty points objects to be redeemed
    private int redeemListSize;
    // the nonce for single proof redeeming protocol
    private BigInteger nonce;
    // the name of the commitment to be used in the proof spec
    private String commName;

    // the amount that the user intends to redeem. It should be equal or smaller
    // than the amount in lpredeemList
    private int amountToRedeem;

    /**
     *  Construct message to request loyalty points
     * @param cmd
     * @param amountList
     */
    public UCSystemMessage(UCSCmdVals cmd, List<Integer> amountList){
        this.cmd = cmd;
        this.amountList = amountList;
    }

    /** Construct message to request nonces for redeeming points
     *
     * @param cmd
     * @param spec
     * @param redeemListSize
     */
    public UCSystemMessage(UCSCmdVals cmd, ProofSpec spec, int redeemListSize){
        this(cmd, null);
        this.specStr = toXML(spec);
        this.redeemListSize = redeemListSize;
    }



    /**
     *  Construct a message to redeem points. Requires a proof spec object
     * @param cmd
     * @param lpRedeemList
     * @param spec
     */
    public UCSystemMessage(UCSCmdVals cmd, List<LoyaltyPointRedeem> lpRedeemList,
                       int amountToRedeem, ProofSpec spec){
        this(cmd, null);
        this.lpRedeemList = lpRedeemList;
        this.specStr = toXML(spec);
        this.amountToRedeem = amountToRedeem;
    }


    /**
     *  Constructor for a message to redeem points with a single proof with or
     *  without server nonces
     *
     * @param cmd
     * @param spec
     * @param proof
     * @param nonce
     * @param lpRedeemList
     */
    public UCSystemMessage(UCSCmdVals cmd, ProofSpec spec, Proof proof,
                       BigInteger nonce, List<LoyaltyPointRedeem> lpRedeemList,
                       int amountToRedeem, String commName){
        this(cmd, null);
        this.specStr = toXML(spec);
        this.proofStr = toXML(proof);
        this.nonce = nonce;
        this.lpRedeemList = lpRedeemList;
        this.amountToRedeem = amountToRedeem;
        this.commName = commName;
    }

//    public UCSystemMessage(UCSCmdVals cmd, String specStr, Proof proof,
//                       BigInteger nonce, List<LoyaltyPointRedeem> lpRedeemList){
//        this(cmd, null);
//        this.specStr = specStr;
//        this.proofStr = toXML(proof);
//        this.nonce = nonce;
//        this.lpRedeemList = lpRedeemList;
//    }


    public UCSCmdVals getCmd() {
        return cmd;
    }

    public ProofSpec getSpec() {
        return (ProofSpec)fromXML(specStr);
    }

    public List<Integer> getAmountList() {
        return amountList;
    }

    public List<LoyaltyPointRedeem> getLpRedeemList() {
        return lpRedeemList;
    }

    public Proof getProof() {
        return (Proof)fromXML(proofStr);
    }

    public BigInteger getNonce() {
        return nonce;
    }

    public int getRedeemListSize() {
        return redeemListSize;
    }

    public String getCommName() {
        return commName;
    }

    public int getAmountToRedeem() {
        return amountToRedeem;
    }

    private String toXML(Object o){
		return PrimanXMLSerializer.getInstance().serialize(o);
	}

	private Object fromXML (String s){
		return  PrimanParser.getInstance().parse(s);
	}
}
