package be.kuleuven.cs.ucsystem;

import java.io.Serializable;

import com.ibm.zurich.idmx.dm.Commitment;
import be.kuleuven.cs.pbs.PBSSignature;

public class LoyaltyPoint implements Serializable, Comparable<LoyaltyPoint>{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	protected PBSSignature signature;
	protected Commitment comm;
	protected int amount = 0;
	protected String epoch = null;
	
	
	/**
	 * Constructor
	 * @param signature : partially blind signature
	 * @param comm : commitment
	 * @param amount:  number of points
	 * @param epoch:  expiration date
	 * 
	 */
	public LoyaltyPoint(PBSSignature signature, Commitment comm,
			int amount, String epoch) {
		this.signature = signature;
		this.comm = comm;
		this.amount = amount;
		this.epoch = epoch;		
	}


	public PBSSignature getSignature() {
		
		return signature;
	}


	public Commitment getComm() {
		return comm;
	}


	public int getAmount() {
		return amount;
	}


	public String getEpoch() {
		return epoch;
	}

    @Override
    public int compareTo(LoyaltyPoint other) {
        return this.getAmount() - other.getAmount();
    }
}
