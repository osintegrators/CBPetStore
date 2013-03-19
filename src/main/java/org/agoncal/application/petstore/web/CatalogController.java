package org.agoncal.application.petstore.web;

import org.agoncal.application.petstore.domain.Item;
import org.agoncal.application.petstore.domain.Product;
import org.agoncal.application.petstore.service.CatalogService;
import org.agoncal.application.petstore.util.Loggable;

import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.Serializable;
import java.util.List;

/**
 * @author Antonio Goncalves
 *         http://www.antoniogoncalves.org
 *         --
 */

@Named
//@RequestScoped TODO should be request scoped
@SessionScoped
@Loggable
@CatchException
public class CatalogController extends Controller implements Serializable {

    // ======================================
    // =             Attributes             =
    // ======================================

    @Inject
    private CatalogService catalogService;

    private String categoryName;
    private String productId;
    private String itemId;

    private String keyword;
    private Product product;
    private Item item;
    private List<Product> products;
    private List<Item> items;

    // ======================================
    // =              Public Methods        =
    // ======================================

    public String doFindProducts() {
   		products = catalogService.findProducts(categoryName);
        return "showproducts.faces";
    }

    public String doFindItems() {
        product = catalogService.findProduct(String.valueOf(productId));
        items = catalogService.findItems(productId);
        return "showitems.faces";
    }

    public String doFindItem() {
        item = catalogService.findItem(itemId);
        return "showitem.faces";
    }

    public String doSearch() {
        items = catalogService.searchItems(keyword);
        return "searchresult.faces?keyword=" + keyword + "&faces-redirect=true";
    }

    // ======================================
    // =         Getters & setters          =
    // ======================================

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public List<Product> getProducts() {
        return products;
    }

    public List<Item> getItems() {
        return items;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }
}