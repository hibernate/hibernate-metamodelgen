// $Id$
package org.hibernate.jpa.metamodel.ap;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import static javax.lang.model.SourceVersion.RELEASE_6;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.persistence.Embeddable;
import javax.persistence.MappedSuperclass;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.hibernate.jpa.metamodel.ap.annotation.MetaEntity;
import org.hibernate.jpa.metamodel.ap.xml.XmlMetaEntity;
import org.hibernate.jpa.metamodel.xml.jaxb.Entity;
import org.hibernate.jpa.metamodel.xml.jaxb.EntityMappings;
import org.hibernate.jpa.metamodel.xml.jaxb.ObjectFactory;
import org.hibernate.jpa.metamodel.xml.jaxb.Persistence;

/**
 * Main annotation processor.
 *
 * @author Max Andersen
 * @author Hardy Ferentschik
 */
//@SupportedAnnotationTypes("javax.persistence.Entity")
@SupportedAnnotationTypes("*")
@SupportedSourceVersion(RELEASE_6)
public class JPAMetaModelEntityProcessor extends AbstractProcessor {

	private static final String PATH_SEPARATOR = "/";
	private static final String PERSISTENCE_XML = "/META-INF/persistence.xml";
	private static final Boolean ALLOW_OTHER_PROCESSORS_TO_CLAIM_ANNOTATIONS = Boolean.TRUE;
	private static final String ENTITY_ANN = javax.persistence.Entity.class.getName();
	private static final String MAPPED_SUPERCLASS_ANN = MappedSuperclass.class.getName();
	private static final String EMBEDDABLE_ANN = Embeddable.class.getName();

	private boolean xmlProcessed = false;
	private Context context;

	public void init(ProcessingEnvironment env) {
		super.init( env );
		context = new Context( env );
		processingEnv.getMessager().printMessage( Diagnostic.Kind.NOTE, "Init Processor " + this );
	}

	@Override
	public boolean process(final Set<? extends TypeElement> annotations,
						   final RoundEnvironment roundEnvironment) {

		if ( roundEnvironment.processingOver() ) {
			processingEnv.getMessager()
					.printMessage( Diagnostic.Kind.NOTE, "Last processing round." );

			createMetaModelClasses();

			processingEnv.getMessager()
					.printMessage( Diagnostic.Kind.NOTE, "Finished processing" );
			return ALLOW_OTHER_PROCESSORS_TO_CLAIM_ANNOTATIONS;
		}

		if ( !xmlProcessed ) {
			parsePersistenceXml();
		}

		if ( !hostJPAAnnotations( annotations ) ) {
			processingEnv.getMessager()
					.printMessage( Diagnostic.Kind.NOTE, "Current processing round does not contain entities" );
			return ALLOW_OTHER_PROCESSORS_TO_CLAIM_ANNOTATIONS;
		}

		Set<? extends Element> elements = roundEnvironment.getRootElements();
		for ( Element element : elements ) {
			processingEnv.getMessager().printMessage( Diagnostic.Kind.NOTE, "Processing " + element.toString() );
			handleRootElementAnnotationMirrors( element );
		}

		return !ALLOW_OTHER_PROCESSORS_TO_CLAIM_ANNOTATIONS;
	}

	private void createMetaModelClasses() {
		for ( IMetaEntity entity : context.getMetaEntitiesToProcess().values() ) {
			processingEnv.getMessager()
					.printMessage( Diagnostic.Kind.NOTE, "Writing meta model for " + entity );
			ClassWriter.writeFile( entity, processingEnv, context );
		}

		//process left over, in most cases is empty
		for ( String className : context.getElementsAlreadyProcessed() ) {
			context.getMetaSuperclassAndEmbeddableToProcess().remove( className );
		}

		for ( IMetaEntity entity : context.getMetaSuperclassAndEmbeddableToProcess().values() ) {
			processingEnv.getMessager()
					.printMessage( Diagnostic.Kind.NOTE, "Writing meta model for " + entity );
			ClassWriter.writeFile( entity, processingEnv, context );
		}
	}

