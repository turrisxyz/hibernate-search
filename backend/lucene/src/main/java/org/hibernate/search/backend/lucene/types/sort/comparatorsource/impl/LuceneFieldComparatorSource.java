/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.sort.comparatorsource.impl;

import org.hibernate.search.backend.lucene.lowlevel.join.impl.NestedDocsProvider;

import org.apache.lucene.search.FieldComparatorSource;
import org.apache.lucene.search.Query;

public abstract class LuceneFieldComparatorSource extends FieldComparatorSource {

	protected final String nestedDocumentPath;
	protected final Query filter;

	protected NestedDocsProvider nestedDocsProvider;

	public LuceneFieldComparatorSource(String nestedDocumentPath, Query filter) {
		this.nestedDocumentPath = nestedDocumentPath;
		this.filter = filter;
	}

	public String getNestedDocumentPath() {
		return nestedDocumentPath;
	}

	public void setOriginalParentQuery(Query luceneQuery) {
		this.nestedDocsProvider = new NestedDocsProvider( nestedDocumentPath, luceneQuery, filter );
	}
}