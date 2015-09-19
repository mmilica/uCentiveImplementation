package org.mobcom.inshopnito.server.command.impl;

import be.kuleuven.cs.ucsystem.UCSystemServer;
import be.kuleuven.cs.priman.exception.ConnectionException;
import org.mobcom.inshopnito.server.command.AbstractCommand;

import java.math.BigInteger;
import java.util.List;


public class GetLoyaltyPointsCommand extends AbstractCommand {

    private static final String constant_p = "98E462268DA976E92AFDF5987BC071CA6AD039663338F368673808EFBA7F1EEBE03870E773F028C17CD08BAF6F5F4875A3BCE63206A6C995149E8700F2731767DB04C0BEFFA0D929962298E959E13E5495699B1ADD7117CE859D108B7CC758264C3A47FAE858AC6341E98E3ECD109A525F6892B3A5592E868832D5E5621A2955";
    private static final String constant_q = "9B9BA5FF1975869AA2FC2724B20C657872EED7ED";
    private static final String constant_g = "068239A1D2C22C7D86D5CD0DAE791CB1FA0E022AF5F9DF5F72280C2BCD0E94D61E5ACD13ECB5E56D319D65537CAE4AD525EACB8128F4922301F9F927D4B3424F820ECE82CA0A813ED3E81352A00B3A9D390ACE90BCCB8FC979AB9AB95BF6E1541E28A2614F5F1DAF456D5AB1A11275616874BE3D0269EFBF714EABC5D6CDBBF2";
    private static final String constant_x = "7E7A7FB03E642BDA4EBE7E9EF08A0B29A4097357";

    private static BigInteger p = new BigInteger( constant_p, 16 );
    private static BigInteger q = new BigInteger( constant_q, 16 );
    private static BigInteger g = new BigInteger( constant_g, 16 );
    private static BigInteger x = new BigInteger( constant_x, 16 );
    private static BigInteger y = g.modPow( x, p );

    private static final String epoch = "2014";
    private List<Integer> amountList;

    public final static String COMMANDWORD = "getLoyaltyPoints";
    private String sessionID;

    public GetLoyaltyPointsCommand(String sessionID, List<Integer> amountList) {
        this.sessionID = sessionID;
        this.amountList = amountList;
    }

    public GetLoyaltyPointsCommand(String sessionID, Object[] arguments) {
        this(sessionID, (List<Integer>) arguments[0]);
    }

    @Override
    public void execute() {

        UCSystemServer ucsServer = new UCSystemServer(p, q, g, x, epoch);

        try {
            ucsServer.issueLoyaltyPoints(getConnection(), amountList, epoch);
            getConnection().close();

        } catch (ConnectionException e) {
            e.printStackTrace();
        }


    }

    @Override
    public String getCommandWord() {
        return GetLoyaltyPointsCommand.COMMANDWORD;
    }
}
