package be.kuleuven.cs.pbs;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import be.kuleuven.cs.pbs.PBSMessage.PBSProtocolValues;
import be.kuleuven.cs.ucsystem.UCSystemException;

import com.ibm.zurich.idmx.dm.CommitmentOpening;


public class PBSUser extends PBS{


    private List<BigInteger> t1List;
    private List<BigInteger> t2List;
    private List<BigInteger> t3List;
    private List<BigInteger> t4List;
    private List<BigInteger> zList;
    private List<String> msgList;
	
	
	public PBSUser(BigInteger p, BigInteger q, BigInteger g, BigInteger y) {

        super(p, q, g, y);

        this.t1List = new ArrayList<BigInteger>();
        this.t2List = new ArrayList<BigInteger>();
        this.t3List = new ArrayList<BigInteger>();
        this.t4List = new ArrayList<BigInteger>();
        this.zList = new ArrayList<BigInteger>();
        this.msgList = new ArrayList<String>();
	}


    public List<PBSMessage> step1(List<PBSMessage> pbsMsg1List,
                                  List<String> msgList){

        this.msgList = msgList;

        List<PBSMessage> pbsMsg2List = new ArrayList<PBSMessage>();

        for(int i=0; i<pbsMsg1List.size(); ++i){

            BigInteger a = pbsMsg1List.get(i).
                    getProtocolElement(PBSProtocolValues.a);
            BigInteger b = pbsMsg1List.get(i).
                    getProtocolElement(PBSProtocolValues.b);

            BigInteger t1 = PBSUtil.randomBigInteger( getQ() );
            BigInteger t2 = PBSUtil.randomBigInteger( getQ() );
            BigInteger t3 = PBSUtil.randomBigInteger( getQ() );
            BigInteger t4 = PBSUtil.randomBigInteger( getQ() );

            String info = pbsMsg1List.get(i).getInfo();

            BigInteger z = PBSUtil.hashF( getP(), getQ(), info);

            BigInteger alpha = ( a.multiply( getG().modPow(t1, getP()) ).
                    multiply(getY().modPow(t2, getP())) ).mod(getP());

            BigInteger beta = ( b.multiply( getG().modPow(t3, getP()) ).
                    multiply(z.modPow(t4, getP())) ).mod(getP());

            BigInteger epsilon = PBSUtil.hashH( alpha, beta, z,
                    msgList.get(i), getQ() );

            BigInteger e = ( epsilon.subtract( t2 ).subtract( t4 ) ).mod( getQ() );

            t1List.add(t1);
            t2List.add(t2);
            t3List.add(t3);
            t4List.add(t4);
            zList.add(z);

            HashMap<PBSProtocolValues, BigInteger> pbsProtocolValues;
            pbsProtocolValues = new HashMap<PBSProtocolValues, BigInteger>();
            pbsProtocolValues.put(PBSProtocolValues.e, e);

            pbsMsg2List.add(new PBSMessage(pbsProtocolValues, info));
        }

        return pbsMsg2List;

    }

	public List<PBSSignature> step2(List<PBSMessage> pbsMsg3List){

        List<PBSSignature> signatureList = new ArrayList<PBSSignature>();

        for(int i=0; i < pbsMsg3List.size(); ++i){

            BigInteger r = pbsMsg3List.get(i).getProtocolElement(PBSProtocolValues.r);
            BigInteger c = pbsMsg3List.get(i).getProtocolElement(PBSProtocolValues.c);
            BigInteger s = pbsMsg3List.get(i).getProtocolElement(PBSProtocolValues.s);
            BigInteger d = pbsMsg3List.get(i).getProtocolElement(PBSProtocolValues.d);

            BigInteger t1 = t1List.get(i);
            BigInteger t2 = t2List.get(i);
            BigInteger t3 = t3List.get(i);
            BigInteger t4 = t4List.get(i);

            BigInteger rho = ( r.add( t1 ) ).mod( getQ() );
            BigInteger omega = ( c.add( t2 ) ).mod( getQ() );
            BigInteger sigma = ( s.add( t3 ) ).mod( getQ() );
            BigInteger delta = ( d.add( t4 ) ).mod( getQ() );


            PBSSignature test =  new PBSSignature(pbsParams, rho, omega, sigma, delta,
                    pbsMsg3List.get(i).getInfo(),
                    msgList.get(i) );
            test.verifySignature();
            signatureList.add(test);

        }
        resetLists();
        return signatureList;
	}
    /**
     * Deletes state data in the client lists once
     * the loyalty points have been issued
     */
    private void resetLists() {

        this.t1List.clear();
        this.t2List.clear();
        this.t3List.clear();
        this.t4List.clear();
        this.zList.clear();
        this.msgList.clear();

    }
	
}
