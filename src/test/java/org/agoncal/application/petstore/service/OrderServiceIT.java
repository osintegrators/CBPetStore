package org.agoncal.application.petstore.service;

import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.agoncal.application.petstore.domain.Address;
import org.agoncal.application.petstore.domain.CartItem;
import org.agoncal.application.petstore.domain.Category;
import org.agoncal.application.petstore.domain.CreditCard;
import org.agoncal.application.petstore.domain.CreditCardType;
import org.agoncal.application.petstore.domain.Customer;
import org.agoncal.application.petstore.domain.Item;
import org.agoncal.application.petstore.domain.Order;
import org.agoncal.application.petstore.domain.Product;
import org.agoncal.application.petstore.exception.ValidationException;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Antonio Goncalves
 *         http://www.antoniogoncalves.org
 */
@RunWith(Arquillian.class)
public class OrderServiceIT extends AbstractServiceIT {

    // ======================================
    // =             Attributes             =
    // ======================================

    @Inject
    private OrderService orderService;
    @Inject
    private CustomerService customerService;

    // ======================================
    // =              Unit tests            =
    // ======================================

    @Test
    @Ignore("TODO Not finished")
    public void shouldCRUDanOrder() {

        // Finds all the objects
        // int initialNumber = orderService.findAllOrders().size();

        // Creates an object
        Address address = new Address("78 Gnu Rd", "Texas", "666", "WWW");
        Customer customer = new Customer("Richard", "Stallman", "rich", "rich", "rich@gnu.org", address);
        CreditCard creditCard = new CreditCard("1234", CreditCardType.MASTER_CARD, "10/12");
        List<CartItem> cartItems = new ArrayList<CartItem>();

        Category reptile = new Category("Reptiles", "Any of various cold-blooded, usually egg-laying vertebrates, such as a snake, lizard, crocodile, turtle");
        Product rattlesnake = new Product("Rattlesnake", "Doubles as a watch dog", reptile);
        Item femaleRattlesnake = new Item("Female Adult", 20.00f, "reptile1.jpg", rattlesnake, "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Praesent lobortis ante et nunc scelerisque aliquet. Phasellus sed auctor purus. Cras tempus lacus eget felis viverra scelerisque. Sed ac tellus vitae nisl vehicula feugiat ac vitae dolor. Duis interdum lorem quis risus ullamcorper id cursus magna pharetra. Sed et nisi odio.");
        CartItem cartItem = new CartItem(femaleRattlesnake, null);

        cartItems.add(cartItem);

        // Persists the object
        //customer = customerService.createCustomer(customer);
        Order order = orderService.createOrder(customer, creditCard, cartItems);
        String id = order.getId();
        
        assertNotNull(order.getId());

        // Finds all the objects and checks there's an extra one
        // assertEquals("Should have an extra object", initialNumber + 1, orderService.findAllOrders().size());

        // Finds the object by id
        //order = orderService.findOrder(id);
        //assertNotNull(order.getOrderDate());

        // Deletes the object
        //orderService.removeOrder(order);

        // Checks the object has been deleted
        //assertNull("Should has been deleted", orderService.findOrder(id));

        // Finds all the objects and checks there's one less
        //assertEquals("Should have an extra object", initialNumber, orderService.findAllOrders().size());
    }

    @Test(expected = ValidationException.class)
    public void shouldNotCreateAnOrderWithAnEmptyCart() {

        // Creates an object
        Address address = new Address("78 Gnu Rd", "Texas", "666", "WWW");
        Customer customer = new Customer("Richard", "Stallman", "rich", "rich", "rich@gnu.org", address);
        CreditCard creditCard = new CreditCard("1234", CreditCardType.MASTER_CARD, "10/12");
        List<CartItem> cartItems = new ArrayList<CartItem>();

        // Persists the object
        orderService.createOrder(customer, creditCard, cartItems);
    }
}
