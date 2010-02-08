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
package org.hibernate.jpamodelgen.annotation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.SimpleTypeVisitor6;
import javax.persistence.AccessType;
import javax.persistence.ElementCollection;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.MapKeyClass;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Transient;
import javax.tools.Diagnostic;

import org.hibernate.jpamodelgen.AccessTypeInformation;
import org.hibernate.jpamodelgen.Context;
import org.hibernate.jpamodelgen.ImportContextImpl;
import org.hibernate.jpamodelgen.MetaModelGenerationException;
import org.hibernate.jpamodelgen.model.ImportContext;
import org.hibernate.jpamodelgen.model.MetaAttribute;
import org.hibernate.jpamodelgen.model.MetaEntity;
import org.hibernate.jpamodelgen.util.StringUtil;
import org.hibernate.jpamodelgen.util.TypeUtils;

/**
 * @author Max Andersen
 * @author Hardy Ferentschik
 * @author Emmanuel Bernard
 */
public class AnnotationMetaEntity implements MetaEntity {

	static Map<String, String> COLLECTIONS = new HashMap<String, String>();

	static {
		COLLECTIONS.put( "java.util.Collection", "javax.persistence.metamodel.CollectionAttribute" );
		COLLECTIONS.put( "java.util.Set", "javax.persistence.metamodel.SetAttribute" );
		COLLECTIONS.put( "java.util.List", "javax.persistence.metamodel.ListAttribute" );
		COLLECTIONS.put( "java.util.Map", "javax.persistence.metamodel.MapAttribute" );
	}

	private final TypeElement element;
	private final ImportContext importContext;
	private Context context;
	private List<MetaAttribute> members;
	private AccessTypeInformation entityAccessTypeInfo;

	public AnnotationMetaEntity(TypeElement element, Context context) {
		this.element = element;
		this.context = context;
		importContext = new ImportContextImpl( getPackageName() );
		init();
	}

	public Context getContext() {
		return context;
	}

	public String getSimpleName() {
		return element.getSimpleName().toString();
	}

	public String getQualifiedName() {
		return element.getQualifiedName().toString();
	}

	public String getPackageName() {
		PackageElement packageOf = context.getProcessingEnvironment().getElementUtils().getPackageOf( element );
		return context.getProcessingEnvironment().getElementUtils().getName( packageOf.getQualifiedName() ).toString();
	}

	public List<MetaAttribute> getMembers() {
		return members;
	}

	@Override
	public boolean isMetaComplete() {
		return false;
	}

	private void addPersistentMembers(List<? extends Element> membersOfClass, AccessType membersKind) {
		for ( Element memberOfClass : membersOfClass ) {
			AccessType forcedAccessType = TypeUtils.determineAnnotationSpecifiedAccessType( memberOfClass );
			if ( entityAccessTypeInfo.getAccessType() != membersKind && forcedAccessType == null ) {
				continue;
			}

			if ( TypeUtils.containsAnnotation( memberOfClass, Transient.class )
					|| memberOfClass.getModifiers().contains( Modifier.TRANSIENT )
					|| memberOfClass.getModifiers().contains( Modifier.STATIC ) ) {
				continue;
			}

			TypeVisitor visitor = new TypeVisitor( this );
			AnnotationMetaAttribute result = memberOfClass.asType().accept( visitor, memberOfClass );
			if ( result != null ) {
				members.add( result );
			}
		}
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append( "AnnotationMetaEntity" );
		sb.append( "{element=" ).append( element );
		sb.append( ", members=" ).append( members );
		sb.append( '}' );
		return sb.toString();
	}

