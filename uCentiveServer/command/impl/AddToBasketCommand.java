package org.mobcom.inshopnito.server.command.impl;



import be.kuleuven.cs.priman.exception.ConnectionException;
import org.mobcom.inshopnito.server.Session;
import org.mobcom.inshopnito.server.SessionManager;
import org.mobcom.inshopnito.server.command.AbstractCommand;



public class AddToBasketCommand extends AbstractCommand{
	
	private String id;
	private String name;
	private Double price;
	private Integer quantity;
	private String category;
	private String sessionID;
	
	public AddToBasketCommand(String id, String name, String price, String quantity,
			String category, String sessionID) {
		super();
		this.id = id;
		this.name = name;
		this.price = Double.parseDouble(price);
		this.category = category;
		this.sessionID = sessionID;
		this.quantity = Integer.parseInt(quantity);
	}
	public AddToBasketCommand(String sessionID, Object[] arguments) {
		this(arguments[0].toString(),arguments[1].toString(),arguments[2].toString(),arguments[3].toString(),arguments[4].toString(),sessionID);
	}
	public static final String COMMANDWORD = "addToBasket";
	@Override
	public void execute() {
		Session session = SessionManager.getInstance().getSession(sessionID);
		if(session == null) return;
		session.addToBasket(id, quantity);

        try {
            getConnection().close();
        } catch (ConnectionException e) {
            e.printStackTrace();
        }
    }
	@Override
	public String getCommandWord() {
		return AddToBasketCommand.COMMANDWORD;
	}

}
