package model;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity 
public class Shop {
	@Id
	long id;
	String name;
}
 