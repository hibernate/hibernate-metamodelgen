/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

import org.hibernate.jpamodelgen.model.MetaEntity;
import org.hibernate.jpamodelgen.util.AccessType;
import org.hibernate.jpamodelgen.util.AccessTypeInformation;
import org.hibernate.jpamodelgen.util.Constants;

/**
 * @author Max Andersen
 * @author Hardy Ferentschik
 * @author Emmanuel Bernard
 */
public final class Context {
	private static final String DEFAULT_PERSISTENCE_XML_LOCATION = "/META-INF/persistence.xml";

	/**
	 * Used for keeping track of parsed entities and mapped super classes (xml + annotations).
	 */
	private final Map<String, MetaEntity> metaEntities = new HashMap<String, MetaEntity>();

	/**
	 * Used for keeping track of parsed embeddable entities. These entities have to be kept separate since
	 * they are lazily initialized.
	 */
	private final Map<String, MetaEntity> metaEmbeddables = new HashMap<String, MetaEntity>();

	private final Map<String, AccessTypeInformation> accessTypeInformation = new HashMap<String, AccessTypeInformation>();

	private final ProcessingEnvironment pe;
	private final boolean logDebug;
	private final boolean lazyXmlParsing;
	private final String persistenceXmlLocation;
	private final List<String> ormXmlFiles;

	/**
	 * Keeping track which (if any) of the mapping files is xml-mapping-metadata-complete. If all are metadata complete
	 * no annotation processing will take place.
	 */
	private List<Boolean> fullyXmlConfigured;
	private boolean addGeneratedAnnotation = true;
	private boolean addGenerationDate;
	private boolean addSuppressWarningsAnnotation;
	private AccessType persistenceUnitDefaultAccessType;

	public Context(ProcessingEnvironment pe) {
		this.pe = pe;
		this.fullyXmlConfigured = new ArrayList<Boolean>();

		if ( pe.getOptions().get( JPAMetaModelEntityProcessor.PERSISTENCE_XML_OPTION ) != null ) {
			String tmp = pe.getOptions().get( JPAMetaModelEntityProcessor.PERSISTENCE_XML_OPTION );
			if ( !tmp.startsWith( Constants.PATH_SEPARATOR ) ) {
				tmp = Constants.PATH_SEPARATOR + tmp;
			}
			persistenceXmlLocation = tmp;
		}
		else {
			persistenceXmlLocation = DEFAULT_PERSISTENCE_XML_LOCATION;
		}

		if ( pe.getOptions().get( JPAMetaModelEntityProcessor.ORM_XML_OPTION ) != null ) {
			String tmp = pe.getOptions().get( JPAMetaModelEntityProcessor.ORM_XML_OPTION );
			ormXmlFiles = new ArrayList<String>();
			for ( String ormFile : tmp.split( "," ) ) {
				if ( !ormFile.startsWith( Constants.PATH_SEPARATOR ) ) {
					ormFile = Constants.PATH_SEPARATOR + ormFile;
				}
				ormXmlFiles.add( ormFile );
			}
		}
		else {
			ormXmlFiles = Collections.emptyList();
		}

		lazyXmlParsing = Boolean.parseBoolean( pe.getOptions().get( JPAMetaModelEntityProcessor.LAZY_XML_PARSING ) );
		logDebug = Boolean.parseBoolean( pe.getOptions().get( JPAMetaModelEntityProcessor.DEBUG_OPTION ) );
	}

	public ProcessingEnvironment getProcessingEnvironment() {
		return pe;
	}

	public boolean addGeneratedAnnotation() {
		return addGeneratedAnnotation;
	}

	public void setAddGeneratedAnnotation(boolean addGeneratedAnnotation) {
		this.addGeneratedAnnotation = addGeneratedAnnotation;
	}

	public boolean addGeneratedDate() {
		return addGenerationDate;
	}

	public void setAddGenerationDate(boolean addGenerationDate) {
		this.addGenerationDate = addGenerationDate;
	}

	public boolean isAddSuppressWarningsAnnotation() {
		return addSuppressWarningsAnnotation;
	}

	public void setAddSuppressWarningsAnnotation(boolean addSuppressWarningsAnnotation) {
		this.addSuppressWarningsAnnotation = addSuppressWarningsAnnotation;
	}

	public Elements getElementUtils() {
		return pe.getElementUtils();
	}

	public Types getTypeUtils() {
		return pe.getTypeUtils();
	}

	public String getPersistenceXmlLocation() {
		return persistenceXmlLocation;
	}

	public List<String> getOrmXmlFiles() {
		return ormXmlFiles;
	}

	public boolean containsMetaEntity(String fqcn) {
		return metaEntities.containsKey( fqcn );
	}

	public MetaEntity getMetaEntity(String fqcn) {
		return metaEntities.get( fqcn );
	}

	public Collection<MetaEntity> getMetaEntities() {
		return metaEntities.values();
	}

	public void addMetaEntity(String fqcn, MetaEntity metaEntity) {
		metaEntities.put( fqcn, metaEntity );
	}

	public boolean containsMetaEmbeddable(String fqcn) {
		return metaEmbeddables.containsKey( fqcn );
	}

	public MetaEntity getMetaEmbeddable(String fqcn) {
		return metaEmbeddables.get( fqcn );
	}

	public void addMetaEmbeddable(String fqcn, MetaEntity metaEntity) {
		metaEmbeddables.put( fqcn, metaEntity );
	}

	public Collection<MetaEntity> getMetaEmbeddables() {
		return metaEmbeddables.values();
	}

	public void addAccessTypeInformation(String fqcn, AccessTypeInformation info) {
		accessTypeInformation.put( fqcn, info );
	}

	public AccessTypeInformation getAccessTypeInfo(String fqcn) {
		return accessTypeInformation.get( fqcn );
	}

	public TypeElement getTypeElementForFullyQualifiedName(String fqcn) {
		Elements elementUtils = pe.getElementUtils();
		return elementUtils.getTypeElement( fqcn );
	}

	public void logMessage(Diagnostic.Kind type, String message) {
		if ( !logDebug && type.equals( Diagnostic.Kind.OTHER ) ) {
			return;
		}
		pe.getMessager().printMessage( type, message );
	}

	public boolean isFullyXmlConfigured() {
		if ( fullyXmlConfigured.isEmpty() ) {
			return false;
		}
		else {
			return !fullyXmlConfigured.contains( Boolean.FALSE );
		}
	}

	public void mappingDocumentFullyXmlConfigured(boolean fullyXmlConfigured) {
		this.fullyXmlConfigured.add( fullyXmlConfigured );
	}

	public AccessType getPersistenceUnitDefaultAccessType() {
		return persistenceUnitDefaultAccessType;
	}

	public void setPersistenceUnitDefaultAccessType(AccessType persistenceUnitDefaultAccessType) {
		this.persistenceUnitDefaultAccessType = persistenceUnitDefaultAccessType;
	}

	public boolean doLazyXmlParsing() {
		return lazyXmlParsing;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append( "Context" );
		sb.append( "{accessTypeInformation=" ).append( accessTypeInformation );
		sb.append( ", logDebug=" ).append( logDebug );
		sb.append( ", lazyXmlParsing=" ).append( lazyXmlParsing );
		sb.append( ", isPersistenceUnitCompletelyXmlConfigured=" ).append( fullyXmlConfigured );
		sb.append( ", ormXmlFiles=" ).append( ormXmlFiles );
		sb.append( ", persistenceXmlLocation='" ).append( persistenceXmlLocation ).append( '\'' );
		sb.append( '}' );
		return sb.toString();
	}
}
