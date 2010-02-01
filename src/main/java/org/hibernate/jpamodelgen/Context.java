// $Id$
/*
* JBoss, Home of Professional Open Source
* Copyright 2008, Red Hat Middleware LLC, and individual contributors
* by the @authors tag. See the copyright.txt in the distribution for a
* full listing of individual contributors.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
* http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.hibernate.jpamodelgen;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.persistence.AccessType;
import javax.tools.Diagnostic;

import org.hibernate.jpamodelgen.annotation.AnnotationMetaEntity;
import org.hibernate.jpamodelgen.util.TypeUtils;

/**
 * @author Max Andersen
 * @author Hardy Ferentschik
 * @author Emmanuel Bernard
 */
public class Context {
	private static final String PATH_SEPARATOR = System.getProperty( "file.separator" );
	private static final String DEFAULT_PERSISTENCE_XML_LOCATION = "/META-INF/persistence.xml";
	private final Map<String, MetaEntity> metaEntities = new HashMap<String, MetaEntity>();
	private final Map<String, MetaEntity> metaSuperclassAndEmbeddable = new HashMap<String, MetaEntity>();

	private final ProcessingEnvironment pe;
	private final boolean logDebug;
	private final String persistenceXmlLocation;


	//used to cache access types
	private Map<TypeElement, AccessTypeHolder> accessTypes = new HashMap<TypeElement, AccessTypeHolder>();
	private Set<String> elementsAlreadyProcessed = new HashSet<String>();

	private boolean isPersistenceUnitCompletelyXmlConfigured;

	private static class AccessTypeHolder {
		public AccessType elementAccessType;
		public AccessType hierarchyAccessType;
	}

	public Context(ProcessingEnvironment pe) {
		this.pe = pe;

		if ( pe.getOptions().get( JPAMetaModelEntityProcessor.PERSISTENCE_XML_OPTION ) != null ) {
			String tmp = pe.getOptions().get( JPAMetaModelEntityProcessor.PERSISTENCE_XML_OPTION );
			if ( !tmp.startsWith( PATH_SEPARATOR ) ) {
				tmp = PATH_SEPARATOR + tmp;
			}
			persistenceXmlLocation = tmp;
		}
		else {
			persistenceXmlLocation = DEFAULT_PERSISTENCE_XML_LOCATION;
		}

		logDebug = Boolean.parseBoolean( pe.getOptions().get( JPAMetaModelEntityProcessor.DEBUG_OPTION ) );

	}

	public ProcessingEnvironment getProcessingEnvironment() {
		return pe;
	}

	public String getPersistenceXmlLocation() {
		return persistenceXmlLocation;
	}

	public Map<String, MetaEntity> getMetaEntities() {
		return metaEntities;
	}

	public Map<String, MetaEntity> getMetaSuperclassAndEmbeddable() {
		return metaSuperclassAndEmbeddable;
	}

	public void addAccessType(TypeElement element, AccessType accessType) {
		AccessTypeHolder typeHolder = accessTypes.get( element );
		if ( typeHolder == null ) {
			typeHolder = new AccessTypeHolder();
			accessTypes.put( element, typeHolder );
		}
		typeHolder.elementAccessType = accessType;
	}

	public void addAccessTypeForHierarchy(TypeElement element, AccessType accessType) {
		AccessTypeHolder typeHolder = accessTypes.get( element );
		if ( typeHolder == null ) {
			typeHolder = new AccessTypeHolder();
			accessTypes.put( element, typeHolder );
		}
		typeHolder.hierarchyAccessType = accessType;
	}

	public AccessType getAccessType(TypeElement element) {
		final AccessTypeHolder typeHolder = accessTypes.get( element );
		return typeHolder != null ? typeHolder.elementAccessType : null;
	}

	public AccessType getDefaultAccessTypeForHierarchy(TypeElement element) {
		final AccessTypeHolder typeHolder = accessTypes.get( element );
		return typeHolder != null ? typeHolder.hierarchyAccessType : null;
	}

	public Set<String> getElementsAlreadyProcessed() {
		return elementsAlreadyProcessed;
	}

	//only process Embeddable or Superclass
	//does not work for Entity (risk of circularity)

	public void processElement(TypeElement element, AccessType defaultAccessTypeForHierarchy) {
		if ( elementsAlreadyProcessed.contains( element.getQualifiedName().toString() ) ) {
			logMessage( Diagnostic.Kind.OTHER, "Element already processed (ignoring): " + element );
			return;
		}
		ClassWriter.writeFile( new AnnotationMetaEntity( element, this, defaultAccessTypeForHierarchy ), this );
		TypeUtils.extractClosestRealTypeAsString( element.asType(), this );
		elementsAlreadyProcessed.add( element.getQualifiedName().toString() );
	}

	public void logMessage(Diagnostic.Kind type, String message) {
		if ( !logDebug && type.equals( Diagnostic.Kind.OTHER ) ) {
			return;
		}
		pe.getMessager().printMessage( type, message );
	}

	public boolean isPersistenceUnitCompletelyXmlConfigured() {
		return isPersistenceUnitCompletelyXmlConfigured;
	}

	public void setPersistenceUnitCompletelyXmlConfigured(boolean persistenceUnitCompletelyXmlConfigured) {
		isPersistenceUnitCompletelyXmlConfigured = persistenceUnitCompletelyXmlConfigured;
	}
}
