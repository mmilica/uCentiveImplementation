package org.mobcom.inshopnito.server.command.impl;

import java.util.List;

import org.mobcom.inshopnito.server.Session;
import org.mobcom.inshopnito.server.SessionManager;
import org.mobcom.inshopnito.server.command.AbstractCommand;
import org.mobcom.ucentiveshoppingassistant.model.shop.Product;

import be.kuleuven.cs.priman.exception.ConnectionException;

public class GetRecommendationCommand extends AbstractCommand{
	public static final String COMMANDWORD = "getRecs";

	private String sessionID;

	public GetRecommendationCommand(String sessionID){
		this.sessionID = sessionID;
	}


	@Override
	public void execute() {
		Session session = SessionManager.getInstance().getSession(sessionID);
		if(session==null)return;

		List<Product> items = session.getRecommendations();
		try {
			getConnection().send("Start");
			for(Product rec:items){
				StringBuilder sb = new StringBuilder();
				sb.append(1).append("=").append(rec.getId()).append(";");
				sb.append(2).append("=").append(rec.getName()).append(";");
				sb.append(3).append("=").append(rec.getPrice()).append(";");
				sb.append(4).append("=").append(rec.getCategory()).append(";");		
				System.out.println(sb.toString());
				getConnection().send(sb.toString());
			}
			getConnection().send("Stop");
            getConnection().close();

		} catch (ConnectionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	@Override
	public String getCommandWord() {
		return GetRecommendationCommand.COMMANDWORD;
	}



}
