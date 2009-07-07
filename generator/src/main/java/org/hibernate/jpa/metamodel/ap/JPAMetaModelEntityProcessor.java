package org.hibernate.jpa.metamodel.ap;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Generated;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.FilerException;
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

//@SupportedAnnotationTypes("javax.persistence.Entity")
@SupportedAnnotationTypes("*")
@SupportedSourceVersion(RELEASE_6)
public class JPAMetaModelEntityProcessor extends AbstractProcessor {

	private static final String PATH_SEPARATOR = "/";
	private static final String PERSISTENCE_XML = "/META-INF/persistence.xml";

	private final Map<String, IMetaEntity> metaEntities = new HashMap<String, IMetaEntity>();
	private boolean xmlProcessed = false;

	public void init(ProcessingEnvironment env) {
		super.init( env );
		processingEnv.getMessager().printMessage( Diagnostic.Kind.NOTE, "Init Processor " + this );
	}

	@Override
	public boolean process(final Set<? extends TypeElement> annotations,
						   final RoundEnvironment roundEnvironment) {

		if ( roundEnvironment.processingOver() ) {
			//assuming that when processing is over, we are done and clear resources like ORM parsing
			//we could keep some ORM parsing in memory but how to detect that a file has changed / not changed?
			xmlProcessed = false;
			metaEntities.clear();
			processingEnv.getMessager().printMessage( Diagnostic.Kind.NOTE, "Clear ORM processing resources" );
			return false;
		}

		if ( !processingRoundConstainsEntities( annotations ) ) {
			processingEnv.getMessager()
					.printMessage( Diagnostic.Kind.NOTE, "Current processing round does not contain entities" );
			return true;
		}

		writeProcessingDiagnostics( annotations, roundEnvironment );

		parsePersistenceXml();

		Set<? extends Element> elements = roundEnvironment.getRootElements();
		for ( Element element : elements ) {
			handleRootElementAnnotationMirrors( element );
		}

		return true;
	}

	private boolean processingRoundConstainsEntities(Set<? extends TypeElement> annotations) {
		for ( TypeElement type : annotations ) {
			if ( type.getQualifiedName().toString().equals( javax.persistence.Entity.class.getName() ) ) {
				return true;
			}
		}
		return false;
	}

	private void parsePersistenceXml() {
		if ( xmlProcessed ) {
			return;
		}

		Persistence persistence = parseXml( PERSISTENCE_XML, Persistence.class );

		if ( persistence != null ) {
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
		Collection<Entity> entities = mappings.getEntity();
		String packageName = mappings.getPackage();
		for ( Entity entity : entities ) {
			String fullyQualifiedClassName = packageName + "." + entity.getClazz();
			Elements utils = processingEnv.getElementUtils();
			XmlMetaEntity metaEntity = new XmlMetaEntity(
					entity, packageName, utils.getTypeElement( fullyQualifiedClassName )
			);

			if ( metaEntities.containsKey( fullyQualifiedClassName ) ) {
				processingEnv.getMessager().printMessage(
						Diagnostic.Kind.WARNING,
						fullyQualifiedClassName + " was already processed once. Skipping second occurance."
				);
			}

			writeFile( metaEntity );
			metaEntities.put( fullyQualifiedClassName, metaEntity );
		}
	}

	private void writeProcessingDiagnostics(Set<? extends TypeElement> annotations, RoundEnvironment roundEnvironment) {
		StringBuilder sb = new StringBuilder();
		sb.append( "\n>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>\n" );
		sb.append( new SimpleDateFormat().format( new Date() ) );
		sb.append( "\n" );
		sb.append( "Processing annotations " ).append( annotations ).append( " on:" );

		Set<? extends Element> elements = roundEnvironment.getRootElements();
		sb.append( "\n" );
		for ( Element element : elements ) {
			sb.append( element.toString() );
			sb.append( "\n" );
		}
		sb.append( ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>\n" );
		processingEnv.getMessager().printMessage( Diagnostic.Kind.NOTE, sb.toString() );
	}

	private void handleRootElementAnnotationMirrors(final Element element) {

		List<? extends AnnotationMirror> annotationMirrors = element
				.getAnnotationMirrors();

		for ( AnnotationMirror mirror : annotationMirrors ) {
			final String annotationType = mirror.getAnnotationType().toString();

			if ( element.getKind() == ElementKind.CLASS &&
					annotationType.equals( javax.persistence.Entity.class.getName() ) ) {
				MetaEntity metaEntity = new MetaEntity( processingEnv, ( TypeElement ) element );
				writeFile( metaEntity );
			}
		}
	}

	private void writeFile(IMetaEntity entity) {

		try {
			String metaModelPackage = entity.getPackageName();

			StringBuffer body = generateBody( entity );

			FileObject fo = processingEnv.getFiler().createSourceFile(
					metaModelPackage + "." + entity.getSimpleName() + "_"
			);
			OutputStream os = fo.openOutputStream();
			PrintWriter pw = new PrintWriter( os );

			pw.println( "package " + metaModelPackage + ";" );

			pw.println();

			pw.println( entity.generateImports() );

			pw.println( body );

			pw.flush();
			pw.close();

		}
		catch ( FilerException filerEx ) {
			processingEnv.getMessager().printMessage(
					Diagnostic.Kind.ERROR,
					"Problem with Processing Environment Filer: "
							+ filerEx.getMessage()
			);
		}
		catch ( IOException ioEx ) {
			processingEnv.getMessager().printMessage(
					Diagnostic.Kind.ERROR,
					"Problem opening file to write MetaModel for " + entity.getSimpleName()
							+ ioEx.getMessage()
			);
		}
	}

	/**
	 * Generate everything after import statements.
	 *
	 * @param entity The meta entity for which to write the body
	 *
	 * @return body content
	 */
	private StringBuffer generateBody(IMetaEntity entity) {

		StringWriter sw = new StringWriter();
		PrintWriter pw = null;
		try {

			pw = new PrintWriter( sw );

			pw.println( "@" + entity.importType( Generated.class.getName() ) + "(\"JPA MetaModel for " + entity.getQualifiedName() + "\")" );

			pw.println( "@" + entity.importType( "javax.persistence.metamodel.TypesafeMetamodel" ) + "(" + entity.getSimpleName() + ".class)" );

			pw.println( "public abstract class " + entity.getSimpleName() + "_" + " {" );

			pw.println();

			List<IMetaMember> members = entity.getMembers();

			for ( IMetaMember metaMember : members ) {
				pw.println( "	" + metaMember.getDeclarationString() );
			}
			pw.println();
			pw.println( "}" );
			return sw.getBuffer();
		}
		finally {
			if ( pw != null ) {
				pw.close();
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
							"Could not load " + resource + " using Filer.getResource(). Trying classpath..."
					);
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
