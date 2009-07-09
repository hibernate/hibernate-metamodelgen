package org.hibernate.jpa.metamodel.ap.annotation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Name;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.SimpleTypeVisitor6;
import javax.persistence.EmbeddedId;
import javax.persistence.Id;
import javax.persistence.AccessType;
import javax.persistence.Entity;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;
import javax.persistence.Embedded;
import javax.persistence.Embeddable;
import javax.persistence.Access;
import javax.tools.Diagnostic.Kind;
import javax.tools.Diagnostic;

import org.hibernate.jpa.metamodel.ap.IMetaEntity;
import org.hibernate.jpa.metamodel.ap.IMetaAttribute;
import org.hibernate.jpa.metamodel.ap.ImportContext;
import org.hibernate.jpa.metamodel.ap.ImportContextImpl;
import org.hibernate.jpa.metamodel.ap.TypeUtils;
import org.hibernate.jpa.metamodel.ap.Context;

public class MetaEntity implements IMetaEntity {

	final TypeElement element;
	final protected ProcessingEnvironment pe;

	final ImportContext importContext;
	private Context context;
	//used to propagate the access type of the root entity over to subclasses, superclasses and embeddable
	private AccessType defaultAccessTypeForHierarchy;

	public MetaEntity(ProcessingEnvironment pe, TypeElement element, Context context) {
		this.element = element;
		this.pe = pe;
		importContext = new ImportContextImpl( getPackageName() );
		this.context = context;
	}

	public MetaEntity(ProcessingEnvironment pe, TypeElement element, Context context, AccessType accessType) {
		this(pe, element, context);
		this.defaultAccessTypeForHierarchy = accessType;
	}

	public String getSimpleName() {
		return element.getSimpleName().toString();
	}

	public Element getOriginalElement() {
		return element;
	}

	public String getQualifiedName() {
		return element.getQualifiedName().toString();
	}

	public String getPackageName() {
		PackageElement packageOf = pe.getElementUtils().getPackageOf( element );
		return pe.getElementUtils().getName( packageOf.getQualifiedName() ).toString();
	}

	public List<IMetaAttribute> getMembers() {

		List<IMetaAttribute> members = new ArrayList<IMetaAttribute>();

		if ( useFields() ) {

			List<? extends Element> myMembers = ElementFilter.fieldsIn( element.getEnclosedElements() );

			pe.getMessager()
					.printMessage( Kind.NOTE, "Scanning " + myMembers.size() + " fields for " + element.toString() );

			for ( Element mymember : myMembers ) {

				MetaAttribute result = mymember.asType().accept( new TypeVisitor( this ), mymember );
				if ( result != null ) {
					members.add( result );
				}
				else {
					//pe.getMessager().printMessage( Kind.WARNING, "Could not find valid info for JPA property", mymember );
				}
			}

		}
		else {
			List<? extends Element> myMembers = ElementFilter.methodsIn( element.getEnclosedElements() );

			pe.getMessager()
					.printMessage( Kind.NOTE, "Scanning " + myMembers.size() + " methods for " + element.toString() );
			for ( Element mymember : myMembers ) {

				MetaAttribute result = mymember.asType().accept( new TypeVisitor( this ), mymember );
				if ( result != null ) {
					members.add( result );
				}
				else {
					//pe.getMessager().printMessage(Kind.WARNING, "Not a valid JPA property", mymember);
				}
			}
		}

		//process superclasses
		for(TypeElement superclass = TypeUtils.getSuperclass(element) ;
			superclass != null ;
			superclass = TypeUtils.getSuperclass( superclass ) ) {
			if ( superclass.getAnnotation( Entity.class ) != null ) {
				break; //will be handled or has been handled already
			}
			else if ( superclass.getAnnotation( MappedSuperclass.class ) != null ) {
				context.processElement( superclass, defaultAccessTypeForHierarchy );
			}
		}


		if ( members.size() == 0 ) {
			pe.getMessager().printMessage( Kind.WARNING, "No properties found on " + element, element );
		}
		return members;
	}

