package de.familienwecker.famwake.ui.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class UiTextTest {

    @Test
    fun dynamicString_holdsCorrectValue() {
        val uiText = UiText.DynamicString("Guten Morgen")
        assertEquals("Guten Morgen", uiText.value)
    }

    @Test
    fun dynamicString_equalityWorksCorrectly() {
        val text1 = UiText.DynamicString("Hallo")
        val text2 = UiText.DynamicString("Hallo")
        val text3 = UiText.DynamicString("Tschüss")

        assertEquals(text1, text2)
        assertNotEquals(text1, text3)
    }
}
