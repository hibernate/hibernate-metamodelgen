package model;

import javax.persistence.Id;
import javax.persistence.Entity;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class House extends Building {
	@Id
	private Long id;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}
}
