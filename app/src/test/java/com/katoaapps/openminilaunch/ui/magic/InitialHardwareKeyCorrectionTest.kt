package com.katoaapps.openminilaunch.ui.magic

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import org.junit.Assert.assertEquals
import org.junit.Test

class InitialHardwareKeyCorrectionTest {
    @Test fun singleAlternateCharacterReplacesCapturedHardwareCharacter() {
        val correction = InitialHardwareKeyCorrection(now = { 100L })
        correction.begin("p", keyCode = 44, alternates = setOf("P", "?"))

        val corrected = correction.correct(TextFieldValue("p?", TextRange(2)))

        assertEquals("?", corrected.text)
        assertEquals(TextRange(1), corrected.selection)
    }

    @Test fun secondKeyIsAppendedWhenFirstKeyUpWasLost() {
        val correction = InitialHardwareKeyCorrection(now = { 100L })
        correction.begin("w", keyCode = 51, alternates = setOf("W", "1"))

        val corrected = correction.correct(TextFieldValue("wh", TextRange(2)))

        assertEquals("wh", corrected.text)
        assertEquals(TextRange(2), corrected.selection)
    }

    @Test fun characterIsNeverReplacedWithoutKnownAlternates() {
        val correction = InitialHardwareKeyCorrection(now = { 100L })
        correction.begin("w", keyCode = 51)

        assertEquals("wh", correction.correct(TextFieldValue("wh", TextRange(2))).text)
    }

    @Test fun spaceAfterLetterIsKept() {
        val correction = InitialHardwareKeyCorrection(now = { 100L })
        correction.begin("w", keyCode = 51, alternates = setOf("W"))

        assertEquals("w ", correction.correct(TextFieldValue("w ", TextRange(2))).text)
    }

    @Test fun autoSpaceAfterPunctuationIsDropped() {
        val correction = InitialHardwareKeyCorrection(now = { 100L })
        correction.begin(",", keyCode = 55)

        assertEquals(",", correction.correct(TextFieldValue(", ", TextRange(2))).text)
    }

    @Test fun largerEditorUpdatePreservesCapturedPrefix() {
        val correction = InitialHardwareKeyCorrection(now = { 100L })
        correction.begin("p", keyCode = 44, alternates = setOf("?"))

        val corrected = correction.correct(TextFieldValue("pmaps", TextRange(5)))

        assertEquals("pmaps", corrected.text)
        assertEquals(TextRange(5), corrected.selection)
    }
}
