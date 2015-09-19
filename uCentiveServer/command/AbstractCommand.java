package org.mobcom.inshopnito.server.command;

import be.kuleuven.cs.priman.connection.Connection;


/**
 * @author andreasp
 *
 */
public abstract class AbstractCommand implements Runnable{
	
	private Connection conn;
	
	
	/**
	 * execute a command without a reply
	 */
	public abstract void execute();
	/**
	 * Returns a unique string which identifies this command type
	 * @return
	 */
	public abstract String getCommandWord();
	
	public void run(){
		execute();
	}
	public Connection getConnection() {
		return conn;
	}
	public void setConnection(Connection connection) {
		this.conn = connection;
	}
	
	
}
