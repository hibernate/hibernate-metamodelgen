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
package org.hibernate.jpamodelgen.xml;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;

import org.hibernate.jpamodelgen.Context;
import org.hibernate.jpamodelgen.ImportContextImpl;
import org.hibernate.jpamodelgen.MetaModelGenerationException;
import org.hibernate.jpamodelgen.model.ImportContext;
import org.hibernate.jpamodelgen.model.MetaAttribute;
import org.hibernate.jpamodelgen.model.MetaEntity;
import org.hibernate.jpamodelgen.util.AccessTypeInformation;
import org.hibernate.jpamodelgen.util.StringUtil;
import org.hibernate.jpamodelgen.util.TypeUtils;
import org.hibernate.jpamodelgen.xml.jaxb.Attributes;
import org.hibernate.jpamodelgen.xml.jaxb.Basic;
import org.hibernate.jpamodelgen.xml.jaxb.ElementCollection;
import org.hibernate.jpamodelgen.xml.jaxb.Embeddable;
import org.hibernate.jpamodelgen.xml.jaxb.EmbeddableAttributes;
import org.hibernate.jpamodelgen.xml.jaxb.Embedded;
import org.hibernate.jpamodelgen.xml.jaxb.EmbeddedId;
import org.hibernate.jpamodelgen.xml.jaxb.Entity;
import org.hibernate.jpamodelgen.xml.jaxb.Id;
import org.hibernate.jpamodelgen.xml.jaxb.ManyToMany;
import org.hibernate.jpamodelgen.xml.jaxb.ManyToOne;
import org.hibernate.jpamodelgen.xml.jaxb.MapKeyClass;
import org.hibernate.jpamodelgen.xml.jaxb.MappedSuperclass;
import org.hibernate.jpamodelgen.xml.jaxb.OneToMany;
import org.hibernate.jpamodelgen.xml.jaxb.OneToOne;

/**
 * @author Hardy Ferentschik
 */
public class XmlMetaEntity implements MetaEntity {

	static Map<String, String> COLLECTIONS = new HashMap<String, String>();

	static {
		COLLECTIONS.put( "java.util.Collection", "javax.persistence.metamodel.CollectionAttribute" );
		COLLECTIONS.put( "java.util.Set", "javax.persistence.metamodel.SetAttribute" );
		COLLECTIONS.put( "java.util.List", "javax.persistence.metamodel.ListAttribute" );
		COLLECTIONS.put( "java.util.Map", "javax.persistence.metamodel.MapAttribute" );
	}

	protected final String clazzName;
	protected final String packageName;
	protected final String defaultPackageName;
	protected final ImportContext importContext;
	protected final List<MetaAttribute> members = new ArrayList<MetaAttribute>();
	protected final TypeElement element;
	protected final Context context;
	protected final boolean isMetaComplete;

	private Attributes attributes;
	private EmbeddableAttributes embeddableAttributes;
	protected AccessTypeInformation accessTypeInfo;

	public XmlMetaEntity(Entity ormEntity, String defaultPackageName, TypeElement element, Context context) {
		this( ormEntity.getClazz(), defaultPackageName, element, context, ormEntity.isMetadataComplete() );
		this.attributes = ormEntity.getAttributes();
		this.embeddableAttributes = null;
		// entities can be directly initialised
		init();
	}

	protected XmlMetaEntity(MappedSuperclass mappedSuperclass, String defaultPackageName, TypeElement element, Context context) {
		this(
				mappedSuperclass.getClazz(),
				defaultPackageName,
				element,
				context,
				mappedSuperclass.isMetadataComplete()
		);
		this.attributes = mappedSuperclass.getAttributes();
		this.embeddableAttributes = null;
		// entities can be directly initialised
		init();
	}

	protected XmlMetaEntity(Embeddable embeddable, String defaultPackageName, TypeElement element, Context context) {
		this( embeddable.getClazz(), defaultPackageName, element, context, embeddable.isMetadataComplete() );
		this.attributes = null;
		this.embeddableAttributes = embeddable.getAttributes();
	}

