package org.mobcom.inshopnito.server.command.impl;

import be.cosic.anonvoucherclient.Voucher;
import be.cosic.pbs.PublicParams;
import be.kuleuven.cs.priman.exception.ConnectionException;

import org.mobcom.inshopnito.server.command.AbstractCommand;


public class RedeemVoucherCommand extends AbstractCommand {


	public final static String COMMANDWORD = "redeemVoucher";

	private Voucher voucher;
	private String sessionID;

	public RedeemVoucherCommand(String sessionID, Voucher voucher) {
		this.sessionID = sessionID;
		this.voucher = voucher;
	}

    public RedeemVoucherCommand(String sessionID, Object[] arguments){
        this(sessionID, (Voucher) arguments[0]);
     }


	@Override
	public void execute() {

		
				
		try {
			if (voucher.isValid(PublicParams.getDefaults())) {
				System.out.println("Voucher " + voucher + " is valid");			
				getConnection().send(true);
			}
		    else {
		    	System.out.println("Voucher " + voucher + " is not valid!");		    
		    	getConnection().send(false);
		    }
			// Gracefully terminate the connection
			getConnection().close();
		} catch (ConnectionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


	}

	@Override
	public String getCommandWord() {
		return RedeemVoucherCommand.COMMANDWORD;
	}


}
