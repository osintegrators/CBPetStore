package org.agoncal.application.petstore.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.ejb.Stateless;

import org.agoncal.application.petstore.domain.Category;
import org.agoncal.application.petstore.domain.Item;
import org.agoncal.application.petstore.domain.Product;
import org.agoncal.application.petstore.exception.ValidationException;
import org.agoncal.application.petstore.util.Loggable;
import org.codehaus.jackson.map.ObjectMapper;

import com.couchbase.client.CouchbaseClient;
import com.couchbase.client.protocol.views.ComplexKey;
import com.couchbase.client.protocol.views.Query;
import com.couchbase.client.protocol.views.View;
import com.couchbase.client.protocol.views.ViewResponse;
import com.couchbase.client.protocol.views.ViewRow;

/**
 * @author Antonio Goncalves
 *         http://www.antoniogoncalves.org
 *         --
 */

@Stateless
@Loggable
public class CatalogService implements Serializable {

    // ======================================
    // =             Attributes             =
    // ======================================


    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public static CouchbaseClient client = null;
    public static ObjectMapper mapper = null;
    
    // Setting the expire time to 0 means never expire
    public static final int EXP_TIME = 0;

    // ======================================
    // =              Public Methods        =
    // ======================================

    public CatalogService() {
    	/** Get Couchbase client and Json mapper **/
    	client = DBPopulator.getClient();
    	mapper = DBPopulator.getMapper();
    }

