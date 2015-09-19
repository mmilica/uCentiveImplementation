package be.kuleuven.cs.pbs;

import java.math.BigInteger;

public class PBS {
	

    protected PBSParams pbsParams;

	public PBS(BigInteger p, BigInteger q, BigInteger g, 
			BigInteger y) {

        this.pbsParams = new PBSParams(p, q, g, y);
	}


	public BigInteger getP() {
		return pbsParams.getP();
	}
	public BigInteger getQ() {
		return pbsParams.getQ();
	}
	public BigInteger getG() {
		return pbsParams.getG();
	}
	public BigInteger getY() {
		return pbsParams.getY();
	}	
	
}
