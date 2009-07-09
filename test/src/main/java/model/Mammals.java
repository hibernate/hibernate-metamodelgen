package model;

import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Mammals extends LivingBeing {
	private String id;
	private String nbrOfMammals;

	@Id
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getNbrOfMammals() {
		return nbrOfMammals;
	}

	public void setNbrOfMammals(String nbrOfMammals) {
		this.nbrOfMammals = nbrOfMammals;
	}
}