    /**
     * Finds a category by Id by using the get command.
     */
    public Category findCategory(String categoryName) {
        if (categoryName == null)
            throw new ValidationException("Invalid category name");

        Category category = null;
		try {
			category = mapper.readValue((String) client.get(categoryName), Category.class);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return category;
    }

    /**
     * Uses the all categories view to find all of the categories
     */
    public List<Category> findAllCategories() {
    	List<Category> categories = new ArrayList<Category>();

    	View view = client.getView("categories", "all");

    	// Create a new View Query
    	Query query = new Query();
    	query.setIncludeDocs(true); // Include the full document as well

    	// Query the Cluster and return the View Response
    	ViewResponse result = client.query(view, query);

    	// Iterate over the results and maps it to a category and adds the
    	// category to the List
    	Iterator<ViewRow> itr = result.iterator();

    	while(itr.hasNext()) {
    	  ViewRow row = itr.next();

    	  Category category = null;
    	  try {
    		  category = mapper.readValue((String) row.getDocument(), Category.class);
    	  } catch (Exception e) {
    		  e.printStackTrace();
    	  }
    	  categories.add(category);
    	}
    	return categories;
    }

    /**
     * Adds a new category to the database by using the set command.
     */
    public Category createCategory(Category category) {
        if (category == null)
            throw new ValidationException("Category object is null");

        category.setId(category.getName() + "-" + new Date().getTime());
        try {
			client.set(category.getName(), EXP_TIME, mapper.writeValueAsString(category));
		} catch (Exception ex) {
			ex.printStackTrace();
		}

        return category;
    }

    /**
     * Updates a category with the new object by calling the replace command
     */
    public Category updateCategory(Category category) {
        if (category == null)
            throw new ValidationException("Category object is null");

        try {
			client.replace(category.getName(), EXP_TIME, mapper.writeValueAsString(category));
		} catch (Exception ex) {
			ex.printStackTrace();
		}
        return category;
    }

    /**
     * Removes the specified category from the database. This will also
     * remove all products and items in this category.
     */
    public void removeCategory(Category category) {
        if (category == null)
            throw new ValidationException("Category object is null");

        client.delete(category.getName());
    }

    /**
     * Removes a category by finding it and then submitting it to
     * the removeCategory by object function
     */
    public void removeCategory(String categoryId) {
        if (categoryId == null)
            throw new ValidationException("Invalid category id");

        removeCategory(findCategory(categoryId));
    }

    /**
     * Finds the category that matches the categoryName and then
     * adds all of the products that match that category to the
     * List for return
     */
    public List<Product> findProducts(String categoryName) {
        if (categoryName == null)
            throw new ValidationException("Invalid category name");

        List<Product> products = null;

    	Category category = findCategory(categoryName);
		products = new ArrayList<Product>();
       	for (Product product : category.getProducts()) {
       		products.add(product);
       	}

    	return products;
    }

    /**
     * Queries the product view for products that match the id. It
     * will then pull back the category match and then find the product
     * in there and return that product. This is due to the fact that
     * you can only pull back the whole document which is a category.
     */
    public Product findProduct(String productId) {
        if (productId == null)
            throw new ValidationException("Invalid product id");

    	View view = client.getView("categories", "products");
    	// Create a new View Query
    	Query query = new Query();
    	query.setRangeStart(ComplexKey.of(productId));
    	query.setRangeEnd(ComplexKey.of(productId + "\uefff"));
    	query.setIncludeDocs(true); // Include the full document as well

    	// Query the Cluster and return the View Response
    	ViewResponse result = client.query(view, query);

    	// Iterate over the results and print out some info
    	Iterator<ViewRow> itr = result.iterator();

    	Product product = null;

    	while(itr.hasNext()) {
    		ViewRow row = itr.next();
    		Category category = null;
	  		try {
	  			String json = (String) row.getDocument();
	  			category = mapper.readValue(json, Category.class);
	  			for (Product tProduct : category.getProducts()) {
  		  	       	if (tProduct.getId().equals(productId)) {
	  		  	       	product = tProduct;
  		  	       	}
	  			}
	  			
	  		} catch (Exception ex) {
	  			ex.printStackTrace();
	  		}
    	}
    	return product;
    }

    /**
     * Finds all the categories and then adds all the products
     * associated with those categories to the List for returning
     */
    public List<Product> findAllProducts() {
        List<Product> products = null;

    	List<Category> categories = findAllCategories();
    	for (Category category : categories) {
			products = new ArrayList<Product>();
	       	for (Product product : category.getProducts()) {
	       		products.add(product);
	       	}
    	}

    	return products;
    }

    /**
     * Finds the category associated with the product, then adds
     * the product to that category and then updates the category
     */
    public Product createProduct(Product product) {
        if (product == null)
            throw new ValidationException("Product object is null");

        Category category = product.getCategory();
        product.setId(product.getType() + "_" + product.getName());
        category.addProduct(product);
        updateCategory(category);

        return product;
    }

    /**
     * Updates the product by finding it in the category, removing it
     * and then readding the updated version back. It then performs
     * an update on the category.
     */
    public Product updateProduct(Product product) {
        if (product == null)
            throw new ValidationException("Product object is null");

        Category category = product.getCategory();
        List<Product> products = category.getProducts();
        for (Product tProduct : products) {
        	if (tProduct.getId().equals(product.getId())) {
        		products.remove(tProduct);
        		products.add(product);
        	}
        }
        category.setProducts(products);
        updateCategory(category);
        return product;
    }

    /**
     * Gets the category the product is assigned to and then finds the 
     * product in that category and then updates the category. This is 
     * due to the fact that all of the data is stored in the category
     * document
     */
    public void removeProduct(Product product) {
        if (product == null)
            throw new ValidationException("Product object is null");

        Category category = product.getCategory();
        List<Product> products = category.getProducts();
        for (Product tProduct : products) {
        	if (tProduct.getId().equals(product.getId())) {
        		products.remove(tProduct);
        
        	}
        }
        updateCategory(category);
    }

    public void removeProduct(String productId) {
        if (productId == null)
            throw new ValidationException("Invalid product id");

        removeProduct(findProduct(productId));
    }

    /**
     * Finding items requires pulling back the category document that the item
     * belongs to. It finds the product that has been submitted and then returns
     * all of the items belonging to the product
     */
    public List<Item> findItems(String productId) {
        if (productId == null)
            throw new ValidationException("Invalid product id");

        List<Item> items = null;

    	View view = client.getView("categories", "products");

    	// Create a new View Query
    	Query query = new Query();
    	query.setRangeStart(ComplexKey.of(productId));
    	query.setRangeEnd(ComplexKey.of(productId + "\uefff"));
    	query.setIncludeDocs(true); // Include the full document as well

    	// Query the Cluster and return the View Response
    	ViewResponse result = client.query(view, query);

    	// Iterate over the results and print out some info
    	Iterator<ViewRow> itr = result.iterator();

    	while(itr.hasNext()) {
    		ViewRow row = itr.next();
    		Category category = null;
    		items = new ArrayList<Item>();
	  		try {
	  			String json = (String) row.getDocument();
	  			category = mapper.readValue(json, Category.class);
	  			for (Product product : category.getProducts()) {
	  				if (product.getId().equals(productId)) {
	  		  	       	for (Item item : product.getItems()) {
	  		  	       		items.add(item);
	  		  	       	}
	  				}
	  			}
	  			
	  		} catch (Exception ex) {
	  			ex.printStackTrace();
	  		}
    	}
    	return items;
    }

    /**
     * Finding items requires pulling back the category document that the item
     * belongs to. It then finds the product and items and item that matches
     */
    public Item findItem(final String itemId) {
        if (itemId == null)
            throw new ValidationException("Invalid item id");

    	View view = client.getView("categories", "items");
    	// Create a new View Query
    	Query query = new Query();
    	query.setRangeStart(ComplexKey.of(itemId));
    	query.setRangeEnd(ComplexKey.of(itemId + "\uefff"));
    	query.setIncludeDocs(true); // Include the full document as well

    	// Query the Cluster and return the View Response
    	ViewResponse result = client.query(view, query);

    	// Iterate over the results and print out some info
    	Iterator<ViewRow> itr = result.iterator();

    	Item item = null;

    	while(itr.hasNext()) {
    		ViewRow row = itr.next();
    		Category category = null;
	  		try {
	  			String json = (String) row.getDocument();
	  			category = mapper.readValue(json, Category.class);
	  			for (Product product : category.getProducts()) {
  		  	       	for (Item tItem : product.getItems()) {
	  		  	       	if (tItem.getId().equals(itemId)) {
	  		  	       		item = tItem;
	  		  	       	}
  		  	       	}
	  			}
	  			
	  		} catch (Exception ex) {
	  			ex.printStackTrace();
	  		}
    	}
    	return item;
    }

    /**
     * Performs a search of items using the keyword from the web page.
     */
    public List<Item> searchItems(String keyword) {
        if (keyword == null)
            keyword = "";

    	List<Item> items = new ArrayList<Item>();
        
        View view = client.getView("categories", "items");
    	// Create a new View Query
    	Query query = new Query();
    	query.setRangeStart(ComplexKey.of(keyword));
    	query.setRangeEnd(ComplexKey.of(keyword + "\uefff"));
    	query.setIncludeDocs(true); // Include the full document as well

    	// Query the Cluster and return the View Response
    	ViewResponse result = null;
    	try {
    		result = client.query(view, query);
    	} catch (Exception ex) {
    		System.out.println("Exception: " + ex.getMessage());
    	}

    	// Iterate over the results and print out some info
    	Iterator<ViewRow> itr = result.iterator();

    	while(itr.hasNext()) {
    		System.out.println("Iterator has something");
    		ViewRow row = itr.next();
    		Category category = null;
	  		try {
	  			String json = (String) row.getDocument();
	  			category = mapper.readValue(json, Category.class);
	  			for (Product product : category.getProducts()) {
  		  	       	for (Item item : product.getItems()) {
	  		  	       	if (item.getId().contains(keyword) || item.getName().contains(keyword)) {
	  		  	       		items.add(item);
	  		  	       	}
  		  	       	}
	  			}
	  			
	  		} catch (Exception ex) {
	  			ex.printStackTrace();
	  		}
    	}
    	return items;
    }

    /**
     * Finding items requires pulling back the category document that the item
     * belongs to. It then finds the product and items and returns all of the
     * items
     */
    public List<Item> findAllItems() {
    	List<Item> items = new ArrayList<Item>();
        
        View view = client.getView("categories", "items");
    	// Create a new View Query
    	Query query = new Query();
    	query.setIncludeDocs(true); // Include the full document as well

    	// Query the Cluster and return the View Response
    	ViewResponse result = null;
    	try {
    		result = client.query(view, query);
    	} catch (Exception ex) {
    		System.out.println("Exception: " + ex.getMessage());
    	}

    	// Iterate over the results and print out some info
    	Iterator<ViewRow> itr = result.iterator();

    	while(itr.hasNext()) {
    		ViewRow row = itr.next();
    		Category category = null;
	  		try {
	  			String json = (String) row.getDocument();
	  			category = mapper.readValue(json, Category.class);
	  			for (Product product : category.getProducts()) {
  		  	       	for (Item item : product.getItems()) {
  		  	       		items.add(item);
  		  	       	}
	  			}
	  			
	  		} catch (Exception ex) {
	  			ex.printStackTrace();
	  		}
    	}
    	return items;
    }

    /**
     * Creates an item and then adds it to a product and then updates
     * the product. 
     */
    public Item createItem(Item item) {
        if (item == null)
            throw new ValidationException("Item object is null");

        Product product = item.getProduct();
        item.setId(product.getType() + "_" + product.getName());
        product.addItem(item);
        updateProduct(product);

        return item;
    }

    /**
     * Finds an item in the products and then updates it and adds
     * it back to the products. 
     */
    public Item updateItem(Item item) {
        if (item == null)
            throw new ValidationException("Item object is null");

        Product product = item.getProduct();
        List<Item> items = product.getItems();
        for (Item tItem : items) {
        	if (tItem.getId().equals(item.getId())) {
        		items.remove(tItem);
        		items.add(item);
        	}
        }
        product.setItems(items);
        updateProduct(product);
        return item;
    }

    /**
     * Finds the product that the item belongs to and then removes that item
     * from the product. It then calls the update product function
     */
    public void removeItem(Item item) {
        if (item == null)
            throw new ValidationException("Item object is null");

        Product product = item.getProduct();
        List<Item> items = product.getItems();
        for (Item tItem : items) {
        	if (tItem.getId().equals(item.getId())) {
        		items.remove(tItem);
        	}
        }
        product.setItems(items);
        updateProduct(product);
    }

    /**
     * Removes an item from the database using the delete function by finding
     * the item and sending it to the remove item function.
     */
    public void removeItem(String itemId) {
        if (itemId == null)
            throw new ValidationException("itemId is null");

        removeItem(findItem(itemId));
    }
}
