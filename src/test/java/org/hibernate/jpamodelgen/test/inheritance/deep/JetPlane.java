package org.hibernate.jpamodelgen.test.inheritance.deep;

import javax.persistence.Basic;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * A {@link Plane} subclass entity that defines extra attributes
 * 
 * @author Igor Vaynberg
 */
@Entity
@DiscriminatorValue("JetPlane")
public class JetPlane extends Plane {
	@Basic(optional = false)
	private Integer jets;

}