	private XmlMetaEntity(String clazz, String defaultPackageName, TypeElement element, Context context, Boolean metaComplete) {
		this.defaultPackageName = defaultPackageName;
		String className = clazz;
		String pkg = defaultPackageName;
		if ( StringUtil.isFullyQualified( className ) ) {
			// if the class name is fully qualified we have to extract the package name from the fqcn.
			// default package name gets ignored
			pkg = StringUtil.packageNameFromFqcn( className );
			className = StringUtil.classNameFromFqcn( clazz );
		}
		this.clazzName = className;
		this.packageName = pkg;
		this.context = context;
		this.importContext = new ImportContextImpl( getPackageName() );
		this.element = element;
		this.isMetaComplete = initIsMetaComplete( metaComplete );
	}

	protected final void init() {
		this.accessTypeInfo = context.getAccessTypeInfo( getQualifiedName() );
		if ( attributes != null ) {
			parseAttributes( attributes );
		}
		else {
			parseEmbeddableAttributes( embeddableAttributes );
		}
	}

	public String getSimpleName() {
		return clazzName;
	}

	public String getQualifiedName() {
		return packageName + "." + getSimpleName();
	}

	public String getPackageName() {
		return packageName;
	}

	public List<MetaAttribute> getMembers() {
		return members;
	}

	public String generateImports() {
		return importContext.generateImports();
	}

	public String importType(String fqcn) {
		return importContext.importType( fqcn );
	}

	public String staticImport(String fqcn, String member) {
		return importContext.staticImport( fqcn, member );
	}

	public TypeElement getTypeElement() {
		return element;
	}

	@Override
	public boolean isMetaComplete() {
		return isMetaComplete;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append( "XmlMetaEntity" );
		sb.append( "{accessTypeInfo=" ).append( accessTypeInfo );
		sb.append( ", clazzName='" ).append( clazzName ).append( '\'' );
		sb.append( ", members=" ).append( members );
		sb.append( ", isMetaComplete=" ).append( isMetaComplete );
		sb.append( '}' );
		return sb.toString();
	}

	private boolean initIsMetaComplete(Boolean metadataComplete) {
		return context.isPersistenceUnitCompletelyXmlConfigured() || Boolean.TRUE.equals( metadataComplete );
	}

	private String[] getCollectionTypes(String propertyName, String explicitTargetEntity, String explicitMapKeyClass, ElementKind expectedElementKind) {
		for ( Element elem : element.getEnclosedElements() ) {
			if ( !expectedElementKind.equals( elem.getKind() ) ) {
				continue;
			}

			String elementPropertyName = elem.getSimpleName().toString();
			if ( elem.getKind().equals( ElementKind.METHOD ) ) {
				elementPropertyName = StringUtil.getPropertyName( elementPropertyName );
			}

			if ( !propertyName.equals( elementPropertyName ) ) {
				continue;
			}

			DeclaredType type = determineDeclaredType( elem );
			if ( type == null ) {
				continue;
			}

			return determineTypes( propertyName, explicitTargetEntity, explicitMapKeyClass, type );
		}
		return null;
	}

	private DeclaredType determineDeclaredType(Element elem) {
		DeclaredType type = null;
		if ( elem.asType() instanceof DeclaredType ) {
			type = ( (DeclaredType) elem.asType() );
		}
		else if ( elem.asType() instanceof ExecutableType ) {
			ExecutableType executableType = (ExecutableType) elem.asType();
			if ( executableType.getReturnType() instanceof DeclaredType ) {
				type = (DeclaredType) executableType.getReturnType();
			}
		}
		return type;
	}

	private String[] determineTypes(String propertyName, String explicitTargetEntity, String explicitMapKeyClass, DeclaredType type) {
		String[] types = new String[3];
		determineTargetType( type, propertyName, explicitTargetEntity, types );
		determineCollectionType( type, types );
		if ( types[1].equals( "javax.persistence.metamodel.MapAttribute" ) ) {
			determineMapType( type, explicitMapKeyClass, types );
		}
		return types;
	}

