package model.xmlmapped;

import model.Address;
import model.Area;

/**
 * @author Emmanuel Bernard
 */
public class Building extends Area {
	private Address address;

	public Address getAddress() {
		return address;
	}

	public void setAddress(Address address) {
		this.address = address;
	}
}