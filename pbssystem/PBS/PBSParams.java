package be.kuleuven.cs.pbs;

import java.io.Serializable;
import java.math.BigInteger;

public class PBSParams implements Serializable {

    private BigInteger p;
    private BigInteger q;
    private BigInteger g;
    private BigInteger y;

    public PBSParams(BigInteger p, BigInteger q, BigInteger g, BigInteger y) {
        this.p = p;
        this.q = q;
        this.g = g;
        this.y = y;
    }

    public BigInteger getP() {
        return p;
    }

    public BigInteger getQ() {
        return q;
    }

    public BigInteger getG() {
        return g;
    }

    public BigInteger getY() {
        return y;
    }

}
