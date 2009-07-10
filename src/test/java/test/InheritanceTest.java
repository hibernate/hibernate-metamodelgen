package test;

import org.testng.annotations.Test;
import org.testng.Assert;
import model.Customer_;
import model.User_;
import model.House_;
import model.Building_;
import model.Area_;

/**
 * @author Emmanuel Bernard
 */
@Test
public class InheritanceTest {
	@Test
	public void testSuperEntity() throws Exception {
		Assert.assertEquals( Customer_.class.getSuperclass(), User_.class,
				"Entity with super entity should inherit at the metamodel level");
	}

	@Test
	public void testMappedSuperclass() throws Exception {
		Assert.assertEquals( House_.class.getSuperclass(), Building_.class,
				"Entity with mapped superclass should inherit at the metamodel level");
		Assert.assertEquals( Building_.class.getSuperclass(), Area_.class,
				"mapped superclass with mapped superclass should inherit at the metamodel level");
	}
}
