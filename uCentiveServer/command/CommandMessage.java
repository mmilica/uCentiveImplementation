package org.mobcom.inshopnito.server.command;

import java.io.Serializable;

public class CommandMessage implements Serializable{

	private static final long serialVersionUID = 1L;
	private String commandWord;
	private Serializable[] arguments;
	private String sessionID = "";
	
	public CommandMessage(String commandWord, Serializable... arguments){
		this.commandWord = commandWord;
		this.arguments = arguments;
	}
	
	public CommandMessage(String commandWord, String sessionID, Serializable... arguments){
		this.commandWord = commandWord;
		this.arguments = arguments;
		this.sessionID = sessionID;
	}

	public String getCommandWord() {
		return commandWord;
	}

	public Serializable[] getArguments() {
		return arguments;
	}
	
	public String getSessionID(){
		return sessionID;
	}
	
}