	private boolean hostJPAAnnotations(Set<? extends TypeElement> annotations) {
		for ( TypeElement type : annotations ) {
			final String typeName = type.getQualifiedName().toString();
			if ( typeName.equals( ENTITY_ANN ) ) {
				return true;
			}
			else if ( typeName.equals( EMBEDDABLE_ANN ) ) {
				return true;
			}
			else if ( typeName.equals( MAPPED_SUPERCLASS_ANN ) ) {
				return true;
			}
		}
		return false;
	}

	private void parsePersistenceXml() {
		Persistence persistence = parseXml( PERSISTENCE_XML, Persistence.class );
		if ( persistence != null )

		{
			List<Persistence.PersistenceUnit> persistenceUnits = persistence.getPersistenceUnit();
			for ( Persistence.PersistenceUnit unit : persistenceUnits ) {
				List<String> mappingFiles = unit.getMappingFile();
				for ( String mappingFile : mappingFiles ) {
					parsingOrmXml( mappingFile );
				}
			}
		}
		xmlProcessed = true;
	}


	private void parsingOrmXml(String resource) {
		EntityMappings mappings = parseXml( resource, EntityMappings.class );
		if ( mappings == null ) {
			return;
		}

		parseEntities( mappings );
		parseEmbeddable( mappings );
		parseMappedSuperClass( mappings );
	}

	private void parseEntities(EntityMappings mappings) {
		String packageName = mappings.getPackage();
		Collection<Entity> entities = mappings.getEntity();
		for ( Entity entity : entities ) {
			String fullyQualifiedClassName = packageName + "." + entity.getClazz();
			Elements utils = processingEnv.getElementUtils();
			XmlMetaEntity metaEntity = new XmlMetaEntity(
					entity, packageName, utils.getTypeElement( fullyQualifiedClassName )
			);

			if ( context.getMetaEntitiesToProcess().containsKey( fullyQualifiedClassName ) ) {
				processingEnv.getMessager().printMessage(
						Diagnostic.Kind.WARNING,
						fullyQualifiedClassName + " was already processed once. Skipping second occurance."
				);
			}
			context.getMetaEntitiesToProcess().put( fullyQualifiedClassName, metaEntity );
		}
	}

	private void parseEmbeddable(EntityMappings mappings) {
		String packageName = mappings.getPackage();
		Collection<org.hibernate.jpa.metamodel.xml.jaxb.Embeddable> embeddables = mappings.getEmbeddable();
		for ( org.hibernate.jpa.metamodel.xml.jaxb.Embeddable embeddable : embeddables ) {
			String fullyQualifiedClassName = packageName + "." + embeddable.getClazz();
			Elements utils = processingEnv.getElementUtils();
			XmlMetaEntity metaEntity = new XmlMetaEntity(
					embeddable, packageName, utils.getTypeElement( fullyQualifiedClassName )
			);

			if ( context.getMetaSuperclassAndEmbeddableToProcess().containsKey( fullyQualifiedClassName ) ) {
				processingEnv.getMessager().printMessage(
						Diagnostic.Kind.WARNING,
						fullyQualifiedClassName + " was already processed once. Skipping second occurance."
				);
			}
			context.getMetaSuperclassAndEmbeddableToProcess().put( fullyQualifiedClassName, metaEntity );
		}
	}

	private void parseMappedSuperClass(EntityMappings mappings) {
		String packageName = mappings.getPackage();
		Collection<org.hibernate.jpa.metamodel.xml.jaxb.MappedSuperclass> mappedSuperClasses = mappings.getMappedSuperclass();
		for ( org.hibernate.jpa.metamodel.xml.jaxb.MappedSuperclass mappedSuperClass : mappedSuperClasses ) {
			String fullyQualifiedClassName = packageName + "." + mappedSuperClass.getClazz();
			Elements utils = processingEnv.getElementUtils();
			XmlMetaEntity metaEntity = new XmlMetaEntity(
					mappedSuperClass, packageName, utils.getTypeElement( fullyQualifiedClassName )
			);

			if ( context.getMetaSuperclassAndEmbeddableToProcess().containsKey( fullyQualifiedClassName ) ) {
				processingEnv.getMessager().printMessage(
						Diagnostic.Kind.WARNING,
						fullyQualifiedClassName + " was already processed once. Skipping second occurance."
				);
			}
			context.getMetaSuperclassAndEmbeddableToProcess().put( fullyQualifiedClassName, metaEntity );
		}
	}

