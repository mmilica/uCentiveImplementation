package be.kuleuven.cs.ucsystem;

import be.kuleuven.cs.pbs.PBSSignature;
import be.kuleuven.cs.primanprovider.credential.idemix.utils.PrimanParser;
import be.kuleuven.cs.primanprovider.credential.idemix.utils.PrimanXMLSerializer;
import com.ibm.zurich.idmx.dm.Commitment;
import com.ibm.zurich.idmx.showproof.Proof;

import java.math.BigInteger;

/**
 * Contains a loyalty point and the data required to
 * redeem it (proof, nonce), except the proof spec.
 * The proof spec is the same for all the points to
 * be redeemed and, therefore, sent it separately as
 * a single object
 */
public class LoyaltyPointRedeem extends LoyaltyPoint {

    /* proof can't be serialized, so we store it as string */
    private String proof;
    private BigInteger nonce;

    public LoyaltyPointRedeem(PBSSignature signature, Commitment comm,
                              int amount, String epoch, Proof proof,
                              BigInteger nonce) {
        super(signature, comm, amount, epoch);
        this.proof = toXML(proof);
        this.nonce = nonce;
    }

    public LoyaltyPointRedeem(LoyaltyPoint lp, Proof proof,
                              BigInteger nonce){
        this(lp.getSignature(), lp.getComm(), lp.getAmount(),
                lp.getEpoch(), proof, nonce);
    }

    public LoyaltyPoint getLoyaltyPointObject(){
        return new LoyaltyPoint(signature, comm, amount, epoch);
    }

    private String toXML(Object o){
        return PrimanXMLSerializer.getInstance().serialize(o);
    }

    private Object fromXML (String s){
        return  PrimanParser.getInstance().parse(s);
    }

    public Proof getProof() {
        return (Proof) fromXML(proof);
    }

    public BigInteger getNonce() {
        return nonce;
    }
}
