package model.xmlmapped;


public class Mammal extends LivingBeing {
	private String id;
	private String subclass;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getSubclass() {
		return subclass;
	}

	public void setSubclass(String subclass) {
		this.subclass = subclass;
	}
}