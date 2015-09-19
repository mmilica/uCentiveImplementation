package org.mobcom.inshopnito.server.recommender;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.mobcom.inshopnito.server.Session;
import org.mobcom.inshopnito.server.SessionManager;
import org.mobcom.inshopnito.server.database.Database;
import org.mobcom.ucentiveshoppingassistant.model.shop.Basket;
import org.mobcom.ucentiveshoppingassistant.model.shop.BasketItem;
import org.mobcom.ucentiveshoppingassistant.model.shop.Product;

public class Recommender {

	private static Recommender instance;
	private int MAX_REC_PROFILE = 2; // max number of recommendations based on profile
	private int MAX_REC_BASKET = 3; // max number of recommendations based on current basket
	
	public static Recommender getInstance() {
		if (instance == null) {
			instance = new Recommender();
		}
		return instance;
	}
	
	private Recommender() {
		
	}
	
	/**
	 * 
	 * @param session current session
	 * @param categoryScores the shopper's predetermined interest profile based on long term behavior
	 * @param basket items currently in the shopping basket
	 * @param recommended id's of products that have already been recommended 
	 * @return recommended products
	 */
	public List<Product> getRecommendations(Session session, Map<String, Double> categoryScores, Basket basket, Set<String> recommended) {
		
		try {

			SortedSet<Recommendation> recommendations = new TreeSet<Recommendation>();
			Set<Product> pool = getProductPool(basket, recommended);
			
			// get recommendations based on predetermined interest profile
			if (categoryScores.size() > 0) {
				List<Recommendation> recs = getRecommendationsByProfile(pool, categoryScores);
				System.out.println(recs.size() + " recommendations based on profile");
				recommendations.addAll(recs);				
			}
			
			// get recommendations based on current basket
			if (session.getNewItems() && basket.getItems().size() > 0) {
				session.setNewItems(false);				
				List<Recommendation> recs = getRecommendationsByBasket(pool, basket);
				System.out.println(recs.size() + " recommendations based on basket contents");
				recommendations.addAll(recs);								
			}
			
			List<Product> recommendedProducts = new ArrayList<Product>();
			for (Recommendation rec : recommendations) {
				recommendedProducts.add(rec.product);
			}
			
			return recommendedProducts;
			
		} catch (Exception e) {
			e.printStackTrace();
			return new ArrayList<Product>();
		}
	}
	
	private List<Recommendation> getRecommendationsByProfile(Set<Product> pool, Map<String, Double> categoryScores) {
		// include high value deals if the customer released their profile
		return getProductScores(pool, categoryScores, true, MAX_REC_PROFILE);
	}
	
	/**
	 * Calculate scores for each product from the pool
	 * @param pool products to recommend from
	 * @param categoryScores scores associated with each product category
	 * @param highValue include high value deals?
	 * @param max maximum number of recommendations
	 * @return
	 */
	private List<Recommendation> getProductScores(Set<Product> pool, Map<String, Double> categoryScores, boolean highValue, int max) {
		
		List<Recommendation> recommendations = new ArrayList<Recommendation>();
		
		for (Product product : pool) {
			
			boolean prioritize = false;
			
			if (product.getId().contains("deal-")) {
				prioritize = true;
				if (!highValue && product.getId().contains("deal-high")) {
					continue;
				} else if (highValue && product.getId().contains("deal-low")) {
					continue;
				}
			}
			
			double score = 3.0;
			if (categoryScores.containsKey(product.getCategory())) {
				score = categoryScores.get(product.getCategory());
				if (prioritize) {
					score += 3*(score-3.0);
				}
			}
			
			//System.out.println("Score " +score + " for " + product);
			if (score > 3.0) {
				recommendations.add(new Recommendation(product, score));
			}
			
		}
		
		return getBestRecommendations(recommendations, max);
		
	}
	
	private List<Recommendation> getRecommendationsByBasket(Set<Product> pool, Basket basket) {
		
		Map<String, Double> categoryScores = new HashMap<String, Double>();

		for (BasketItem item : basket.getItems()) {
			Double score = categoryScores.get(item.getProduct().getCategory()); 
			if (score == null) {
				score = 3.5;
			} else {
				score += 0.25;
			}
			categoryScores.put(item.getProduct().getCategory(), score);
		}
		
		// don't include high value deals for basket-based recommendations
		return getProductScores(pool, categoryScores, false, MAX_REC_BASKET);
	}
	
	private List<Recommendation> getBestRecommendations(List<Recommendation> recommendations, int max) {
		//System.out.println("Get best " + max + " out of " + recommendations.size() +" recommendations");
		Collections.sort(recommendations);
		for (Recommendation r : recommendations) {
			//System.out.println(r.score + ": " + r.product);
		}
		return recommendations.subList(0, Math.min(max, recommendations.size()));
	}
	
	/**
	 * Get the set of products that may be recommended. We don't recommend products that are already
	 * in the shopping basket or that have already been recommended.
	 * @param basket
	 * @param recommended
	 * @return
	 */
	private Set<Product> getProductPool(Basket basket, Set<String> recommended) throws SQLException {		

		Collection<Product> products = Database.getInstance().getAllProducts();
		Set<Product> pool = new HashSet<Product>(products);
		
		int removed = 0;
		
		// remove already recommended products from the pool
		for (Product p : products) {
			if (recommended.contains(p.getId())) {
				pool.remove(p);
				removed++;
			}
		}
		
		// remove products already in the basket from the pool
		for (BasketItem item : basket.getItems()) {
			pool.remove(item.getProduct());
			removed++;
		}
		
		System.out.println("removed " + removed + " products from the pool.");
		
		return pool;		
	}
	
	class Recommendation implements Comparable<Recommendation> {
		
		protected Product product;
		protected Double score;
		
		Recommendation(Product product, Double score) {
			this.product = product;
			this.score = score;
		}

		@Override
		public int compareTo(Recommendation o) {
			int value = -1 * score.compareTo(o.score);			
			if (value == 0) {
				return 1;
			}			
			return value;
		}		
		
	}
	
	public static void main(String[] arg) throws Exception {
		Map<String, Double> categoryScores = new HashMap<String, Double>();
		//categoryScores.put("drinks", 10.0);
		Basket basket = new Basket();
		Product p = Database.getInstance().getProduct("4100590935358");
		System.out.println("Buy " + p);
		basket.add(p, 1);
		Set<String> recommended = new HashSet<String>();
		SessionManager.getInstance().addSession("abcd", new ArrayList<String>(), "");
		Session session = SessionManager.getInstance().getSession("abcd");
		List<Product> recommendations = Recommender.getInstance().getRecommendations(session, categoryScores, basket, recommended);
		for (Product product : recommendations) {
			System.out.println("recommend " + product);
		}
	}
}