	private void determineMapType(DeclaredType type, String explicitMapKeyClass, String[] types) {
		if ( explicitMapKeyClass != null ) {
			types[2] = explicitMapKeyClass;
		}
		else {
			types[2] = TypeUtils.getKeyType( type, context );
		}
	}

	private void determineCollectionType(DeclaredType type, String[] types) {
		types[1] = COLLECTIONS.get( type.asElement().toString() );
	}

	private void determineTargetType(DeclaredType type, String propertyName, String explicitTargetEntity, String[] types) {
		List<? extends TypeMirror> typeArguments = type.getTypeArguments();

		if ( typeArguments.size() == 0 && explicitTargetEntity == null ) {
			throw new MetaModelGenerationException( "Unable to determine target entity type for " + clazzName + "." + propertyName + "." );
		}

		if ( explicitTargetEntity == null ) {
			types[0] = TypeUtils.extractClosestRealTypeAsString( typeArguments.get( 0 ), context );
		}
		else {
			types[0] = explicitTargetEntity;
		}
	}

	/**
	 * Returns the entity type for a property.
	 *
	 * @param propertyName The property name
	 * @param explicitTargetEntity The explicitly specified target entity type or {@code null}.
	 * @param expectedElementKind Determines property vs field access type
	 *
	 * @return The entity type for this property  or {@code null} if the property with the name and the matching access
	 *         type does not exist.
	 */
	private String getType(String propertyName, String explicitTargetEntity, ElementKind expectedElementKind) {
		for ( Element elem : element.getEnclosedElements() ) {
			if ( !expectedElementKind.equals( elem.getKind() ) ) {
				continue;
			}

			TypeMirror mirror;
			String name = elem.getSimpleName().toString();
			if ( ElementKind.METHOD.equals( elem.getKind() ) ) {
				name = StringUtil.getPropertyName( name );
				mirror = ( (ExecutableElement) elem ).getReturnType();
			}
			else {
				mirror = elem.asType();
			}

			if ( name == null || !name.equals( propertyName ) ) {
				continue;
			}

			if ( explicitTargetEntity != null ) {
				// TODO should there be a check of the target entity class and if it is loadable?
				return explicitTargetEntity;
			}

			switch ( mirror.getKind() ) {
				case INT: {
					return "java.lang.Integer";
				}
				case LONG: {
					return "java.lang.Long";
				}
				case BOOLEAN: {
					return "java.lang.Boolean";
				}
				case BYTE: {
					return "java.lang.Byte";
				}
				case SHORT: {
					return "java.lang.Short";
				}
				case CHAR: {
					return "java.lang.Char";
				}
				case FLOAT: {
					return "java.lang.Float";
				}
				case DOUBLE: {
					return "java.lang.Double";
				}
				case DECLARED: {
					return mirror.toString();
				}
				case TYPEVAR: {
					return mirror.toString();
				}
			}
		}

		context.logMessage(
				Diagnostic.Kind.WARNING,
				"Unable to determine type for property " + propertyName + " of class " + getQualifiedName()
						+ " using assess type " + accessTypeInfo.getDefaultAccessType()
		);
		return null;
	}

	private void parseAttributes(Attributes attributes) {
		XmlMetaSingleAttribute attribute;
		for ( Id id : attributes.getId() ) {
			ElementKind elementKind = getElementKind( id.getAccess() );
			String type = getType( id.getName(), null, elementKind );
			if ( type != null ) {
				attribute = new XmlMetaSingleAttribute( this, id.getName(), type );
				members.add( attribute );
			}
		}

		if ( attributes.getEmbeddedId() != null ) {
			EmbeddedId embeddedId = attributes.getEmbeddedId();
			ElementKind elementKind = getElementKind( embeddedId.getAccess() );
			String type = getType( embeddedId.getName(), null, elementKind );
			if ( type != null ) {
				attribute = new XmlMetaSingleAttribute( this, embeddedId.getName(), type );
				members.add( attribute );
			}
		}

		for ( Basic basic : attributes.getBasic() ) {
			parseBasic( basic );
		}

		for ( ManyToOne manyToOne : attributes.getManyToOne() ) {
			parseManyToOne( manyToOne );
		}

		for ( OneToOne oneToOne : attributes.getOneToOne() ) {
			parseOneToOne( oneToOne );
		}

		for ( ManyToMany manyToMany : attributes.getManyToMany() ) {
			if ( parseManyToMany( manyToMany ) ) {
				break;
			}
		}

		for ( OneToMany oneToMany : attributes.getOneToMany() ) {
			if ( parseOneToMany( oneToMany ) ) {
				break;
			}
		}

		for ( ElementCollection collection : attributes.getElementCollection() ) {
			if ( parseElementCollection( collection ) ) {
				break;
			}
		}

		for ( Embedded embedded : attributes.getEmbedded() ) {
			parseEmbedded( embedded );
		}
	}

