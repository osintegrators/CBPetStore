package org.agoncal.application.petstore.service;

import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import net.spy.memcached.internal.OperationFuture;

import org.agoncal.application.petstore.domain.CartItem;
import org.agoncal.application.petstore.domain.CreditCard;
import org.agoncal.application.petstore.domain.Customer;
import org.agoncal.application.petstore.domain.Order;
import org.agoncal.application.petstore.domain.OrderLine;
import org.agoncal.application.petstore.exception.ValidationException;
import org.agoncal.application.petstore.util.Loggable;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import com.couchbase.client.CouchbaseClient;
import com.couchbase.client.protocol.views.Query;
import com.couchbase.client.protocol.views.View;
import com.couchbase.client.protocol.views.ViewResponse;
import com.couchbase.client.protocol.views.ViewRow;
import com.fasterxml.jackson.core.JsonParser;

/**
 * @author Antonio Goncalves
 *         http://www.antoniogoncalves.org
 *         --
 */

@Stateless
@Loggable
public class OrderService implements Serializable {

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

    public OrderService() {
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

    public Order createOrder(final Customer customer, final CreditCard creditCard, final List<CartItem> cartItems) {

        // OMake sure the object is valid
        if (cartItems == null || cartItems.size() == 0)
            throw new ValidationException("Shopping cart is empty"); // TODO exception bean validation

        // Creating the order
        Order order = new Order(customer, creditCard, customer.getHomeAddress());

        // From the shopping cart we create the order lines
        List<OrderLine> orderLines = new ArrayList<OrderLine>();

        for (CartItem cartItem : cartItems) {
            orderLines.add(new OrderLine(cartItem.getQuantity(), em.merge(cartItem.getItem())));
        }
        order.setOrderLines(orderLines);

        // Persists the object to the database
        //em.persist(order);
        order.setId(customer.getFirstname());

        try {
			client.set(order.getId(), EXP_TIME, mapper.writeValueAsString(order));
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

        return order;
    }

    public Order findOrder(String orderId) {
        if (orderId == null)
            throw new ValidationException("Invalid order id");

        //return em.find(Order.class, orderId);
        return mapper.convertValue(client.get(orderId), Order.class);
    }

    public List<Order> findAllOrders() {
        //TypedQuery<Order> typedQuery = em.createNamedQuery(Order.FIND_ALL, Order.class);
        //return typedQuery.getResultList();

    	List<Order> orders = new ArrayList<Order>();

    	View view = client.getView("orders", "all");

    	// Create a new View Query
    	Query query = new Query();
    	query.setIncludeDocs(true); // Include the full document as well

    	// Query the Cluster and return the View Response
    	ViewResponse result = client.query(view, query);

    	// Iterate over the results and print out some info
    	Iterator<ViewRow> itr = result.iterator();

    	while(itr.hasNext()) {
    	  ViewRow row = itr.next();

    	  Order order = mapper.convertValue(row.getDocument(), Order.class);
    	  orders.add(order);
    	}

    	return orders;
    }

    public void removeOrder(Order order) {
        if (order == null)
            throw new ValidationException("Order object is null");

        //em.remove(em.merge(order));
        client.delete(order.getId());
    }
}
