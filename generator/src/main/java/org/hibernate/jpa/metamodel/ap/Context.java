package org.hibernate.jpa.metamodel.ap;

import java.util.Map;
import java.util.HashMap;
import javax.lang.model.element.TypeElement;
import javax.persistence.AccessType;

/**
 * @author Emmanuel Bernard
 */
public class Context {
	//used to cache access types
	private Map<TypeElement, AccessType> accessTypes = new HashMap<TypeElement,AccessType>();

	public void addAccessType(TypeElement element, AccessType accessType) {
		accessTypes.put( element, accessType );
	}

	public Map<TypeElement, AccessType> getAccessTypes() {
		return accessTypes;
	}
}