	private void parseEmbeddableAttributes(EmbeddableAttributes attributes) {
		if ( attributes == null ) {
			return;
		}
		for ( Basic basic : attributes.getBasic() ) {
			parseBasic( basic );
		}

		for ( ManyToOne manyToOne : attributes.getManyToOne() ) {
			parseManyToOne( manyToOne );
		}

		for ( OneToOne oneToOne : attributes.getOneToOne() ) {
			parseOneToOne( oneToOne );
		}

		for ( ManyToMany manyToMany : attributes.getManyToMany() ) {
			if ( parseManyToMany( manyToMany ) ) {
				break;
			}
		}

		for ( OneToMany oneToMany : attributes.getOneToMany() ) {
			if ( parseOneToMany( oneToMany ) ) {
				break;
			}
		}

		for ( ElementCollection collection : attributes.getElementCollection() ) {
			if ( parseElementCollection( collection ) ) {
				break;
			}
		}
	}

	private boolean parseElementCollection(ElementCollection collection) {
		String[] types;
		XmlMetaCollection metaCollection;
		ElementKind elementKind = getElementKind( collection.getAccess() );
		String explicitTargetClass = determineExplicitTargetEntity( collection.getTargetClass() );
		String explicitMapKey = determineExplicitMapKeyClass( collection.getMapKeyClass() );
		try {
			types = getCollectionTypes(
					collection.getName(), explicitTargetClass, explicitMapKey, elementKind
			);
		}
		catch ( MetaModelGenerationException e ) {
			logMetaModelException( collection.getName(), e );
			return true;
		}
		if ( types != null ) {
			if ( types[2] == null ) {
				metaCollection = new XmlMetaCollection( this, collection.getName(), types[0], types[1] );
			}
			else {
				metaCollection = new XmlMetaMap( this, collection.getName(), types[0], types[1], types[2] );
			}
			members.add( metaCollection );
		}
		return false;
	}

	private void parseEmbedded(Embedded embedded) {
		XmlMetaSingleAttribute attribute;
		ElementKind elementKind = getElementKind( embedded.getAccess() );
		String type = getType( embedded.getName(), null, elementKind );
		if ( type != null ) {
			attribute = new XmlMetaSingleAttribute( this, embedded.getName(), type );
			members.add( attribute );
		}
	}

	private String determineExplicitTargetEntity(String targetClass) {
		String explicitTargetClass = targetClass;
		if ( explicitTargetClass != null ) {
			explicitTargetClass = StringUtil.determineFullyQualifiedClassName(
					defaultPackageName, targetClass
			);
		}
		return explicitTargetClass;
	}

	private String determineExplicitMapKeyClass(MapKeyClass mapKeyClass) {
		String explicitMapKey = null;
		if ( mapKeyClass != null ) {
			explicitMapKey = StringUtil.determineFullyQualifiedClassName( defaultPackageName, mapKeyClass.getClazz() );
		}
		return explicitMapKey;
	}

