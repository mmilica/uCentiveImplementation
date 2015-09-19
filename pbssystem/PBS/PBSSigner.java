package be.kuleuven.cs.pbs;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import be.kuleuven.cs.pbs.PBSMessage.PBSProtocolValues;
import be.kuleuven.cs.ucsystem.UCSystemException;
import be.kuleuven.cs.ucsystem.UCSystemUtil;

public class PBSSigner extends PBS {

    private BigInteger x;

    private List<BigInteger> uList;
    private List<BigInteger> sList;
    private List<BigInteger> dList;

    public PBSSigner(BigInteger p, BigInteger q, BigInteger g,
                      BigInteger x) {

        super( p, q, g, g.modPow(x, p) );
        this.x = x;

        this.uList = new ArrayList<BigInteger>();
        this.sList = new ArrayList<BigInteger>();
        this.dList = new ArrayList<BigInteger>();
    }



    public List<PBSMessage> step1( List<Integer> amountList, String epoch ) {

        List<PBSMessage> PBSMessageList = new ArrayList<PBSMessage>();

        for(int amount : amountList){

            String info = amount+"-"+epoch;

            BigInteger u = PBSUtil.randomBigInteger( getQ() );
            BigInteger s = PBSUtil.randomBigInteger( getQ() );
            BigInteger d = PBSUtil.randomBigInteger( getQ() );
            BigInteger z = PBSUtil.hashF( getP(), getQ(), info );
            BigInteger a = getG().modPow(u, getP());
            BigInteger b = getG().modPow(s, getP()).multiply( z.modPow( d, getP() ) ).mod( getP() );

            HashMap<PBSProtocolValues, BigInteger> pbsProtocolValues;
            pbsProtocolValues = new HashMap<PBSProtocolValues, BigInteger>();
            pbsProtocolValues.put(PBSProtocolValues.a, a);
            pbsProtocolValues.put(PBSProtocolValues.b, b);

            uList.add(u);
            sList.add(s);
            dList.add(d);
            PBSMessageList.add(new PBSMessage(pbsProtocolValues, info));
        }

        return PBSMessageList;
    }



    public List<PBSMessage> step2(List<PBSMessage> pbsMsg2List){

        List<PBSMessage> pbsMsg3List = new ArrayList<PBSMessage>();

        for(int i=0; i < pbsMsg2List.size(); ++i){

            BigInteger e = pbsMsg2List.get(i).getProtocolElement(PBSProtocolValues.e);
            BigInteger u = uList.get(i);
            BigInteger s = sList.get(i);
            BigInteger d = dList.get(i);

            BigInteger c = e.subtract( d ).mod( getQ() );
            BigInteger r = (u.subtract( c.multiply( x ) )).mod( getQ() );

            HashMap<PBSProtocolValues, BigInteger> pbsProtocolValues;
            pbsProtocolValues = new HashMap<PBSProtocolValues, BigInteger>();
            pbsProtocolValues.put(PBSProtocolValues.r, r);
            pbsProtocolValues.put(PBSProtocolValues.c, c);
            pbsProtocolValues.put(PBSProtocolValues.s, s);
            pbsProtocolValues.put(PBSProtocolValues.d, d);

            pbsMsg3List.add(new PBSMessage(pbsProtocolValues, pbsMsg2List.get(i).getInfo()));
        }

        resetLists();
        return pbsMsg3List;
    }

    /**
     * Deletes state data in the server lists once
     * the loyalty points have been issued
     */
    private void resetLists() {

        this.dList.clear();
        this.sList.clear();
        this.uList.clear();

    }
}
