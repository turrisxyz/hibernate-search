/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.codec.impl;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonElementTypes;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.engine.cfg.spi.NumberScaleConstants;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;

public class ElasticsearchBigDecimalFieldCodec implements ElasticsearchFieldCodec<BigDecimal> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final int decimalScale;
	private final BigDecimal scalingFactor;

	public ElasticsearchBigDecimalFieldCodec(int decimalScale) {
		this.decimalScale = decimalScale;
		scalingFactor = BigDecimal.TEN.pow( decimalScale, new MathContext( 10, RoundingMode.HALF_UP ) );
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "["
				+ "decimalScale=" + decimalScale
				+ "]";
	}

	@Override
	public JsonElement encode(BigDecimal value) {
		if ( value == null ) {
			return JsonNull.INSTANCE;
		}

		if ( isTooLarge( value ) ) {
			throw log.scaledNumberTooLarge( value );
		}

		return new JsonPrimitive( value );
	}

	@Override
	public BigDecimal decode(JsonElement element) {
		if ( element == null || element.isJsonNull() ) {
			return null;
		}

		return JsonElementTypes.BIG_DECIMAL.fromElement( element );
	}

	@Override
	public boolean isCompatibleWith(ElasticsearchFieldCodec<?> obj) {
		if ( this == obj ) {
			return true;
		}
		if ( getClass() != obj.getClass() ) {
			return false;
		}

		ElasticsearchBigDecimalFieldCodec other = (ElasticsearchBigDecimalFieldCodec) obj;
		return decimalScale == other.decimalScale;
	}

	public BigDecimal scalingFactor() {
		return scalingFactor;
	}

	private boolean isTooLarge(BigDecimal value) {
		BigDecimal scaled = value.multiply( scalingFactor );
		return (
			scaled.compareTo( NumberScaleConstants.MIN_LONG_AS_BIGDECIMAL ) < 0 ||
			scaled.compareTo( NumberScaleConstants.MAX_LONG_AS_BIGDECIMAL ) > 0
		);
	}
}