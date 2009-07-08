package org.hibernate.jpa.metamodel.ap;

import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import javax.lang.model.element.TypeElement;
import javax.persistence.AccessType;
import javax.annotation.processing.ProcessingEnvironment;
import javax.tools.Diagnostic;

import org.hibernate.jpa.metamodel.ap.annotation.MetaEntity;

/**
 * @author Emmanuel Bernard
 */
public class Context {
	//used to cache access types
	private Map<TypeElement, AccessType> accessTypes = new HashMap<TypeElement,AccessType>();
	private Set<String> elementsAlreadyProcessed = new HashSet<String>();
	private ProcessingEnvironment pe;
	private final Map<String, IMetaEntity> metaEntitiesToProcess = new HashMap<String, IMetaEntity>();
	private final Map<String, IMetaEntity> metaSuperclassAndEmbeddableToProcess = new HashMap<String, IMetaEntity>();

	public Context(ProcessingEnvironment pe) {
		this.pe = pe;
	}

	public Map<String, IMetaEntity> getMetaEntitiesToProcess() {
		return metaEntitiesToProcess;
	}

	public Map<String, IMetaEntity> getMetaSuperclassAndEmbeddableToProcess() {
		return metaSuperclassAndEmbeddableToProcess;
	}

	public void addAccessType(TypeElement element, AccessType accessType) {
		accessTypes.put( element, accessType );
	}

	public Map<TypeElement, AccessType> getAccessTypes() {
		return accessTypes;
	}

	public Set<String> getElementsAlreadyProcessed() {
		return elementsAlreadyProcessed;
	}

	//only process Embeddable or Superclass
	//does not work for Entity (risk of circularity)
	public void processElement(TypeElement element, AccessType defaultAccessTypeForHierarchy) {
		if ( elementsAlreadyProcessed.contains( element.getQualifiedName().toString() ) ) {
			pe.getMessager().printMessage( Diagnostic.Kind.WARNING, "Element already processed (ignoring): " + element );
			return;
		}

		ClassWriter.writeFile( new MetaEntity( pe, element, this, defaultAccessTypeForHierarchy ), pe, this );
		elementsAlreadyProcessed.add( element.getQualifiedName().toString() );
	}
}
