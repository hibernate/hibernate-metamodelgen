package org.hibernate.jpamodelgen.test.inheritance.deep;

import java.util.Date;

import javax.persistence.MappedSuperclass;
import javax.persistence.PrePersist;

/**
 * A mapped super class that does not define an id attribute.
 * 
 * @author Igor Vaynberg
 * 
 */
@MappedSuperclass
public abstract class PersistenceBase {

	Date created;

	@PrePersist
	void prePersist() {
		created = new Date();
	}

}
