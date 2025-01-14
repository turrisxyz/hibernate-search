/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.mapping.impl;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.search.mapper.orm.automaticindexing.impl.AutomaticIndexingTypeContextProvider;
import org.hibernate.search.mapper.orm.loading.impl.LoadingIndexedTypeContextProvider;
import org.hibernate.search.mapper.orm.logging.impl.Log;
import org.hibernate.search.mapper.orm.model.impl.HibernateOrmBasicTypeMetadataProvider;
import org.hibernate.search.mapper.orm.event.impl.HibernateOrmListenerTypeContextProvider;
import org.hibernate.search.mapper.orm.model.impl.HibernateOrmRawTypeIdentifierResolver;
import org.hibernate.search.mapper.orm.session.impl.HibernateOrmSessionTypeContextProvider;
import org.hibernate.search.mapper.orm.spi.BatchTypeIdentifierProvider;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

class HibernateOrmTypeContextContainer
		implements HibernateOrmListenerTypeContextProvider, HibernateOrmSessionTypeContextProvider,
		AutomaticIndexingTypeContextProvider, LoadingIndexedTypeContextProvider, BatchTypeIdentifierProvider {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	// Use a LinkedHashMap for deterministic iteration
	private final Map<PojoRawTypeIdentifier<?>, AbstractHibernateOrmTypeContext<?>> typeContexts = new LinkedHashMap<>();
	private final Map<String, AbstractHibernateOrmTypeContext<?>> typeContextsByHibernateOrmEntityName = new LinkedHashMap<>();
	private final Map<PojoRawTypeIdentifier<?>, HibernateOrmIndexedTypeContext<?>> indexedTypeContexts = new LinkedHashMap<>();
	private final Map<String, AbstractHibernateOrmTypeContext<?>> typeContextsByJpaEntityName = new LinkedHashMap<>();

	private final HibernateOrmRawTypeIdentifierResolver typeIdentifierResolver;

	private HibernateOrmTypeContextContainer(Builder builder, SessionFactoryImplementor sessionFactory) {
		for ( HibernateOrmIndexedTypeContext.Builder<?> contextBuilder : builder.indexedTypeContextBuilders ) {
			HibernateOrmIndexedTypeContext<?> indexedTypeContext = contextBuilder.build( sessionFactory );
			PojoRawTypeIdentifier<?> typeIdentifier = indexedTypeContext.typeIdentifier();
			typeContexts.put( typeIdentifier, indexedTypeContext );
			indexedTypeContexts.put( typeIdentifier, indexedTypeContext );
			typeContextsByHibernateOrmEntityName.put( indexedTypeContext.hibernateOrmEntityName(), indexedTypeContext );
			typeContextsByJpaEntityName.put( indexedTypeContext.jpaEntityName(), indexedTypeContext );
		}
		for ( HibernateOrmContainedTypeContext.Builder<?> contextBuilder : builder.containedTypeContextBuilders ) {
			HibernateOrmContainedTypeContext<?> containedTypeContext = contextBuilder.build( sessionFactory );
			PojoRawTypeIdentifier<?> typeIdentifier = containedTypeContext.typeIdentifier();
			typeContexts.put( typeIdentifier, containedTypeContext );
			typeContextsByHibernateOrmEntityName.put( containedTypeContext.hibernateOrmEntityName(), containedTypeContext );
			typeContextsByJpaEntityName.put( containedTypeContext.jpaEntityName(), containedTypeContext );
		}

		this.typeIdentifierResolver = builder.basicTypeMetadataProvider.getTypeIdentifierResolver();
	}

	@Override
	@SuppressWarnings("unchecked")
	public <E> AbstractHibernateOrmTypeContext<E> forExactType(PojoRawTypeIdentifier<E> typeIdentifier) {
		return (AbstractHibernateOrmTypeContext<E>) typeContexts.get( typeIdentifier );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <E> HibernateOrmIndexedTypeContext<E> indexedForExactType(PojoRawTypeIdentifier<E> typeIdentifier) {
		return (HibernateOrmIndexedTypeContext<E>) indexedTypeContexts.get( typeIdentifier );
	}

	@Override
	public AbstractHibernateOrmTypeContext<?> forJpaEntityName(String indexName) {
		return typeContextsByJpaEntityName.get( indexName );
	}

	@Override
	public AbstractHibernateOrmTypeContext<?> forHibernateOrmEntityName(String hibernateOrmEntityName) {
		return typeContextsByHibernateOrmEntityName.get( hibernateOrmEntityName );
	}

	@Override
	public <T> PojoRawTypeIdentifier<T> typeIdentifierForJavaClass(Class<T> clazz) {
		return typeIdentifierResolver.resolveByJavaClass( clazz );
	}

	@Override
	public PojoRawTypeIdentifier<?> typeIdentifierForHibernateOrmEntityName(String entityName) {
		PojoRawTypeIdentifier<?> result = typeIdentifierResolver.resolveByHibernateOrmEntityName( entityName );
		if ( result == null ) {
			throw log.invalidEntityName( entityName, typeIdentifierResolver.allKnownHibernateOrmEntityNames() );
		}
		return result;
	}

	@Override
	public PojoRawTypeIdentifier<?> typeIdentifierForEntityName(String entityName) {
		PojoRawTypeIdentifier<?> result = typeIdentifierResolver.resolveByJpaOrHibernateOrmEntityName( entityName );
		if ( result == null ) {
			throw log.invalidEntityName( entityName, typeIdentifierResolver.allKnownJpaOrHibernateOrmEntityNames() );
		}
		return result;
	}

	Collection<HibernateOrmIndexedTypeContext<?>> allIndexed() {
		return indexedTypeContexts.values();
	}

	static class Builder {

		private final HibernateOrmBasicTypeMetadataProvider basicTypeMetadataProvider;
		private final List<HibernateOrmIndexedTypeContext.Builder<?>> indexedTypeContextBuilders = new ArrayList<>();
		private final List<HibernateOrmContainedTypeContext.Builder<?>> containedTypeContextBuilders = new ArrayList<>();

		Builder(HibernateOrmBasicTypeMetadataProvider basicTypeMetadataProvider) {
			this.basicTypeMetadataProvider = basicTypeMetadataProvider;
		}

		<E> HibernateOrmIndexedTypeContext.Builder<E> addIndexed(PojoRawTypeModel<E> typeModel, String jpaEntityName) {
			HibernateOrmIndexedTypeContext.Builder<E> builder = new HibernateOrmIndexedTypeContext.Builder<>(
					typeModel,
					jpaEntityName, basicTypeMetadataProvider.getHibernateOrmEntityNameByJpaEntityName( jpaEntityName )
			);
			indexedTypeContextBuilders.add( builder );
			return builder;
		}

		<E> HibernateOrmContainedTypeContext.Builder<E> addContained(PojoRawTypeModel<E> typeModel, String jpaEntityName) {
			HibernateOrmContainedTypeContext.Builder<E> builder = new HibernateOrmContainedTypeContext.Builder<>(
					typeModel,
					jpaEntityName, basicTypeMetadataProvider.getHibernateOrmEntityNameByJpaEntityName( jpaEntityName )
			);
			containedTypeContextBuilders.add( builder );
			return builder;
		}

		HibernateOrmTypeContextContainer build(SessionFactoryImplementor sessionFactory) {
			return new HibernateOrmTypeContextContainer( this, sessionFactory );
		}
	}

}
