package org.mobcom.inshopnito.server.command;

import org.mobcom.inshopnito.server.command.impl.*;

public class CommandFactory {

	/**
	 * This method will create a command object from a given command method.
	 * Which type of command is instantiated will depend on the command word from the given command message
	 *
	 * @param msg
	 * @return
	 */
	public AbstractCommand createCommand(CommandMessage msg){
		String cmdWord = msg.getCommandWord();

		if(cmdWord.equals(AuthenticationCommand.COMMANDWORD)){
			return new AuthenticationCommand();
		}else if(cmdWord.equals(AddToBasketCommand.COMMANDWORD)){
			Object[] arguments = msg.getArguments();
			return new AddToBasketCommand(msg.getSessionID(),msg.getArguments());
		}else if(cmdWord.equals(GetRecommendationCommand.COMMANDWORD)){
			return new GetRecommendationCommand(msg.getSessionID());
		} else if (cmdWord.equals(GetLoyaltyPointsCommand.COMMANDWORD))
            return new GetLoyaltyPointsCommand(msg.getSessionID(), msg.getArguments());
        else if (cmdWord.equals(RedeemLoyaltyPointsCommand.COMMANDWORD))
            return new RedeemLoyaltyPointsCommand(msg.getSessionID(), msg.getArguments());
        else if (cmdWord.equals(DoPaymentCommand.COMMANDWORD))
            return new DoPaymentCommand(msg.getSessionID(), msg.getArguments());
        else if (cmdWord.equals(GetMetadataCommand.COMMANDWORD))
            return new GetMetadataCommand( msg.getArguments()[0].toString());
		else if (cmdWord.equals(GetVoucherCommand.COMMANDWORD))
            return new GetVoucherCommand(msg.getSessionID(), msg.getArguments());
        else if (cmdWord.equals(RedeemVoucherCommand.COMMANDWORD))
            return new RedeemVoucherCommand(msg.getSessionID(), msg.getArguments());
		return null;
	}
}
