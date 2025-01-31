/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.work.operations;

import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

/**
 * Test behavior of "addOrUpdateOrDelete" when the entity is absent upon loading,
 * this resulting in a "delete".
 */
@RunWith(Enclosed.class)
public class PojoIndexingAddOrUpdateOrDeleteEntityAbsentIT {

	private static final PojoIndexingOperationScenario SCENARIO = new PojoIndexingAddOrUpdateOrDeleteScenario( BackendIndexingOperation.DELETE ) {
		@Override
		public boolean isEntityPresentOnLoading() {
			return false;
		}
	};

	public static class IndexingPlanNullEntityIT extends AbstractPojoIndexingPlanOperationNullEntityIT {
		@Override
		protected PojoIndexingOperationScenario scenario() {
			return SCENARIO;
		}
	}

	public static class IndexingPlanContainedNullEntityIT extends AbstractPojoIndexingPlanOperationContainedNullEntityIT {
		@Override
		protected PojoIndexingOperationScenario scenario() {
			return SCENARIO;
		}
	}

}
