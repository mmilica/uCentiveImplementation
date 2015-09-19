package org.mobcom.inshopnito.server.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.mobcom.ucentiveshoppingassistant.model.shop.Product;

public class Database {

	private static Database instance;	
	private String connstring;
	private Connection connection;
	private Map<String, Product> productCache;
	
	private String dbclass = "com.mysql.jdbc.Driver";
	private String host = "localhost:3307";
	private String db = "inShopnito";
	private String user = "mobcom";
	private String paswd = "inShopnito";
	
	
	/**
	 * Gets a new or existing instance of the database object. If the connection was closed since the last
	 * database operation, it will be reopened.
	 * @return
	 * @throws SQLException
	 */
	public static Database getInstance() throws SQLException {
		if (instance == null) {
			instance = new Database();			
		} else if (instance.isClosed()) {
			instance.openConnection();
		}
		return instance;
	}
	
	private Database() throws SQLException {
		this.productCache = new HashMap<String, Product>();
		this.connstring = "jdbc:mysql://"+host+"/"+db+"?user="+user+"&password="+paswd;
		System.out.println("Connecting to " + connstring);
		openConnection();
	}
	
	private void openConnection() throws SQLException {		
		try {
			Class.forName(dbclass);
			this.connection = DriverManager.getConnection(connstring);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private boolean isClosed() throws SQLException {
		return (connection == null || connection.isClosed());
	}
	
	/**
	 * Close the database connection
	 */
	public void close() {
		try {
			connection.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Get product metadata
	 * @param id
	 * @return
	 * @throws SQLException
	 */
	public Product getProduct(String id) throws SQLException {
		Product product = productCache.get(id);
		if (product == null) {
			//System.out.println("... get product " + id + " from database");
			PreparedStatement ps = connection.prepareStatement("SELECT * FROM itemmeta WHERE id = ?");
			ps.setString(1, id);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				product = new Product(id);
				product.setName(rs.getString("title"));
				product.setPrice(rs.getDouble("price"));				
				productCache.put(id, product);
			}
			rs.close();
			ps.close();
			if (product != null) {
				ps = connection.prepareStatement("SELECT category FROM itemmeta_category WHERE itemid = ?");
				ps.setString(1, id);
				rs = ps.executeQuery();
				while (rs.next()) {
					product.setCategory(rs.getString("category"));					
				}
				rs.close();
				ps.close();
			}
		}		
		return product;
	}
	
	/**
	 * Cache all product metadata for faster recommendations
	 */
	public void cacheAllProducts() throws SQLException {
		PreparedStatement ps = connection.prepareStatement("SELECT id FROM itemmeta");		
		ResultSet rs = ps.executeQuery();
		while (rs.next()) {
			//System.out.println("Get product " + rs.getString("id"));
			getProduct(rs.getString("id"));
		}
	}
	
	/**
	 * Clear the product metadata cache
	 */
	public void flushProductCache() {
		productCache.clear();
	}
	
	/**
	 * Get metadata for all available products
	 * @return
	 */
	public Collection<Product> getAllProducts() throws SQLException {
		cacheAllProducts();
		return productCache.values();
	}
	
	/**
	 * Test methods
	 * @param arg
	 * @throws Exception
	 */
	public static void main(String[] arg) throws Exception {
		//Database.getInstance().cacheAllProducts();
		System.out.println(Database.getInstance().getProduct("3058320026030"));
		System.out.println(Database.getInstance().getProduct("4100590935358"));
		System.out.println(Database.getInstance().getProduct("3058320026030"));
	}
}
