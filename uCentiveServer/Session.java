package org.mobcom.inshopnito.server;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.mobcom.inshopnito.server.database.Database;
import org.mobcom.inshopnito.server.recommender.Recommender;
import org.mobcom.ucentiveshoppingassistant.model.shop.Basket;
import org.mobcom.ucentiveshoppingassistant.model.shop.Product;


public class Session {
	private long lastActive;
	private ArrayList<String> disclosedInfo;
	private String sessionID;
	private Map<String, Double> categoryScores;
	private Basket basket;	
	private Set<String> recommended;
	private boolean newItems;



    public Session(ArrayList<String> disclosedInfo,
			String sessionID, String categoryScores) {
		super();
		this.lastActive = new Date().getTime();
		this.disclosedInfo = disclosedInfo;
		this.sessionID = sessionID;
		this.categoryScores = parseCategoryScores(categoryScores);
		this.basket = new Basket();
		this.recommended = new HashSet<String>();
		this.newItems = false;

        System.out.println("[inShopnito Server] ------------------------- START -------------------------");
		System.out.println("[inShopnito Server] Session ID:\t"+sessionID);
        System.out.println("[inShopnito Server]");
        for(String info : Arrays.toString(disclosedInfo.toArray()).split(",")){

            if(info.contains("http://")){
                System.out.println("[inShopnito Server] Disclosed info:\t"+info.split(";")[2]);
            }
            else
                System.out.println("[inShopnito Server] Disclosed info:\t"+info);
        }
        System.out.println("[inShopnito Server]");
//		System.out.println("[inShopnito Server] Disclosed info: " + Arrays.toString(disclosedInfo.toArray()));

        for(String category : categoryScores.split(",")){
            System.out.println("[inShopnito Server] Disclosed CatScore:\t"+category);
        }

//		System.out.println("[inShopnito Server] Disclosed categoryScores: "+categoryScores);
	}
	
	private Map<String, Double> parseCategoryScores(String scores){
		HashMap<String, Double> scoreMap = new HashMap<String,Double>();
		
		if("".equals(scores)) return scoreMap;
		
		for(String score:scores.split(",")){
			String[] split = score.split("=");
			scoreMap.put(split[0], Double.parseDouble(split[1]));
		}
		return scoreMap;
	}

	public long getLastActive() {
		return lastActive;
	}

	public void setLastActive(long lastActive) {
		this.lastActive = lastActive;
	}

	public ArrayList<String> getDisclosedInfo() {
		return disclosedInfo;
	}

	public String getSessionID() {
		return sessionID;
	}

	public Map<String, Double> getCategoryScores() {
		return categoryScores;
	}
	
	public synchronized void renewSession(){
		setLastActive(new Date().getTime());
	}
	
	public synchronized void addToBasket(String productID, int quantity){		
		
		try {
			int oldSize = basket.getItems().size();
			Product product = Database.getInstance().getProduct(productID);
			basket.add(product, quantity);			
			int newSize = basket.getItems().size();
			System.out.println("Added product "+productID+" to basket of " +sessionID);
			newItems = (newSize > oldSize);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public List<Product> getRecommendations() {
		
		List<Product> recList =  Recommender.getInstance().getRecommendations(this, categoryScores, basket, recommended);		
		
		for (Product rec : recList) {
			recommended.add(rec.getId()); // don't recommend this product again next time
			System.out.println("recommended "+ rec.getId() + "("+rec.getName()+") to "+ sessionID);
		}
		
		return recList;		
	}
	
	/**
	 * does the basket have new items since the last time we sent recommendations?
	 * @return
	 */
	public boolean getNewItems() {
		return this.newItems;
	}
	
	public void setNewItems(boolean value) {
		this.newItems = value;
	}
	
	public synchronized void removeSession(){
		basket.getItems().clear();
	}

    public Basket getBasket() {
        return basket;
    }
}
