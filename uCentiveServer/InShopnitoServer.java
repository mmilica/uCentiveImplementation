package org.mobcom.inshopnito.server;

import java.io.File;
import java.net.URI;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.mobcom.inshopnito.server.command.AbstractCommand;
import org.mobcom.inshopnito.server.command.CommandFactory;
import org.mobcom.inshopnito.server.command.CommandMessage;

import be.kuleuven.cs.priman.Priman;
import be.kuleuven.cs.priman.connection.Connection;
import be.kuleuven.cs.priman.connection.ConnectionListener;
import be.kuleuven.cs.priman.connection.ConnectionParameters;
import be.kuleuven.cs.priman.exception.ConnectionException;
import be.kuleuven.cs.priman.exception.ProviderNotFoundException;
import be.kuleuven.cs.priman.manager.ConnectionManager;
import be.kuleuven.cs.primanprovider.connection.sockets.SocketListenerParameters;

public class InShopnitoServer {
	
	public static void main(String...args){
		startServer();
	}
	public static final URI HOME = new File("./files/").toURI();
	private static void startServer() {
		ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
		
		Priman priman = Priman.getInstance();
		priman.loadConfiguration(HOME.resolve("config/server-priman.conf"));
		ConnectionManager cm = priman.getConnectionManager();
		ConnectionParameters cp = priman.getPersistenceManager().load(HOME.resolve("config/server-ssl.param"));
		//ConnectionParameters cp = new SocketListenerParameters(18099);
		ConnectionListener cl = null;
		
		try {
			cl = cm.getListener(cp);
			System.out.println("listener created");
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		CommandFactory factory = new CommandFactory();
		
		while(true){
			try {
				Connection conn = cl.listen();
				System.out.println("connection received");
				//connection received
				CommandMessage message = (CommandMessage)conn.receive();
				
				
				
				AbstractCommand command = factory.createCommand(message);
				command.setConnection(conn);
				
				executor.execute(command);
				
				
			} catch (ConnectionException e) {
				e.printStackTrace();
			}
		}
		
	}

}
