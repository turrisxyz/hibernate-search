/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.query.validation;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.hibernate.Transaction;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.util.common.SearchException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Hardy Ferentschik
 */
public class QueryValidationTest extends SearchTestBase {
	private FullTextSession fullTextSession;

	@Before
	@Override
	public void setUp() throws Exception {
		super.setUp();

		Transaction tx = openSession().beginTransaction();
		getSession().save( new A() );
		tx.commit();
		getSession().close();

		this.fullTextSession = Search.getFullTextSession( openSession() );
	}

	@After
	@Override
	public void tearDown() throws Exception {
		fullTextSession.close();
		super.tearDown();
	}

	@Test
	public void testTargetingNonIndexedEntityThrowsException() {
		TermQuery query = new TermQuery( new Term( "foo", "bar" ) );
		assertThatThrownBy( () -> fullTextSession.createFullTextQuery( query, C.class ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll( "Invalid target types: [" + C.class.getName() + "]",
						"These types are not indexed, nor is any of their subtypes" );
	}

	@Test
	public void testTargetingNonConfiguredEntityThrowsException() {
		TermQuery query = new TermQuery( new Term( "foo", "bar" ) );
		assertThatThrownBy( () -> fullTextSession.createFullTextQuery( query, D.class ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll( "Invalid target types: [" + D.class.getName() + "]",
						"These types are not indexed, nor is any of their subtypes" );
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				A.class,
				B.class,
				C.class
		};
	}

	@Entity
	@Indexed
	public static class A {
		@Id
		@GeneratedValue
		private long id;

		@Field
		@Column(name = "\"value\"")
		private String value;
	}

	@Entity
	@Indexed
	public static class B {
		@Id
		@GeneratedValue
		private long id;

		@Field
		@Column(name = "\"value\"")
		private long value;
	}

	@Entity
	public static class C {
		@Id
		@GeneratedValue
		private long id;
	}

	@Entity
	public static class D {
		@Id
		@GeneratedValue
		private long id;
	}
}


