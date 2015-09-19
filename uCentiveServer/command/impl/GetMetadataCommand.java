package org.mobcom.inshopnito.server.command.impl;

import org.mobcom.inshopnito.server.command.AbstractCommand;
import org.mobcom.inshopnito.server.database.Database;
import org.mobcom.ucentiveshoppingassistant.model.shop.Product;

public class GetMetadataCommand extends AbstractCommand{
	public static final String COMMANDWORD = "getMetaData";
	private String id;
	
	public GetMetadataCommand(String id){
		this.id = id;
	}
	
	@Override
	public void execute() {
		try {
			Product product = Database.getInstance().getProduct(id);
			getConnection().send(product);
            getConnection().close();            
		} catch (Exception e) {			
			e.printStackTrace();
		}
		
	}

	@Override
	public String getCommandWord() {
		return COMMANDWORD;
	}

}
