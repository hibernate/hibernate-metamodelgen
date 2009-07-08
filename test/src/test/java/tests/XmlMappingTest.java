package tests;

import static org.testng.Assert.assertNotNull;
import org.testng.annotations.Test;

/**
 * @author Hardy Ferentschik
 */
@Test
public class XmlMappingTest {
	@Test
	public void testDefaultAccessType() throws Exception {
		assertNotNull( Class.forName( "model.xmlmapped.Address_" ) );
	}
}