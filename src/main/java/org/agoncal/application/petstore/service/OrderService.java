package org.agoncal.application.petstore.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.ejb.Stateless;

import org.agoncal.application.petstore.domain.CartItem;
import org.agoncal.application.petstore.domain.CreditCard;
import org.agoncal.application.petstore.domain.Customer;
import org.agoncal.application.petstore.domain.Order;
import org.agoncal.application.petstore.domain.OrderLine;
import org.agoncal.application.petstore.exception.ValidationException;
import org.agoncal.application.petstore.util.Loggable;
import org.codehaus.jackson.map.ObjectMapper;

import com.couchbase.client.CouchbaseClient;
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
public class OrderService implements Serializable {

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

    public OrderService() {
    	/** Get Couchbase client and Json mapper **/
    	client = DBPopulator.getClient();
    	mapper = DBPopulator.getMapper();
    }

    /** Write order to the database using the login name as the key
     *	and using mapper to convert the customer object to json. The
     *	id will be the firstname of the customer with the epoch time
     *	in milliseconds 
     **/
    public Order createOrder(final Customer customer, final CreditCard creditCard, final List<CartItem> cartItems) {

        // OMake sure the object is valid
        if (cartItems == null || cartItems.size() == 0)
            throw new ValidationException("Shopping cart is empty"); // TODO exception bean validation

        // Creating the order
        Order order = new Order(customer, creditCard, customer.getHomeAddress());

        // From the shopping cart we create the order lines
        List<OrderLine> orderLines = new ArrayList<OrderLine>();

        for (CartItem cartItem : cartItems) {
        	Long currentItem = 1L;
        	OrderLine orderLine = new OrderLine(cartItem.getQuantity(), cartItem.getItem());
        	orderLine.setId(currentItem++);
            orderLines.add(orderLine);
        }
        order.setOrderLines(orderLines);

        order.setOrderDate(new Date());
        order.setId(customer.getFirstname() + "-" + order.getOrderDate().getTime());

        // Persists the object to the database
        try {
			client.set(order.getId(), EXP_TIME, mapper.writeValueAsString(order));
		} catch (Exception ex) {
			ex.printStackTrace();
		}
        return order;
    }

    /**
     * Try to get a customer from the database using the key which is the order
     * id. This will also map it back into object form using mapper.readValue
     */
    public Order findOrder(String orderId) {
        if (orderId == null)
            throw new ValidationException("Invalid order id");

        Order order = null;
        try {
			order = mapper.readValue((String) client.get(orderId), Order.class);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
        return order;
    }

    /**
     * Returns all of the orders in the database.
     */
    public List<Order> findAllOrders() {

    	List<Order> orders = new ArrayList<Order>();

    	// Gets a reference to the all orders view
    	View view = client.getView("orders", "all");

    	// Create a new View Query
    	Query query = new Query();
    	query.setIncludeDocs(true); // Include the full document as well

    	// Query the Cluster and return the View Response
    	ViewResponse result = client.query(view, query);

    	// Iterate over the results and add all the orders to the order List
    	Iterator<ViewRow> itr = result.iterator();

    	while(itr.hasNext()) {
    	  ViewRow row = itr.next();

    	  Order order = null;
    	  try {
    		  order = mapper.readValue((String) row.getDocument(), Order.class);
    	  } catch (Exception ex) {
    		  ex.printStackTrace();
    	  }
    	  if (order != null) {
    		  orders.add(order);
    	  }
    	}

    	return orders;
    }

    /**
     * Removes a customer from the database using the delete function
     */
    public void removeOrder(Order order) {
        if (order == null)
            throw new ValidationException("Order object is null");

        client.delete(order.getId());
    }
}