	private boolean useFields() {
		//default strategy has more power than local discovery
		//particularly @MappedSuperclass and @Embedded have defaultAccessTypeForHierarchy already filled
		if ( this.defaultAccessTypeForHierarchy != null ) {
			return defaultAccessTypeForHierarchy == AccessType.FIELD;
		}

		//get local strategy
		AccessType accessType = getAccessTypeForClass(element);
		if (accessType != null) {
			this.defaultAccessTypeForHierarchy = accessType;
			return accessType == AccessType.FIELD;
		}

		//we dont' know
		//if an enity go up
		//
		//superclasses alre always treated after their entities
		//and their access type are discovered
		//FIXME is it really true if only the superclass is changed
		TypeElement superClass = element;
		do {
			superClass = TypeUtils.getSuperclass( superClass );
			if (superClass != null) {
				if ( superClass.getAnnotation( Entity.class ) != null ) {
					//FIXME make it work for XML
					accessType = getAccessTypeForClass(superClass);
					if ( accessType != null ) {
						this.defaultAccessTypeForHierarchy = accessType;
						return accessType == AccessType.FIELD;
					}
				}
				else if ( superClass.getAnnotation( MappedSuperclass.class ) != null ) {
					accessType = getAccessTypeForClass(superClass);
					if ( accessType != null ) {
						this.defaultAccessTypeForHierarchy = accessType;
						return accessType == AccessType.FIELD;
					}
				}
				else {
					break; //neither @Entity nor @MappedSuperclass
				}
			}
		}
		while ( superClass != null );
		//this is a subclass so caching is OK
		this.defaultAccessTypeForHierarchy = accessType;
		context.addAccessType( this.element, AccessType.PROPERTY );
		return false; //default to getter
	}

	private AccessType getAccessTypeForClass(TypeElement searchedElement) {
		pe.getMessager().printMessage( Diagnostic.Kind.NOTE, "check class" + searchedElement );
		AccessType accessType = context.getAccessTypes().get( searchedElement );
		if ( accessType != null ) {
			this.defaultAccessTypeForHierarchy = accessType;
			pe.getMessager().printMessage( Diagnostic.Kind.NOTE, "Found in cache" + searchedElement + ":" + accessType );
			return accessType;
		}

		/**
		 * when forcing access type, we can only override the defaultAccessTypeForHierarchy
		 * if we are the entity root (identified by having @Id or @EmbeddedId
		 */
		final Access accessAnn = searchedElement.getAnnotation( Access.class );
		AccessType forcedAccessType = accessAnn != null ? accessAnn.value() : null;
		if ( forcedAccessType != null) {
			pe.getMessager().printMessage( Diagnostic.Kind.NOTE, "access type " + searchedElement + ":" + accessType );
			context.addAccessType( searchedElement, forcedAccessType );
		}
		//continue nevertheless to check if we are root and if defaultAccessTypeForHierarchy
		//should be overridden

		List<? extends Element> myMembers = searchedElement.getEnclosedElements();
		for ( Element subElement : myMembers ) {
			List<? extends AnnotationMirror> entityAnnotations =
					pe.getElementUtils().getAllAnnotationMirrors( subElement );

			for ( Object entityAnnotation : entityAnnotations ) {
				AnnotationMirror annotationMirror = ( AnnotationMirror ) entityAnnotation;

				final String annotationType = annotationMirror.getAnnotationType().toString();

				//FIXME consider XML
				if ( annotationType.equals( Id.class.getName() )
						|| annotationType.equals( EmbeddedId.class.getName() ) ) {
					pe.getMessager().printMessage( Diagnostic.Kind.NOTE, "Found id on" + searchedElement );
					final ElementKind kind = subElement.getKind();
					if ( kind == ElementKind.FIELD || kind == ElementKind.METHOD ) {
						accessType = kind == ElementKind.FIELD ? AccessType.FIELD : AccessType.PROPERTY;
						this.defaultAccessTypeForHierarchy = accessType;
						if ( forcedAccessType == null) {
							context.addAccessType( searchedElement, accessType );
							pe.getMessager().printMessage( Diagnostic.Kind.NOTE, "access type " + searchedElement + ":" + accessType );
							return accessType;
						}
						else {
							return forcedAccessType;
						}
					}
				}
			}
		}
		return null;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append( "MetaEntity" );
		sb.append( "{element=" ).append( element );
		sb.append( '}' );
		return sb.toString();
	}

