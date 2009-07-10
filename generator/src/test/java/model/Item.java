package model;

import java.util.Map;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

@Entity  
public class Item {
	
	long _id;
	
	int _quantity;
	
	Product _product;
	
	Order _order;

	Detail detail;

	@Id
	public long getId() {
		return _id;
	}

	public void setId(long id) {
		this._id = id;
	}

	public int getQuantity() {
		return _quantity;
	}

	public void setQuantity(int quantity) {
		this._quantity = quantity;
	}

	@ManyToOne
	public Product getProduct() {
		return _product;
	}

	public void setProduct(Product product) {
		this._product = product;
	}

	@ManyToOne
	public Order getOrder() {
		return _order;
	}

	public void setOrder(Order order) {
		this._order = order;
	}
	
	@OneToMany
	public Map<String, Order> getNamedOrders() {
		return null;
	}

	public Detail getDetail() {
		return detail;
	}

	public void setDetail(Detail detail) {
		this.detail = detail;
	}
}
