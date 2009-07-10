package model;

import java.math.BigDecimal;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

@Entity  
public class Product {

	transient String nonPersistent;
	static String nonPersistent2;
	
	@Id
	long id;

    int test;
	
	String description;
	BigDecimal price;
	
	@ManyToOne
	Shop shop;
	
	@OneToMany
	Set<Item> items;
}
