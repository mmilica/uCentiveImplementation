package org.mobcom.inshopnito.server;


import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SessionManager {
	private static SessionManager instance;
	
	private List<Session> sessions;
	
	private SessionManager(){
		this.sessions = new ArrayList<Session>();
	}
	
	public static SessionManager getInstance(){
		if(instance==null)instance = new SessionManager();
		return instance;
	}
	

	
	public Session getSession(String sessionID){
		for(Session session:sessions){
			if(session.getSessionID().equals(sessionID))
				return session;
		}
		return null;
	}
	
	public synchronized void addSession(String sessionID,ArrayList<String> disclosedInfo, String categoryScores){
		
		Session session = getSession(sessionID);
		if(session!=null)
			session.renewSession();
		else{
			session = new Session(disclosedInfo,sessionID,categoryScores);
			sessions.add(session);
		}
		
		
	}
	public synchronized boolean removeSession(String sessionID){
		Session session = getSession(sessionID);
		if(session!=null){
			session.removeSession();
			return sessions.remove(session);
		}
		else return false;
	}
	
	public  Map<String, Double> getCategoryScores(String sessionID){
		Session session = getSession(sessionID);
		if(session==null)return new HashMap<String, Double>();
		return session.getCategoryScores();
	}
}
