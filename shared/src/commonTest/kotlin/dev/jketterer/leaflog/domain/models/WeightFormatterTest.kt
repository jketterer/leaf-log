package dev.jketterer.leaflog.domain.models

import dev.jketterer.leaflog.domain.models.WeightUnit.GRAMS
import dev.jketterer.leaflog.domain.models.WeightUnit.OUNCES
import kotlin.test.Test
import kotlin.test.assertEquals

class WeightFormatterTest {

    @Test
    fun `a leaf weight shows as many decimals as it is typed in`() {
        assertEquals("5 g", WeightFormatter.format(5.0, GRAMS))
        assertEquals("5.5 g", WeightFormatter.format(5.5, GRAMS))
        assertEquals("0.18 oz", WeightFormatter.format(5.0, OUNCES))
    }

    @Test
    fun `a large total moves to kilograms or pounds`() {
        assertEquals("1.2 kg", WeightFormatter.format(1234.0, GRAMS))
        assertEquals("1.0 lb", WeightFormatter.format(453.592, OUNCES))
    }
}
