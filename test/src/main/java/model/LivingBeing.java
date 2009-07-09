package model;

import javax.persistence.MappedSuperclass;
import javax.persistence.Access;
import javax.persistence.AccessType;

/**
 * @author Emmanuel Bernard
 */
@MappedSuperclass
@Access(javax.persistence.AccessType.FIELD)
public class LivingBeing {
	boolean isReallyAlive;

	public boolean isReallyAlive() {
		return isReallyAlive;
	}

	public void setReallyAlive(boolean reallyAlive) {
		isReallyAlive = reallyAlive;
	}

	public int nonPersistent() {
		return 0;
	}
}
