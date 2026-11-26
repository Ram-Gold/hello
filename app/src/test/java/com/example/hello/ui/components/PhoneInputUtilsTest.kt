package com.example.hello.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PhoneInputUtilsTest {

    @Test
    fun sanitizeInput_preservesStandardInternationalAndLocalFormats() {
        assertEquals("+1 (555) 876-5432", PhoneInputUtils.sanitizeInput("+1 (555) 876-5432"))
        assertEquals("+44 20 7946 0958", PhoneInputUtils.sanitizeInput("+44 20 7946 0958"))
        assertEquals("0917 123 4567", PhoneInputUtils.sanitizeInput("0917 123 4567"))
        assertEquals("+63 917-123-4567", PhoneInputUtils.sanitizeInput("+63 917-123-4567"))
        assertEquals("555.123.4567", PhoneInputUtils.sanitizeInput("555.123.4567"))
    }

    @Test
    fun sanitizeInput_normalizesUnicodeBidiNbspFullwidthAndDashes() {
        // Bidi isolates/marks, fullwidth '+' and digits, NBSP, en-dash, em-dash, minus sign
        val messyPh = "\u202A\uFF0B\uFF16\uFF13\u00A0\uFF19\uFF11\uFF17\u2013\uFF11\uFF12\uFF13\u2014\uFF14\uFF15\uFF16\uFF17\u202C"
        assertEquals("+63 917-123-4567", PhoneInputUtils.sanitizeInput(messyPh))

        // All six Unicode dash variants normalize to '-'
        val allDashes = "1\u20102\u20113\u20124\u20135\u20146\u22127"
        assertEquals("1-2-3-4-5-6-7", PhoneInputUtils.sanitizeInput(allDashes))

        // Arabic-Indic decimal digits normalize to ASCII digits
        assertEquals("+20 1234567890", PhoneInputUtils.sanitizeInput("+\u0662\u0660\u00A0\u0661\u0662\u0663\u0664\u0665\u0666\u0667\u0668\u0669\u0660"))
    }

    @Test
    fun sanitizeInput_handlesLeadingAndConsecutiveSpacesAndInteractiveTrailingSpace() {
        assertEquals("", PhoneInputUtils.sanitizeInput(""))
        assertEquals("", PhoneInputUtils.sanitizeInput("   \t\u00A0\u2003  "))
        assertEquals("+1 555 000", PhoneInputUtils.sanitizeInput("  \u00A0 + \u00A0 1   555\t\t000"))
        assertEquals("+", PhoneInputUtils.sanitizeInput("  +   "))
        // Trailing space during live typing is retained (collapsed to one space)
        assertEquals("+1 ", PhoneInputUtils.sanitizeInput("+1  "))
    }

    @Test
    fun sanitizeInput_keepsAtMostOneLeadingPlusAndStripsLettersBidiEmojisAndSymbols() {
        assertEquals("+1 555-1234", PhoneInputUtils.sanitizeInput("+++\uFF0B1 555-1234"))
        assertEquals("+1 555 1234", PhoneInputUtils.sanitizeInput("+1 555 +1234"))
        assertEquals("123456", PhoneInputUtils.sanitizeInput("123+456"))
        assertEquals(
            "+1 (555) 123-4567",
            PhoneInputUtils.sanitizeInput("\u200ETel: \uD83D\uDCF1 +1 (555) 123-4567!@#\$%^&*_=")
        )
        assertEquals("", PhoneInputUtils.sanitizeInput("call me maybe \u200B\uFEFF\uD83D\uDE00"))
    }

    @Test
    fun normalizeForStorage_producesCanonicalDialableFormAcrossFormats() {
        assertEquals("+15558765432", PhoneInputUtils.normalizeForStorage("+1 (555) 876-5432"))
        assertEquals("5558765432", PhoneInputUtils.normalizeForStorage("(555) 876-5432"))
        assertEquals("+442079460958", PhoneInputUtils.normalizeForStorage("+44 20 7946 0958"))
        assertEquals("09171234567", PhoneInputUtils.normalizeForStorage("0917-123-4567"))
        assertEquals(
            "+639171234567",
            PhoneInputUtils.normalizeForStorage("\u202A\uFF0B\uFF16\uFF13\u00A0\uFF19\uFF11\uFF17\u2014\uFF11\uFF12\uFF13\u2013\uFF14\uFF15\uFF16\uFF17\u202C")
        )
        assertEquals("+15551234", PhoneInputUtils.normalizeForStorage("++1 555 +1234"))
        assertEquals("123456", PhoneInputUtils.normalizeForStorage("123+456"))
    }

    @Test
    fun normalizeForStorage_returnsNullForBlankOrZeroDigitInputs() {
        assertNull(PhoneInputUtils.normalizeForStorage(null))
        assertNull(PhoneInputUtils.normalizeForStorage(""))
        assertNull(PhoneInputUtils.normalizeForStorage("   \t\u00A0 "))
        assertNull(PhoneInputUtils.normalizeForStorage("---"))
        assertNull(PhoneInputUtils.normalizeForStorage("+"))
        assertNull(PhoneInputUtils.normalizeForStorage("\uFF0B"))
        assertNull(PhoneInputUtils.normalizeForStorage("+() - ."))
        assertNull(PhoneInputUtils.normalizeForStorage("abc\u200E\uD83D\uDCF1"))
    }

    @Test
    fun sanitizeInput_enforcesMaxDigitsAndMaxSeparatorLength() {
        // Stops accepting extra digits and trailing separators once 15 digits are reached
        assertEquals("123456789012345", PhoneInputUtils.sanitizeInput("12345678901234567890"))
        assertEquals("+123456789012345", PhoneInputUtils.sanitizeInput("+123456789012345-678 "))
        assertEquals("+1 234 567 890 123 45", PhoneInputUtils.sanitizeInput("+1 234 567 890 123 456 789"))

        // Separator spam without digits is capped at MAX_INPUT_LENGTH (24)
        val longDashes = "-".repeat(40)
        assertEquals(PhoneInputUtils.MAX_INPUT_LENGTH, PhoneInputUtils.sanitizeInput(longDashes).length)
    }

    @Test
    fun normalizeForStorage_capsOverlyLongInputAt15Digits() {
        assertEquals("+123456789012345", PhoneInputUtils.normalizeForStorage("+12345678901234567890"))
        assertEquals("123456789012345", PhoneInputUtils.normalizeForStorage("123-456-789-012-345-678"))
    }

    @Test
    fun validateInput_acceptsBlankAnd7To15Digits_rejectsTooShortOrSymbolOnly() {
        // Blank is valid (optional field)
        assertNull(PhoneInputUtils.validateInput(""))
        assertNull(PhoneInputUtils.validateInput("   "))

        // 7 to 15 digits are valid
        assertNull(PhoneInputUtils.validateInput("1234567"))
        assertNull(PhoneInputUtils.validateInput("+63 917-123-4567"))
        assertNull(PhoneInputUtils.validateInput("+123456789012345"))

        // Symbol-only / zero digits is invalid
        assertEquals("Enter a valid phone number", PhoneInputUtils.validateInput("+"))
        assertEquals("Enter a valid phone number", PhoneInputUtils.validateInput("---"))

        // 1 to 6 digits is too short
        assertEquals("Phone number is too short (at least 7 digits)", PhoneInputUtils.validateInput("1"))
        assertEquals("Phone number is too short (at least 7 digits)", PhoneInputUtils.validateInput("123-456"))

        // Over 15 digits (if passed directly unsanitized) is too long
        assertEquals("Phone number is too long (max 15 digits)", PhoneInputUtils.validateInput("1234567890123456"))
    }
}
