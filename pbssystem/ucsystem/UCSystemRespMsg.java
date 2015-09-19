package be.kuleuven.cs.ucsystem;

import java.io.Serializable;
import java.util.HashMap;

/**
 * Contains a list of loyalty points that were
 * not redeemed and the reason
 */
public class UCSystemRespMsg implements Serializable {

    private HashMap <String, UCSystemUtil.redeemRespVals> lpNoRedeemed;
    private boolean redeemErrors;

    public UCSystemRespMsg(HashMap<String, UCSystemUtil.redeemRespVals> lpNoRedeemed, Boolean redeemErrors) {
        this.redeemErrors = redeemErrors;
        this.lpNoRedeemed = lpNoRedeemed;
    }

    public HashMap<String, UCSystemUtil.redeemRespVals> getLpNoRedeemed() {
        return lpNoRedeemed;
    }

    public boolean areRedeemErrors() {
        return redeemErrors;
    }
}
