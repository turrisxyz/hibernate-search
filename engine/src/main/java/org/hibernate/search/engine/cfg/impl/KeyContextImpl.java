/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.cfg.impl;

import java.util.function.Function;

import org.hibernate.search.engine.cfg.spi.ConvertUtils;
import org.hibernate.search.engine.cfg.spi.KeyContext;
import org.hibernate.search.engine.cfg.spi.OptionalPropertyContext;
import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.util.common.impl.Contracts;

public class KeyContextImpl implements KeyContext {

	private final String key;

	public KeyContextImpl(String key) {
		this.key = key;
	}

	@Override
	public OptionalPropertyContext<String> asString() {
		return as( String.class, Function.identity() );
	}

	@Override
	public OptionalPropertyContext<Boolean> asBoolean() {
		return new OptionalPropertyContextImpl<>( key, ConvertUtils::convertBoolean );
	}

	@Override
	public OptionalPropertyContext<Integer> asIntegerPositiveOrZeroOrNegative() {
		return new OptionalPropertyContextImpl<>( key, ConvertUtils::convertInteger );
	}

	@Override
	public OptionalPropertyContext<Integer> asIntegerPositiveOrZero() {
		return new OptionalPropertyContextImpl<>( key, ConvertUtils::convertInteger )
				.validate( value -> Contracts.assertPositiveOrZero( value, "value" ) );
	}

	@Override
	public OptionalPropertyContext<Integer> asIntegerStrictlyPositive() {
		return new OptionalPropertyContextImpl<>( key, ConvertUtils::convertInteger )
				.validate( value -> Contracts.assertStrictlyPositive( value, "value" ) );
	}

	@Override
	public OptionalPropertyContext<Long> asLongPositiveOrZeroOrNegative() {
		return new OptionalPropertyContextImpl<>( key, ConvertUtils::convertLong );
	}

	@Override
	public <T> OptionalPropertyContext<BeanReference<? extends T>> asBeanReference(Class<T> expectedBeanType) {
		return new OptionalPropertyContextImpl<>( key, v -> ConvertUtils.convertBeanReference( expectedBeanType, v ) );
	}

	@Override
	public <T> OptionalPropertyContext<T> as(Class<T> expectedType, Function<String, T> parser) {
		return new OptionalPropertyContextImpl<>( key, v -> ConvertUtils.convert( expectedType, parser, v ) );
	}
}
