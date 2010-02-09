// $Id: MixedModeMappingTest.java 18683 2010-02-02 21:51:40Z hardy.ferentschik $
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
package org.hibernate.jpamodelgen.test.mixedmode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.testng.annotations.Test;

import org.hibernate.jpamodelgen.test.util.CompilationTest;
import org.hibernate.jpamodelgen.test.util.TestUtil;

import static org.hibernate.jpamodelgen.test.util.TestUtil.assertAbsenceOfFieldInMetamodelFor;
import static org.hibernate.jpamodelgen.test.util.TestUtil.assertMetamodelClassGeneratedFor;
import static org.hibernate.jpamodelgen.test.util.TestUtil.assertPresenceOfFieldInMetamodelFor;

/**
 * @author Hardy Ferentschik
 */
public class MixedConfigurationTest extends CompilationTest {
	@Test
	public void testDefaultAccessTypeApplied() {
		assertMetamodelClassGeneratedFor( Vehicle.class );
		assertMetamodelClassGeneratedFor( Car.class );
		assertAbsenceOfFieldInMetamodelFor( Car.class, "horsePower" );
	}

	@Test
	public void testExplicitXmlConfiguredAccessTypeApplied() {
		assertMetamodelClassGeneratedFor( Vehicle.class );
		assertMetamodelClassGeneratedFor( Truck.class );
		assertPresenceOfFieldInMetamodelFor(
				Truck.class, "horsePower", "Property 'horsePower' has explicit access type and should be in metamodel"
		);
	}

	@Test
	public void testMixedConfiguration() {
		assertMetamodelClassGeneratedFor( RentalCar.class );
		assertMetamodelClassGeneratedFor( RentalCompany.class );
		assertPresenceOfFieldInMetamodelFor(
				RentalCar.class, "company", "Property 'company' should be included due to xml configuration"
		);
		assertPresenceOfFieldInMetamodelFor(
				RentalCar.class, "insurance", "Property 'insurance' should be included since it is an embeddable"
		);
	}

	@Override
	protected String getPackageNameOfTestSources() {
		return MixedConfigurationTest.class.getPackage().getName();
	}

	@Override
	protected Collection<String> getOrmFiles() {
		List<String> ormFiles = new ArrayList<String>();
		String dir = TestUtil.fcnToPath( MixedConfigurationTest.class.getPackage().getName() );
		ormFiles.add( dir + "/car.xml" );
		ormFiles.add( dir + "/rentalcar.xml" );
		ormFiles.add( dir + "/truck.xml" );
		return ormFiles;
	}
}