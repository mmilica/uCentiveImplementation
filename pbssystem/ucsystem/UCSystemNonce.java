package be.kuleuven.cs.ucsystem;

import java.math.BigInteger;
import java.util.List;

/**
 * This class is used to hide the fact that the nonce could be
 * a single value for all the points or a value per point (list).
 */

public class UCSystemNonce {

    private BigInteger nonce;
    private List<BigInteger> nonceList;

    public UCSystemNonce(BigInteger nonce) {
        this.nonce = nonce;
        this.nonceList = null;
    }

    public UCSystemNonce(List<BigInteger> nonceList) {
        this.nonceList = nonceList;
        this.nonce = null;
    }

    public int getSize(){
        if (nonceList == null)
            return 1;
        else
            return nonceList.size();
    }

    public BigInteger getNonce(int i){
        if(nonceList == null)
            return nonce;
        else
            return nonceList.get(i);
    }

    /**
     * Returns True if the object is a list of nonces,
     * False if it is a single BigInteger nonce value
     * @return
     */
    public boolean isList(){
        if(nonceList == null)
            return false;
        else
            return true;
    }
}
