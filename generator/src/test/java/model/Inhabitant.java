package model;

import java.util.Set;
import javax.persistence.Embeddable;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.ElementCollection;

/**
 * @author Emmanuel Bernard
 */
@Embeddable
@Access(javax.persistence.AccessType.FIELD)
public class Inhabitant {
	private String name;
	@ElementCollection
	private Set<Pet> pets;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
