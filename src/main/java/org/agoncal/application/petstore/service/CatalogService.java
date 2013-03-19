package org.agoncal.application.petstore.service;

import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import org.agoncal.application.petstore.domain.Category;
import org.agoncal.application.petstore.domain.Item;
import org.agoncal.application.petstore.domain.Product;
import org.agoncal.application.petstore.exception.ValidationException;
import org.agoncal.application.petstore.util.Loggable;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
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

    @Inject
    private EntityManager em;

    public static CouchbaseClient client = null;
    public static ObjectMapper mapper = null;
    public static final int EXP_TIME = 0;

    // ======================================
    // =              Public Methods        =
    // ======================================

    public CatalogService() {
        // Set the URIs and get a client
        List<URI> uris = new LinkedList<URI>();

        // Connect to localhost or to the appropriate URI(s)
        uris.add(URI.create("http://localhost:8091/pools"));

        
        try {
          // Use the "default" bucket with no password
          client = new CouchbaseClient(uris, "petstore", "");
        } catch (IOException e) {
          System.err.println("IOException connecting to Couchbase: " + e.getMessage());
        }

        mapper = new ObjectMapper();
    }

    @Override
    public void finalize() {
    	client.shutdown();
    }

    /*public Category findCategory(Long categoryId) {
        if (categoryId == null)
            throw new ValidationException("Invalid category id");

        return em.find(Category.class, categoryId);
    }*/

    public Category findCategory(String categoryName) {
        if (categoryName == null)
            throw new ValidationException("Invalid category name");

        //TypedQuery<Category> typedQuery = em.createNamedQuery(Category.FIND_BY_NAME, Category.class);
        //typedQuery.setParameter("pname", categoryName);
        //return typedQuery.getSingleResult();
        return mapper.convertValue(client.get(categoryName), Category.class);
    }

    public List<Category> findAllCategories() {
        //TypedQuery<Category> typedQuery = em.createNamedQuery(Category.FIND_ALL, Category.class);
        //return typedQuery.getResultList();

    	List<Category> categories = new ArrayList<Category>();

    	View view = client.getView("categories", "all");

    	// Create a new View Query
    	Query query = new Query();
    	query.setIncludeDocs(true); // Include the full document as well

    	// Query the Cluster and return the View Response
    	ViewResponse result = client.query(view, query);

    	// Iterate over the results and print out some info
    	Iterator<ViewRow> itr = result.iterator();

    	while(itr.hasNext()) {
    	  ViewRow row = itr.next();

    	  Category category = mapper.convertValue(row.getDocument(), Category.class);
    	  categories.add(category);
    	}
    	return categories;
    }

    public Category createCategory(Category category) {
        if (category == null)
            throw new ValidationException("Category object is null");

        //em.persist(category);

        category.setId(category.getType() + "_" + category.getName());
        try {
			client.set(category.getName(), EXP_TIME, mapper.writeValueAsString(category));
		} catch (JsonGenerationException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (JsonMappingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

        return category;
    }

    public Category updateCategory(Category category) {
        if (category == null)
            throw new ValidationException("Category object is null");

        return em.merge(category);
    }

    public void removeCategory(Category category) {
        if (category == null)
            throw new ValidationException("Category object is null");

        //em.remove(em.merge(category));
        client.delete(category.getId());
    }

    public void removeCategory(String categoryId) {
        if (categoryId == null)
            throw new ValidationException("Invalid category id");

        removeCategory(findCategory(categoryId));
    }

    public List<Product> findProducts(String categoryName) {
        if (categoryName == null)
            throw new ValidationException("Invalid category name");

        List<Product> products = null;

    	Category category;
		try {
			category = mapper.readValue((String) client.get(categoryName), Category.class);
			products = new ArrayList<Product>();
	       	for (Product product : category.getProducts()) {
	       		products.add(product);
	       	}
		} catch (JsonParseException e) {
			e.printStackTrace();
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

    	return products;
    }

    public Product findProduct(String productId) {
        if (productId == null)
            throw new ValidationException("Invalid product id");

        //Product product = em.find(Product.class, productId);
        //if (product != null) {
        //    product.getItems(); // TODO check lazy loading
        //}
        //return product;
        return mapper.convertValue(client.get(productId), Product.class);
    }

    public List<Product> findAllProducts() {
        //TypedQuery<Product> typedQuery = em.createNamedQuery(Product.FIND_ALL, Product.class);
        //return typedQuery.getResultList();

    	List<Product> products = new ArrayList<Product>();

    	View view = client.getView("products", "all");

    	// Create a new View Query
    	Query query = new Query();
    	query.setIncludeDocs(true); // Include the full document as well

    	// Query the Cluster and return the View Response
    	ViewResponse result = client.query(view, query);

    	// Iterate over the results and print out some info
    	Iterator<ViewRow> itr = result.iterator();

    	while(itr.hasNext()) {
    	  ViewRow row = itr.next();

    	  Product product = mapper.convertValue(row.getDocument(), Product.class);
    	  products.add(product);
    	}
    	return products;
    }

    public Product createProduct(Product product) {
        if (product == null)
            throw new ValidationException("Product object is null");

        //if (product.getCategory() != null && product.getCategory().getId() == null)
        //    em.persist(product.getCategory());

        //em.persist(product);

        product.setId(product.getType() + "_" + product.getName());
        try {
			client.set(product.getName(), EXP_TIME, mapper.writeValueAsString(product));
		} catch (JsonGenerationException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (JsonMappingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

        return product;
    }

    public Product updateProduct(Product product) {
        if (product == null)
            throw new ValidationException("Product object is null");

        return em.merge(product);
    }

    public void removeProduct(Product product) {
        if (product == null)
            throw new ValidationException("Product object is null");

        //em.remove(em.merge(product));
        client.delete(product.getId());
    }

    public void removeProduct(String productId) {
        if (productId == null)
            throw new ValidationException("Invalid product id");

        removeProduct(findProduct(productId));
    }

    public List<Item> findItems(String productId) {
        if (productId == null)
            throw new ValidationException("Invalid product id");

        //TypedQuery<Item> typedQuery = em.createNamedQuery(Item.FIND_BY_PRODUCT_ID, Item.class);
        //typedQuery.setParameter("productId", productId);
        //return typedQuery.getResultList();
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
	  			
	  		} catch (JsonParseException e) {
	  			e.printStackTrace();
	  		} catch (JsonMappingException e) {
	  			e.printStackTrace();
	  		} catch (IOException e) {
	  			e.printStackTrace();
	  		}
    	}
    	return items;
    }

    public Item findItem(final String itemId) {
    	System.out.println("Item ID: " + itemId);
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
    		System.out.println("Iterator has something");
    		ViewRow row = itr.next();
    		Category category = null;
	  		try {
	  			String json = (String) row.getDocument();
	  			category = mapper.readValue(json, Category.class);
	  			System.out.println("Category: " + category.getName());
	  			for (Product product : category.getProducts()) {
  		  	       	for (Item tItem : product.getItems()) {
	  		  	       	if (tItem.getId().equals(itemId)) {
	  		  	       		item = tItem;
	  		  	       	}
  		  	       	}
	  			}
	  			
	  		} catch (JsonParseException e) {
	  			e.printStackTrace();
	  		} catch (JsonMappingException e) {
	  			e.printStackTrace();
	  		} catch (IOException e) {
	  			e.printStackTrace();
	  		}
    	}
    	return item;
        //return em.find(Item.class, itemId);
    }

    public List<Item> searchItems(String keyword) {
        if (keyword == null)
            keyword = "";

        TypedQuery<Item> typedQuery = em.createNamedQuery(Item.SEARCH, Item.class);
        typedQuery.setParameter("keyword", "%" + keyword.toUpperCase() + "%");
        return typedQuery.getResultList();
    }

    public List<Item> findAllItems() {
        //TypedQuery<Item> typedQuery = em.createNamedQuery(Item.FIND_ALL, Item.class);
        //return typedQuery.getResultList();
    	List<Item> items = new ArrayList<Item>();

    	View view = client.getView("items", "all");

    	// Create a new View Query
    	Query query = new Query();
    	query.setIncludeDocs(true); // Include the full document as well

    	// Query the Cluster and return the View Response
    	ViewResponse result = client.query(view, query);

    	// Iterate over the results and print out some info
    	Iterator<ViewRow> itr = result.iterator();

    	while(itr.hasNext()) {
    	  ViewRow row = itr.next();

    	  Item item = mapper.convertValue(row.getDocument(), Item.class);
    	  items.add(item);
    	}
    	return items;
    }

    public Item createItem(Item item) {
        if (item == null)
            throw new ValidationException("Item object is null");

        //if (item.getProduct() != null && item.getProduct().getId() == null) {
        //    em.persist(item.getProduct());
        //    if (item.getProduct().getCategory() != null && item.getProduct().getCategory().getId() == null)
        //        em.persist(item.getProduct().getCategory());
        //}

        //em.persist(item);

        item.setId(item.getType() + "_" + item.getName());
        try {
			client.set(item.getName(), EXP_TIME, mapper.writeValueAsString(item));
		} catch (JsonGenerationException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (JsonMappingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

        return item;
    }

    public Item updateItem(Item item) {
        if (item == null)
            throw new ValidationException("Item object is null");

        return em.merge(item);
    }

    public void removeItem(Item item) {
        if (item == null)
            throw new ValidationException("Item object is null");

        client.delete(item.getId());
        //em.remove(em.merge(item));
    }

    public void removeItem(String itemId) {
        if (itemId == null)
            throw new ValidationException("itemId is null");

        removeItem(findItem(itemId));
    }
}