	private void handleRootElementAnnotationMirrors(final Element element) {

		List<? extends AnnotationMirror> annotationMirrors = element
				.getAnnotationMirrors();

		for ( AnnotationMirror mirror : annotationMirrors ) {
			final String annotationType = mirror.getAnnotationType().toString();

			if ( element.getKind() == ElementKind.CLASS ) {
				if ( annotationType.equals( ENTITY_ANN ) ) {
					MetaEntity metaEntity = new MetaEntity( processingEnv, ( TypeElement ) element, context );
					// TODO instead of just adding the entity we have to do some merging.
					context.getMetaEntitiesToProcess().put( metaEntity.getQualifiedName(), metaEntity );
				}
				else if ( annotationType.equals( MAPPED_SUPERCLASS_ANN )
						|| annotationType.equals( EMBEDDABLE_ANN ) ) {
					MetaEntity metaEntity = new MetaEntity( processingEnv, ( TypeElement ) element, context );

					// TODO instead of just adding the entity we have to do some merging.
					context.getMetaSuperclassAndEmbeddableToProcess().put( metaEntity.getQualifiedName(), metaEntity );
				}
			}
		}
	}

	private InputStream getInputStreamForResource(String resource) {
		String pkg = getPackage( resource );
		String name = getRelativeName( resource );
		processingEnv.getMessager()
				.printMessage( Diagnostic.Kind.NOTE, "Checking for " + resource );
		InputStream ormStream;
		try {
			FileObject fileObject = processingEnv.getFiler().getResource( StandardLocation.CLASS_OUTPUT, pkg, name );
			ormStream = fileObject.openInputStream();
		}
		catch ( IOException e1 ) {
			processingEnv.getMessager()
					.printMessage(
							Diagnostic.Kind.WARNING,
							"Could not load " + resource + " using processingEnv.getFiler().getResource(). Using classpath..."
					);

			// TODO
			// unfortunately, the Filer.getResource API seems not to be able to load from /META-INF. One gets a
			// FilerException with the message with "Illegal name /META-INF". This means that we have to revert to
			// using the classpath. This might mean that we find a persistence.xml which is 'part of another jar.
			// Not sure what else we can do here
			ormStream = this.getClass().getResourceAsStream( resource );
		}
		return ormStream;
	}

	/**
	 * Tries to open the specified xml file and return an instance of the specified class using JAXB.
	 *
	 * @param resource the xml file name
	 * @param clazz The type of jaxb node to return
	 *
	 * @return The top level jaxb instance contained in the xml file or {@code null} in case the file could not be found.
	 */
	private <T> T parseXml(String resource, Class<T> clazz) {

		InputStream stream = getInputStreamForResource( resource );

		if ( stream == null ) {
			processingEnv.getMessager().printMessage( Diagnostic.Kind.NOTE, resource + " not found." );
			return null;
		}
		try {
			JAXBContext jc = JAXBContext.newInstance( ObjectFactory.class );
			Unmarshaller unmarshaller = jc.createUnmarshaller();
			return clazz.cast( unmarshaller.unmarshal( stream ) );
		}
		catch ( JAXBException e ) {
			processingEnv.getMessager().printMessage( Diagnostic.Kind.NOTE, "Error unmarshalling " + resource );
			e.printStackTrace();
			return null;
		}
		catch ( Exception e ) {
			processingEnv.getMessager().printMessage(
					Diagnostic.Kind.ERROR,
					"Problem while reading " + resource + " " + e.getMessage()
			);
			e.printStackTrace();
			return null;
		}
	}

	private String getPackage(String resourceName) {
		if ( !resourceName.contains( PATH_SEPARATOR ) ) {
			return "";
		}
		else {
			return resourceName.substring( 0, resourceName.lastIndexOf( PATH_SEPARATOR ) );
		}
	}

	private String getRelativeName(String resourceName) {
		if ( !resourceName.contains( PATH_SEPARATOR ) ) {
			return resourceName;
		}
		else {
			return resourceName.substring( resourceName.lastIndexOf( PATH_SEPARATOR ) + 1 );
		}
	}
}