	private void init() {
		members = new ArrayList<MetaAttribute>();
		TypeUtils.determineAccessTypeForHierarchy( element, context );
		entityAccessTypeInfo = context.getAccessTypeInfo( getQualifiedName() );

		List<? extends Element> fieldsOfClass = ElementFilter.fieldsIn( element.getEnclosedElements() );
		addPersistentMembers( fieldsOfClass, AccessType.FIELD );

		List<? extends Element> methodsOfClass = ElementFilter.methodsIn( element.getEnclosedElements() );
		addPersistentMembers( methodsOfClass, AccessType.PROPERTY );
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

	public String importType(Name qualifiedName) {
		return importType( qualifiedName.toString() );
	}

	public TypeElement getTypeElement() {
		return element;
	}

	class TypeVisitor extends SimpleTypeVisitor6<AnnotationMetaAttribute, Element> {
		AnnotationMetaEntity parent;

		TypeVisitor(AnnotationMetaEntity parent) {
			this.parent = parent;
		}

		@Override
		protected AnnotationMetaAttribute defaultAction(TypeMirror e, Element p) {
			return super.defaultAction( e, p );
		}

		@Override
		public AnnotationMetaAttribute visitPrimitive(PrimitiveType t, Element element) {
			return new AnnotationMetaSingleAttribute( parent, element, TypeUtils.toTypeString( t ) );
		}

		@Override
		public AnnotationMetaAttribute visitArray(ArrayType t, Element element) {
			return new AnnotationMetaSingleAttribute( parent, element, TypeUtils.toTypeString( t ) );
		}

		@Override
		public AnnotationMetaAttribute visitDeclared(DeclaredType declaredType, Element element) {
			TypeElement returnedElement = ( TypeElement ) context.getProcessingEnvironment()
					.getTypeUtils()
					.asElement( declaredType );
			// WARNING: .toString() is necessary here since Name equals does not compare to String
			String fqNameOfReturnType = returnedElement.getQualifiedName().toString();
			String collection = COLLECTIONS.get( fqNameOfReturnType );
			String targetEntity = getTargetEntity( element.getAnnotationMirrors() );
			if ( collection != null ) {
				if ( TypeUtils.containsAnnotation( element, ElementCollection.class ) ) {
					String explicitTargetEntity = getTargetEntity( element.getAnnotationMirrors() );
					TypeMirror collectionElementType = getCollectionElementType(
							declaredType, fqNameOfReturnType, explicitTargetEntity
					);
					final TypeElement collectionElement = ( TypeElement ) context.getProcessingEnvironment()
							.getTypeUtils()
							.asElement( collectionElementType );
					AccessTypeInformation accessTypeInfo = context.getAccessTypeInfo( collectionElement.getQualifiedName().toString() );
					if ( accessTypeInfo == null ) {
						AccessType explicitAccessType = TypeUtils.determineAnnotationSpecifiedAccessType(
								collectionElement
						);
						accessTypeInfo = new AccessTypeInformation(
								collectionElement.getQualifiedName().toString(),
								explicitAccessType,
								entityAccessTypeInfo.getAccessType()
						);
						context.addAccessTypeInformation(
								collectionElement.getQualifiedName().toString(), accessTypeInfo
						);
					}
					else {
						accessTypeInfo.setDefaultAccessType( entityAccessTypeInfo.getAccessType() );
					}
					AnnotationMetaEntity metaEntity = new AnnotationMetaEntity( collectionElement, context );
					context.addMetaSuperclassOrEmbeddable( metaEntity.getQualifiedName(), metaEntity );
				}
				if ( collection.equals( "javax.persistence.metamodel.MapAttribute" ) ) {
					return createAnnotationMetaAttributeForMap( declaredType, element, collection, targetEntity );
				}
				else {
					return new AnnotationMetaCollection(
							parent, element, collection, getElementType( declaredType, targetEntity )
					);
				}
			}
			else {
				return new AnnotationMetaSingleAttribute(
						parent, element, returnedElement.getQualifiedName().toString()
				);
			}
		}

		@Override
		public AnnotationMetaAttribute visitExecutable(ExecutableType t, Element p) {
			if ( !p.getKind().equals( ElementKind.METHOD ) ) {
				return null;
			}

			String string = p.getSimpleName().toString();
			if ( StringUtil.isPropertyName( string ) ) {
				TypeMirror returnType = t.getReturnType();
				return returnType.accept( this, p );
			}
			else {
				return null;
			}
		}

		private AnnotationMetaAttribute createAnnotationMetaAttributeForMap(DeclaredType declaredType, Element element, String collection, String targetEntity) {
			String keyType;
			if ( TypeUtils.containsAnnotation( element, MapKeyClass.class ) ) {
				TypeMirror typeMirror = ( TypeMirror ) TypeUtils.getAnnotationValue(
						TypeUtils.getAnnotationMirror(
								element, MapKeyClass.class
						), TypeUtils.DEFAULT_ANNOTATION_PARAMETER_NAME
				);
				keyType = typeMirror.toString();
			}
			else {
				keyType = getKeyType( declaredType );
			}
			return new AnnotationMetaMap(
					parent,
					element,
					collection,
					keyType,
					getElementType( declaredType, targetEntity )
			);
		}

		private TypeMirror getCollectionElementType(DeclaredType t, String fqNameOfReturnedType, String explicitTargetEntityName) {
			TypeMirror collectionElementType;
			if ( explicitTargetEntityName != null ) {
				Elements elements = context.getProcessingEnvironment().getElementUtils();
				TypeElement element = elements.getTypeElement( explicitTargetEntityName );
				collectionElementType = element.asType();
			}
			else {
				List<? extends TypeMirror> typeArguments = t.getTypeArguments();
				if ( typeArguments.size() == 0 ) {
					throw new MetaModelGenerationException( "Unable to determine collection type for property in " + getSimpleName() );
				}
				else if ( Map.class.getCanonicalName().equals( fqNameOfReturnedType ) ) {
					collectionElementType = t.getTypeArguments().get( 1 );
				}
				else {
					collectionElementType = t.getTypeArguments().get( 0 );
				}
			}
			return collectionElementType;
		}

		private String getElementType(DeclaredType declaredType, String targetEntity) {
			if ( targetEntity != null ) {
				return targetEntity;
			}
			final List<? extends TypeMirror> mirrors = declaredType.getTypeArguments();
			if ( mirrors.size() == 1 ) {
				final TypeMirror type = mirrors.get( 0 );
				return TypeUtils.extractClosestRealTypeAsString( type, context );
			}
			else if ( mirrors.size() == 2 ) {
				return TypeUtils.extractClosestRealTypeAsString( mirrors.get( 1 ), context );
			}
			else {
				//for 0 or many
				//0 is expected, many is not
				if ( mirrors.size() > 2 ) {
					context.logMessage(
							Diagnostic.Kind.WARNING, "Unable to find the closest solid type" + declaredType
					);
				}
				return "?";
			}
		}

		private String getKeyType(DeclaredType t) {
			return TypeUtils.extractClosestRealTypeAsString( t.getTypeArguments().get( 0 ), context );
		}

		/**
		 * @param annotations list of annotation mirrors.
		 *
		 * @return target entity class name as string or {@code null} if no targetEntity is here or if equals to void
		 */
		private String getTargetEntity(List<? extends AnnotationMirror> annotations) {

			String fullyQualifiedTargetEntityName = null;
			for ( AnnotationMirror mirror : annotations ) {
				if ( TypeUtils.isAnnotationMirrorOfType( mirror, ElementCollection.class ) ) {
					fullyQualifiedTargetEntityName = getFullyQualifiedClassNameOfTargetEntity( mirror, "targetClass" );
				}
				else if ( TypeUtils.isAnnotationMirrorOfType( mirror, OneToMany.class )
						|| TypeUtils.isAnnotationMirrorOfType( mirror, ManyToMany.class )
						|| TypeUtils.isAnnotationMirrorOfType( mirror, ManyToOne.class )
						|| TypeUtils.isAnnotationMirrorOfType( mirror, OneToOne.class ) ) {
					fullyQualifiedTargetEntityName = getFullyQualifiedClassNameOfTargetEntity( mirror, "targetEntity" );
				}
			}
			return fullyQualifiedTargetEntityName;
		}

		private String getFullyQualifiedClassNameOfTargetEntity(AnnotationMirror mirror, String parameterName) {
			assert mirror != null;
			assert parameterName != null;

			String targetEntityName = null;
			Object parameterValue = TypeUtils.getAnnotationValue( mirror, parameterName );
			if ( parameterValue != null ) {
				TypeMirror parameterType = ( TypeMirror ) parameterValue;
				if ( !parameterType.getKind().equals( TypeKind.VOID ) ) {
					targetEntityName = parameterType.toString();
				}
			}
			return targetEntityName;
		}
	}
}
