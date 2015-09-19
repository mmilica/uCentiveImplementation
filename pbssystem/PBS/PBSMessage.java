package be.kuleuven.cs.pbs;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Iterator;


/**
 * Convenience class to exchange the values of the PBS protocol 
 * between the user and the signer
 * 
 * 
 */

public class PBSMessage implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String info;

	public enum PBSProtocolValues{
		/** First message values */		
		a,
		b,
		/** Second message value */
		e,
		/** Third message values */
		r,
		c,
		s,
		d,
	}
	
	private final HashMap<PBSProtocolValues, BigInteger> pbsProtocolValues;

	
	/**
	 * @param pbsElements: values generated during a protocol step that need to be
	 * 			communicated to the communication partner
	 * @param info: public information include in the partially blinded signature
	 */
	public PBSMessage(HashMap<PBSProtocolValues, BigInteger> pbsElements,
                      String info){
        pbsProtocolValues = pbsElements;
		this.info = info;
	}
	/**
	 * @return The PBS element queried for (e.g., <tt>a<tt>, <tt>b<tt>,
	 * 			<tt>e<tt>, <tt>r<tt>, <tt>c<tt>, <tt>s<tt>, <tt>d<tt>)
	 */
	public final BigInteger getProtocolElement( PBSProtocolValues element){
		return pbsProtocolValues.get(element);
	}
	
    /**
     * Serialization method.
     */
    public final Iterator<PBSProtocolValues> iterator() {
        return pbsProtocolValues.keySet().iterator();
    }
	
	public void setInfo(String info) {
		this.info = info;
	}

	public String getInfo() {
		return info;
	}
}
