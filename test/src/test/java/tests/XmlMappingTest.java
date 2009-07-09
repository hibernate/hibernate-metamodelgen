package tests;

import static org.testng.Assert.assertNotNull;
import org.testng.annotations.Test;

/**
 * @author Hardy Ferentschik
 */
public class XmlMappingTest {
	@Test
	public void testXmlConfiguredEmbeddedClassGenerated() throws Exception {
		assertNotNull( Class.forName( "model.xmlmapped.Address_" ) );
	}

	@Test
	public void testXmlConfiguredMappedSuperclassGenerated() throws Exception {
		Class<?> building =  Class.forName( "model.xmlmapped.Building_" );
		assertNotNull( building );
		assertNotNull( building.getField( "address" ));
	}
}