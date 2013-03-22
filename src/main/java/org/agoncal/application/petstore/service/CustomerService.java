package org.agoncal.application.petstore.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.ejb.Stateless;

import org.agoncal.application.petstore.domain.Customer;
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
 * 
 */

@Stateless
@Loggable
public class CustomerService implements Serializable {

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

    public CustomerService() {
    	/** Get Couchbase client and Json mapper **/
    	client = DBPopulator.getClient();
    	mapper = DBPopulator.getMapper();
    }

    public boolean doesLoginAlreadyExist(final String login) {

        if (login == null)
            throw new ValidationException("Login cannot be null");

        Customer customer = findCustomer(login);
        if (customer == null) {
        	return false;
        }
        return true;
    }

    /** Write customer to the database using the login name as the key
     *	and using mapper to convert the customer object to json. The
     *	id will be the epoch time in milliseconds 
     **/
    public Customer createCustomer(final Customer customer) {

        if (customer == null)
            throw new ValidationException("Customer object is null");

        customer.setId(String.valueOf(new Date().getTime()));
        try {
			client.set(customer.getLogin(), EXP_TIME, mapper.writeValueAsString(customer));
		} catch (Exception ex) {
			ex.printStackTrace();
		}
        return customer;
    }

    /**
     * Try to get a customer from the database using the key which is the login
     * name. This will also map it back into object form using mapper.readValue
     */
    public Customer findCustomer(final String login) {

        if (login == null)
            throw new ValidationException("Invalid login");

        Customer customer = null;
        try {
			customer = mapper.readValue((String) client.get(login), Customer.class);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
        return customer;
    }

    /**
     * Finds a customer by login and compares it's password to the password
     * provided. If they are a match return a customer, if not return null
     */
    public Customer findCustomer(final String login, final String password) {

        Customer customer = findCustomer(login);

        if (customer == null || !customer.getPassword().equals(password)) {
        	return null;
        }

        return customer;
    }

    /**
     * Returns all of the customers in the database.
     */
    public List<Customer> findAllCustomers() {
    	List<Customer> customers = new ArrayList<Customer>();

    	// Get a connection to the all customers view
    	View view = client.getView("customers", "all");

    	// Create a new View Query
    	Query query = new Query();
    	query.setIncludeDocs(true); // Include the full document as well

    	// Query the Cluster and return the View Response
    	ViewResponse result = client.query(view, query);

    	// Iterate over the results and add the customers to the List
    	Iterator<ViewRow> itr = result.iterator();
    	while(itr.hasNext()) {
    		ViewRow row = itr.next();
    		Customer customer = null;
    		try {
    			customer = mapper.readValue((String) row.getDocument(), Customer.class);
			} catch (Exception e) {
				e.printStackTrace();
			}
    		if (customer != null) {
    			customers.add(customer);
    		}
    	}
    	return customers;
    }

    /**
     * Updates a customer with the appropriate information using the replace function.
     * This will replace a document with the new one based on the key which is login
     * in our case
     */
    public Customer updateCustomer(final Customer customer) {

        // Make sure the object is valid
        if (customer == null)
            throw new ValidationException("Customer object is null");

        try {
			client.replace(customer.getLogin(), EXP_TIME, mapper.writeValueAsString(customer));
		} catch (Exception e) {
			e.printStackTrace();
		}

        return customer;
    }

    /**
     * Removes a customer from the database using the delete function
     */
    public void removeCustomer(final Customer customer) {
        if (customer == null)
            throw new ValidationException("Customer object is null");

        client.delete(customer.getLogin());
    }
}
