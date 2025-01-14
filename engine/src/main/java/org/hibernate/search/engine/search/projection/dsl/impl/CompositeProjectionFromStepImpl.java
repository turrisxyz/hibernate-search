/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.projection.dsl.impl;

import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.dsl.CompositeProjectionAsStep;
import org.hibernate.search.engine.search.projection.dsl.CompositeProjectionFrom1AsStep;
import org.hibernate.search.engine.search.projection.dsl.CompositeProjectionFrom2AsStep;
import org.hibernate.search.engine.search.projection.dsl.CompositeProjectionFrom3AsStep;
import org.hibernate.search.engine.search.projection.dsl.CompositeProjectionFromStep;
import org.hibernate.search.engine.search.projection.dsl.ProjectionFinalStep;
import org.hibernate.search.engine.search.projection.dsl.spi.SearchProjectionDslContext;
import org.hibernate.search.engine.search.projection.spi.CompositeProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.ProjectionTypeKeys;
import org.hibernate.search.util.common.impl.Contracts;

public class CompositeProjectionFromStepImpl implements CompositeProjectionFromStep {

	private final CompositeProjectionBuilder builder;

	public CompositeProjectionFromStepImpl(SearchProjectionDslContext<?> dslContext) {
		this.builder = dslContext.scope().projectionBuilders().composite();
	}

	public CompositeProjectionFromStepImpl(SearchProjectionDslContext<?> dslContext, String objectFieldPath) {
		this.builder = dslContext.scope().fieldQueryElement( objectFieldPath, ProjectionTypeKeys.OBJECT );
	}

	@Override
	public <V1> CompositeProjectionFrom1AsStep<V1> from(SearchProjection<V1> projection) {
		return new CompositeProjectionFrom1AsStepImpl<>( builder, projection );
	}

	@Override
	public <V1, V2> CompositeProjectionFrom2AsStep<V1, V2> from(SearchProjection<V1> projection1,
			SearchProjection<V2> projection2) {
		return new CompositeProjectionFrom2AsStepImpl<>( builder, projection1, projection2 );
	}

	@Override
	public <V1, V2, V3> CompositeProjectionFrom3AsStep<V1, V2, V3> from(SearchProjection<V1> projection1,
			SearchProjection<V2> projection2, SearchProjection<V3> projection3) {
		return new CompositeProjectionFrom3AsStepImpl<>( builder, projection1, projection2, projection3 );
	}

	@Override
	public CompositeProjectionAsStep from(SearchProjection<?>... projections) {
		Contracts.assertNotNullNorEmpty( projections, "projections" );
		return new CompositeProjectionFromAnyNumberAsStep( builder, projections );
	}

	@Override
	public final CompositeProjectionAsStep from(ProjectionFinalStep<?>... dslFinalSteps) {
		Contracts.assertNotNullNorEmpty( dslFinalSteps, "dslFinalSteps" );
		SearchProjection<?>[] projections = new SearchProjection<?>[dslFinalSteps.length];
		for ( int i = 0; i < dslFinalSteps.length; i++ ) {
			projections[i] = dslFinalSteps[i].toProjection();
		}
		return from( projections );
	}

}
