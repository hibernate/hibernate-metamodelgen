package model;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import java.util.Date;
import java.util.List;
import java.util.Set;

//@Entity
public class Order {
	
	//@Id
	long id;
	
	//@OneToMany
	Set<Item> items;
	
	boolean filled;
	Date date;
	
	//@OneToMany
	List<String> notes;
	
	//@ManyToOne
	Shop shop;
}