	private boolean parseOneToMany(OneToMany oneToMany) {
		String[] types;
		XmlMetaCollection metaCollection;
		ElementKind elementKind = getElementKind( oneToMany.getAccess() );
		String explicitTargetClass = determineExplicitTargetEntity( oneToMany.getTargetEntity() );
		String explicitMapKey = determineExplicitMapKeyClass( oneToMany.getMapKeyClass() );
		try {
			types = getCollectionTypes( oneToMany.getName(), explicitTargetClass, explicitMapKey, elementKind );
		}
		catch ( MetaModelGenerationException e ) {
			logMetaModelException( oneToMany.getName(), e );
			return true;
		}
		if ( types != null ) {
			if ( types[2] == null ) {
				metaCollection = new XmlMetaCollection( this, oneToMany.getName(), types[0], types[1] );
			}
			else {
				metaCollection = new XmlMetaMap( this, oneToMany.getName(), types[0], types[1], types[2] );
			}
			members.add( metaCollection );
		}
		return false;
	}

	private boolean parseManyToMany(ManyToMany manyToMany) {
		String[] types;
		XmlMetaCollection metaCollection;
		ElementKind elementKind = getElementKind( manyToMany.getAccess() );
		String explicitTargetClass = determineExplicitTargetEntity( manyToMany.getTargetEntity() );
		String explicitMapKey = determineExplicitMapKeyClass( manyToMany.getMapKeyClass() );
		try {
			types = getCollectionTypes(
					manyToMany.getName(), explicitTargetClass, explicitMapKey, elementKind
			);
		}
		catch ( MetaModelGenerationException e ) {
			logMetaModelException( manyToMany.getName(), e );
			return true;
		}
		if ( types != null ) {
			if ( types[2] == null ) {
				metaCollection = new XmlMetaCollection( this, manyToMany.getName(), types[0], types[1] );
			}
			else {
				metaCollection = new XmlMetaMap( this, manyToMany.getName(), types[0], types[1], types[2] );
			}
			members.add( metaCollection );
		}
		return false;
	}

	private void parseOneToOne(OneToOne oneToOne) {
		XmlMetaSingleAttribute attribute;
		ElementKind elementKind = getElementKind( oneToOne.getAccess() );
		String type = getType( oneToOne.getName(), oneToOne.getTargetEntity(), elementKind );
		if ( type != null ) {
			attribute = new XmlMetaSingleAttribute( this, oneToOne.getName(), type );
			members.add( attribute );
		}
	}

	private void parseManyToOne(ManyToOne manyToOne) {
		XmlMetaSingleAttribute attribute;
		ElementKind elementKind = getElementKind( manyToOne.getAccess() );
		String type = getType( manyToOne.getName(), manyToOne.getTargetEntity(), elementKind );
		if ( type != null ) {
			attribute = new XmlMetaSingleAttribute( this, manyToOne.getName(), type );
			members.add( attribute );
		}
	}

	private void parseBasic(Basic basic) {
		XmlMetaSingleAttribute attribute;
		ElementKind elementKind = getElementKind( basic.getAccess() );
		String type = getType( basic.getName(), null, elementKind );
		if ( type != null ) {
			attribute = new XmlMetaSingleAttribute( this, basic.getName(), type );
			members.add( attribute );
		}
	}

	private void logMetaModelException(String name, MetaModelGenerationException e) {
		StringBuilder builder = new StringBuilder();
		builder.append( "Error processing xml for " );
		builder.append( clazzName );
		builder.append( "." );
		builder.append( name );
		builder.append( ". Error message: " );
		builder.append( e.getMessage() );
		context.logMessage(
				Diagnostic.Kind.WARNING,
				builder.toString()
		);
	}

	private ElementKind getElementKind(org.hibernate.jpamodelgen.xml.jaxb.AccessType accessType) {
		// if no explicit access type was specified in xml we use the entity access type
		if ( accessType == null ) {
			return TypeUtils.getElementKindForAccessType( accessTypeInfo.getAccessType() );
		}
		if ( org.hibernate.jpamodelgen.xml.jaxb.AccessType.FIELD.equals( accessType ) ) {
			return ElementKind.FIELD;
		}
		else {
			return ElementKind.METHOD;
		}
	}
}
