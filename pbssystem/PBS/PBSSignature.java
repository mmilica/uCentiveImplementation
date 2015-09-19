package be.kuleuven.cs.pbs;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PBSSignature implements Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	//local or remote config file
	

    private PBSParams pbsParams;
	private BigInteger rho;	
	private BigInteger sigma;	
	private BigInteger omega;	
	private BigInteger delta;	
	private String info;
	private String msg;

	
	public PBSSignature(PBSParams pbsParams, BigInteger rho, BigInteger omega,
			BigInteger sigma, BigInteger delta, String info, String msg){
	    this.rho = rho;
	    this.omega = omega;
	    this.sigma = sigma;
	    this.delta = delta;
	    this.info = info;
	    this.msg = msg;
        this.pbsParams = pbsParams;
	}

	
	public boolean verifySignature(){
		
		// Compute z
		BigInteger z = PBSUtil.hashF( getP(), getQ(), info );
		
		// Compute alpha and beta 
		BigInteger alpha = (getG().modPow(rho, getP())
				.multiply(getY().modPow(omega, getP()))).mod(getP());
		
		BigInteger beta = (getG().modPow(sigma, getP())
				.multiply(z.modPow(delta, getP()))).mod(getP());
		
		BigInteger rhs = PBSUtil.hashH(alpha, beta, z, msg, getQ());
		BigInteger lhs = (omega.add(delta)).mod(getQ());

        return  lhs.equals(rhs);
	}


	public BigInteger getRho() {
		return rho;
	}

	public BigInteger getSigma() {
		return sigma;
	}

	public BigInteger getOmega() {
		return omega;
	}

	public BigInteger getDelta() {
		return delta;
	}

	public String getInfo() {
		return info;
	}

	public String getMsg() {
		return msg;
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

	/**
	 * This method can be used by the verifier to validate that
	 * the public parameters (p,q,g) used to generated this signature
	 * are the appropriate ones. 
	 * @param p
	 * @param q
	 * @param g
	 * @return True if the values are correct, False otherwise
	 */
	public boolean verifyParams(BigInteger p, BigInteger q, 
			BigInteger g, BigInteger y){
		
		if( p.equals(getP()) && q.equals(getQ())
				&& g.equals(getG()) && y.equals(getY()))
			return true;
		else		
			return false;
	}
	
}
