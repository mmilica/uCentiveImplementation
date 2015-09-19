package be.kuleuven.cs.ucsystem;

import be.kuleuven.cs.pbs.PBSSignature;

import com.ibm.zurich.idmx.dm.Commitment;
import com.ibm.zurich.idmx.dm.CommitmentOpening;

public class LoyaltyPointClient extends LoyaltyPoint {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private CommitmentOpening commOpening;
    // loyalty point ID in the database
    private String lpID;
	
	public LoyaltyPointClient(PBSSignature signature,
                              CommitmentOpening commOpening,
                              int amount, String epoch) {
		
		super(signature, commOpening.getCommitmentObject(), amount, epoch);
		this.commOpening = commOpening;
        this.lpID = null;
	}

    public LoyaltyPointClient(PBSSignature signature,
                              CommitmentOpening commOpening,
                              int amount, String epoch,  String lpID) {
        this(signature, commOpening, amount, epoch);
        this.commOpening = commOpening;
        this.lpID = lpID;
    }

    public CommitmentOpening getCommOpening() {
		return commOpening;
	}
	
	/**
	 * Returns a loyalty point object that can be send to the 
	 * server (it only includes a commitment object without the
	 * opening values)
	 * @return
	 */
	public LoyaltyPoint getLoyaltyPointObject(){
		return new LoyaltyPoint(signature, comm, amount, epoch);
	}

    public String getLpID() {
        return lpID;
    }
}
