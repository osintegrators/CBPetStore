package org.agoncal.application.petstore.service;

import org.agoncal.application.petstore.domain.Category;
import org.agoncal.application.petstore.domain.Customer;
import org.agoncal.application.petstore.exception.ValidationException;
import org.agoncal.application.petstore.util.Loggable;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

import com.couchbase.client.CouchbaseClient;
import com.couchbase.client.protocol.views.Query;
import com.couchbase.client.protocol.views.View;
import com.couchbase.client.protocol.views.ViewResponse;
import com.couchbase.client.protocol.views.ViewRow;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Antonio Goncalves
 *         http://www.antoniogoncalves.org
 *         --
 */

@Stateless
@Loggable
public class CustomerService implements Serializable {

    // ======================================
    // =             Attributes             =
    // ======================================

    //@Inject
    //private EntityManager em;

    public static CouchbaseClient client = null;
    public static ObjectMapper mapper = null;
    public static final int EXP_TIME = 0;

    // ======================================
    // =              Public Methods        =
    // ======================================

    public CustomerService() {
    	client = DBPopulator.getClient();
    	mapper = DBPopulator.getMapper();
    }

    public boolean doesLoginAlreadyExist(final String login) {

        if (login == null)
            throw new ValidationException("Login cannot be null");

        // Login has to be unique
        //TypedQuery<Customer> typedQuery = em.createNamedQuery(Customer.FIND_BY_LOGIN, Customer.class);
        //typedQuery.setParameter("login", login);
        //try {
        //    typedQuery.getSingleResult();
        //    return true;
        //} catch (NoResultException e) {
        //    return false;
        //}
        Customer customer = findCustomer(login);
        if (customer == null) {
        	return false;
        }
        return true;
    }

    public Customer createCustomer(final Customer customer) {

        if (customer == null)
            throw new ValidationException("Customer object is null");

        //em.persist(customer);

        customer.setId(customer.getType() + "_" + customer.getLogin());
        try {
			client.set(customer.getLogin(), EXP_TIME, mapper.writeValueAsString(customer));
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

        return customer;
    }

    public Customer findCustomer(final String login) {

        if (login == null)
            throw new ValidationException("Invalid login");

        //TypedQuery<Customer> typedQuery = em.createNamedQuery(Customer.FIND_BY_LOGIN, Customer.class);
        //typedQuery.setParameter("login", login);

        //try {
        //    return typedQuery.getSingleResult();
        //} catch (NoResultException e) {
        //    return null;
        //}
        Customer customer = null;
        try {
			customer = mapper.readValue((String) client.get(login), Customer.class);
		} catch (JsonParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonMappingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        return customer;
    }

    public Customer findCustomer(final String login, final String password) {

        /*if (login == null)
            throw new ValidationException("Invalid login");
        if (password == null)
            throw new ValidationException("Invalid password");*/
        
        //TypedQuery<Customer> typedQuery = em.createNamedQuery(Customer.FIND_BY_LOGIN_PASSWORD, Customer.class);
        //typedQuery.setParameter("login", login);
        //typedQuery.setParameter("password", password);

        Customer customer = null;
        String json = (String) client.get(login);
        try {
			customer = mapper.readValue(json, Customer.class);
        } catch (Exception ex) {
        	ex.printStackTrace();
        }

        if (customer != null && !customer.getPassword().equals(password)) {
        	return null;
        }

        return customer;
        //return typedQuery.getSingleResult();
    }

    public List<Customer> findAllCustomers() {
    	List<Customer> customers = new ArrayList<Customer>();

    	View view = client.getView("customers", "all");

    	// Create a new View Query
    	Query query = new Query();
    	query.setIncludeDocs(true); // Include the full document as well

    	// Query the Cluster and return the View Response
    	ViewResponse result = client.query(view, query);

    	// Iterate over the results and print out some info
    	Iterator<ViewRow> itr = result.iterator();

    	while(itr.hasNext()) {
    	  ViewRow row = itr.next();

    	  Customer customer = null;
    	  try {
			customer = mapper.readValue((String) row.getDocument(), Customer.class);
			} catch (JsonParseException e) {
				e.printStackTrace();
			} catch (JsonMappingException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
    	  if (customer != null) {
    		  customers.add(customer);
    	  }
    	}
    	return customers;
    }

    public Customer updateCustomer(final Customer customer) {

        // Make sure the object is valid
        if (customer == null)
            throw new ValidationException("Customer object is null");

        // Update the object in the database
        //em.merge(customer);
        try {
			client.replace(customer.getLogin(), EXP_TIME, mapper.writeValueAsString(customer));
		} catch (JsonGenerationException e) {
			e.printStackTrace();
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

        return customer;
    }

    public void removeCustomer(final Customer customer) {
        if (customer == null)
            throw new ValidationException("Customer object is null");

        client.delete(customer.getLogin());
    }
}