	static Map<String, String> COLLECTIONS = new HashMap<String, String>();

	static {
		COLLECTIONS.put( "java.util.Collection", "javax.persistence.metamodel.CollectionAttribute" );
		COLLECTIONS.put( "java.util.Set", "javax.persistence.metamodel.SetAttribute" );
		COLLECTIONS.put( "java.util.List", "javax.persistence.metamodel.ListAttribute" );
		COLLECTIONS.put( "java.util.Map", "javax.persistence.metamodel.MapAttribute" );
	}

	class TypeVisitor extends SimpleTypeVisitor6<MetaAttribute, Element> {

		MetaEntity parent;

		TypeVisitor(MetaEntity parent) {
			this.parent = parent;
		}

		@Override
		protected MetaAttribute defaultAction(TypeMirror e, Element p) {
			return super.defaultAction( e, p );
		}

		@Override
		public MetaAttribute visitPrimitive(PrimitiveType t, Element p) {
			//FIXME consider XML
			if ( p.getAnnotation( Transient.class ) == null ) {
				return new MetaSingleAttribute( parent, p, TypeUtils.toTypeString( t ) );
			}
			else {
				return null;
			}
		}


		@Override
		public MetaAttribute visitDeclared(DeclaredType t, Element element) {
			//FIXME consider XML
			if ( element.getAnnotation( Transient.class ) == null ) {
				TypeElement returnedElement = ( TypeElement ) pe.getTypeUtils().asElement( t );
				String collection = COLLECTIONS.get( returnedElement.getQualifiedName().toString() ); // WARNING: .toString() is necessary here since Name equals does not compare to String
				//FIXME collection of element
				if ( collection != null ) {
					if ( collection.equals( "javax.persistence.metamodel.MapAttribute" ) ) {
						return new MetaMap( parent, element, collection, getKeyType( t ), getElementType( t ) );
					}
					else {
						return new MetaCollection( parent, element, collection, getElementType( t ) );
					}
				}
				else {
					//FIXME Consider XML
					if ( element.getAnnotation( Embedded.class ) != null
							|| returnedElement.getAnnotation( Embeddable.class ) != null ) {
						this.parent.context.processElement( returnedElement, 
								this.parent.defaultAccessTypeForHierarchy );
					}
					return new MetaSingleAttribute( parent, element, returnedElement.getQualifiedName().toString() );
				}
			}
			else {
				return null;
			}
		}


		@Override
		public MetaAttribute visitExecutable(ExecutableType t, Element p) {
			String string = p.getSimpleName().toString();

			// TODO: implement proper property get/is/boolean detection
			if ( string.startsWith( "get" ) || string.startsWith( "is" ) ) {
				TypeMirror returnType = t.getReturnType();

				return returnType.accept( this, p );
			}
			else {
				return null;
			}
		}
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

	private String getKeyType(DeclaredType t) {
		return t.getTypeArguments().get( 0 ).toString();
	}


	private String getElementType(DeclaredType declaredType) {
		if ( declaredType.getTypeArguments().size() == 1 ) {
			return declaredType.getTypeArguments().get( 0 ).toString();
		}
		else {
			return declaredType.getTypeArguments().get( 1 ).toString();
		}
	}
}
