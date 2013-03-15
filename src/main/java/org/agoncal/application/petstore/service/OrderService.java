package org.agoncal.application.petstore.service;

import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import net.spy.memcached.internal.OperationFuture;

import org.agoncal.application.petstore.domain.CartItem;
import org.agoncal.application.petstore.domain.CreditCard;
import org.agoncal.application.petstore.domain.Customer;
import org.agoncal.application.petstore.domain.Order;
import org.agoncal.application.petstore.domain.OrderLine;
import org.agoncal.application.petstore.exception.ValidationException;
import org.agoncal.application.petstore.util.Loggable;

import com.couchbase.client.CouchbaseClient;
import com.google.gson.Gson;

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
    public static Gson gson = null;
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

        gson = new Gson();
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

        // Do an asynchronous set
        client.set(order.getId(), EXP_TIME, gson.toJson(order));

        return order;
    }

    public Order findOrder(Long orderId) {
        if (orderId == null)
            throw new ValidationException("Invalid order id");

        return em.find(Order.class, orderId);
    }

    public List<Order> findAllOrders() {
    	//List<Order> = new ArrayList<Order>();
        TypedQuery<Order> typedQuery = em.createNamedQuery(Order.FIND_ALL, Order.class);
        return typedQuery.getResultList();
    }

    public void removeOrder(Order order) {
        if (order == null)
            throw new ValidationException("Order object is null");

        em.remove(em.merge(order));
    }
}
