package com.watchyourself.watchperiod

import com.watchyourself.watchperiod.domain.Hormone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VisibleHormonesTest {

    @Test
    fun csvRoundTripKeepsEnumOrder() {
        val selection = setOf(Hormone.CORTISOL, Hormone.ESTROGEN, Hormone.LH)
        val csv = Hormone.toCsv(selection)

        assertEquals("LH,ESTROGEN,CORTISOL", csv)
        assertEquals(selection, Hormone.parseCsv(csv))
    }

    @Test
    fun unknownNamesAreIgnored() {
        assertEquals(setOf(Hormone.ESTROGEN), Hormone.parseCsv("ESTROGEN,UNKNOWN"))
    }

    @Test
    fun blankValuesParseToEmpty() {
        assertTrue(Hormone.parseCsv(null).isEmpty())
        assertTrue(Hormone.parseCsv("").isEmpty())
        assertTrue(Hormone.parseCsv(" , ").isEmpty())
    }

    @Test
    fun orderedFiltersAndSortsByEnumOrder() {
        val ordered = Hormone.ordered(setOf(Hormone.PROSTAGLANDIN, Hormone.TESTOSTERONE, Hormone.LH))

        assertEquals(listOf(Hormone.TESTOSTERONE, Hormone.LH, Hormone.PROSTAGLANDIN), ordered)
    }
}
