import org.testng.annotations.Test;
import org.testng.Assert;
import model.Customer_;
import model.User_;

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
}
