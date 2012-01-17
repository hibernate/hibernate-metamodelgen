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

package org.hibernate.jpamodelgen.test.inheritance.deep;

import static org.hibernate.jpamodelgen.test.util.TestUtil.*;

import org.hibernate.jpamodelgen.test.inheritance.AbstractEntity;
import org.hibernate.jpamodelgen.test.util.CompilationTest;
import org.testng.annotations.Test;

/**
 * Tests a deep class hierarchy mixed with inheritance and a root class that
 * does not declare an id
 * 
 * @author Igor Vaynberg
 * 
 */
public class DeepInheritanceTest extends CompilationTest {
	@Test
	public void testDeepInheritance() throws Exception {

		assertMetamodelClassGeneratedFor(Plane.class);
		assertMetamodelClassGeneratedFor(JetPlane.class);
		assertPresenceOfFieldInMetamodelFor( JetPlane.class, "jets", "jets should be defined in JetPlane_" );
		assertAttributeTypeInMetaModelFor(JetPlane.class, "jets",
				Integer.class, "jets should be defined in JetPlane_");
	}

	@Override
	protected String getPackageNameOfCurrentTest() {
		return DeepInheritanceTest.class.getPackage().getName();
	}
}
