package org.mobcom.inshopnito.server.command.impl;

import be.cosic.anonvoucherclient.Voucher;
import be.cosic.pbs.PBSIssuer;
import be.cosic.pbs.PrivateKey;
import be.cosic.pbs.PublicParams;
import be.kuleuven.cs.priman.exception.ConnectionException;

import org.mobcom.inshopnito.server.command.AbstractCommand;

import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.Calendar;


public class GetVoucherCommand extends AbstractCommand {


	private static final String epoch = "2014";

	public final static String COMMANDWORD = "getVoucher";

	private int value;
	private String sessionID;

	public GetVoucherCommand(String sessionID, int value) {
		this.sessionID = sessionID;
		this.value = value;
	}

    public GetVoucherCommand(String sessionID, Object[] arguments){
        this(sessionID, (Integer) arguments[0]);
     }


	@Override
	public void execute() {

		
		PBSIssuer myIssuer;
		Voucher myVoucher;

		try {
			// Create a Voucher object (to store value and exp date)
			myVoucher = createVoucher(value);
			// Initiate a PBSIssuer object
			myIssuer = new PBSIssuer(myVoucher.getInfo(), PublicParams.getDefaults(), PrivateKey.x());

			// Calculate a and b and send to the client
			String[] bundle = stepOne(myIssuer, myVoucher);
			getConnection().send(bundle);			
			
			// Wait for response from client containing 'e'
			BigInteger e = (BigInteger)getConnection().receive();
			
			// Compute the response r, c, s, d
			BigInteger[] rcsd = myIssuer.computeRCSD(e);
			// Send the rcsd values to the client
			getConnection().send(rcsd);			

			// Gracefully terminate the connection
			getConnection().close();
		} catch (ConnectionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


	}

	@Override
	public String getCommandWord() {
		return GetVoucherCommand.COMMANDWORD;
	}

	
	private Voucher createVoucher(int value) {
		// Set the default expiration date to today + 1 year				
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.YEAR, cal.get(Calendar.YEAR) + 1);				
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");				
		String exp = sdf.format(cal.getTime());				

		// We use the Voucher class to calculate the "info" string
		return new Voucher(new byte[4], value, exp, Voucher.VALID);		
	}
	
	private String[] stepOne(PBSIssuer myIssuer, Voucher myVoucher) {

		// Compute AB
		BigInteger[] ab = myIssuer.computeAB();

		// Bundle the response and send it to the client
		String[] bundle = new String[5];
		bundle[0] = Integer.toString(myVoucher.getValue());		// value
		bundle[1] = myVoucher.getExpDate();						// exp date
		bundle[2] = myIssuer.getZ().toString(16);				// z value (not used)
		bundle[3] = ab[0].toString(16);							// a
		bundle[4] = ab[1].toString(16);							// b

		return bundle;
	}
}
