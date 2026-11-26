package com.example.hello.ui.components

/**
 * Sanitizes interactive/pasted phone input for display in text fields,
 * validates phone length/content, and normalizes phone numbers into
 * canonical dialable form for storage.
 */
object PhoneInputUtils {

    const val MIN_PHONE_DIGITS = 7
    const val MAX_PHONE_DIGITS = 15
    const val MAX_INPUT_LENGTH = 24

    private val UNICODE_DASHES = setOf(
        '\u2010', // Hyphen
        '\u2011', // Non-breaking hyphen
        '\u2012', // Figure dash
        '\u2013', // En dash
        '\u2014', // Em dash
        '\u2212'  // Minus sign
    )

    /**
     * Sanitizes live or pasted phone input while preserving standard formatting separators
     * (`' '`, `'-'`, `'('`, `')'`, `'.'`) and at most one leading `'+'` before any digit.
     *
     * Enforces [MAX_PHONE_DIGITS] (stopping input as soon as 15 digits are reached, including
     * trailing separators) and [MAX_INPUT_LENGTH] (preventing separator-only spam).
     *
     * Normalizes fullwidth plus (`\uFF0B`), Unicode decimal digits, Unicode dashes, and
     * Unicode whitespace (`Character.isWhitespace` / `\u00A0`), strips leading spaces
     * (at start or immediately after leading `'+'`), collapses consecutive spaces into `' '`,
     * and drops letters, invisible bidi/format characters, emojis, and other symbols.
     */
    fun sanitizeInput(raw: String): String {
        if (raw.isEmpty()) return ""
        val sb = StringBuilder(raw.length.coerceAtMost(MAX_INPUT_LENGTH))
        var hasPlus = false
        var digitCount = 0

        for (ch in raw) {
            if (digitCount >= MAX_PHONE_DIGITS || sb.length >= MAX_INPUT_LENGTH) break
            when {
                ch == '+' || ch == '\uFF0B' -> {
                    if (!hasPlus && digitCount == 0) {
                        sb.append('+')
                        hasPlus = true
                    }
                }
                ch == '-' || ch in UNICODE_DASHES -> {
                    sb.append('-')
                }
                ch == '(' || ch == ')' || ch == '.' -> {
                    sb.append(ch)
                }
                Character.isWhitespace(ch) || ch == '\u00A0' -> {
                    if (sb.isNotEmpty() && sb.last() != '+' && sb.last() != ' ') {
                        sb.append(' ')
                    }
                }
                else -> {
                    val asciiDigit = toAsciiDigitOrNull(ch)
                    if (asciiDigit != null) {
                        sb.append(asciiDigit)
                        digitCount++
                    }
                }
            }
        }

        return sb.toString()
    }

    /**
     * Validates a phone input string on save/submit.
     * Blank input is always valid (phone is optional). Non-blank input must contain
     * between [MIN_PHONE_DIGITS] and [MAX_PHONE_DIGITS] digits.
     *
     * Returns a user-facing error message if invalid, or `null` if valid.
     */
    fun validateInput(raw: String): String? {
        if (raw.isBlank()) return null
        var digitCount = 0
        for (ch in raw) {
            if (toAsciiDigitOrNull(ch) != null) {
                digitCount++
            }
        }
        return when {
            digitCount == 0 -> "Enter a valid phone number"
            digitCount < MIN_PHONE_DIGITS -> "Phone number is too short (at least $MIN_PHONE_DIGITS digits)"
            digitCount > MAX_PHONE_DIGITS -> "Phone number is too long (max $MAX_PHONE_DIGITS digits)"
            else -> null
        }
    }

    /**
     * Returns canonical dialable form for storage: an optional single leading `'+'`
     * (when `'+'` or `'\uFF0B'` appeared before the first digit) followed strictly by
     * up to [MAX_PHONE_DIGITS] ASCII digits `'0'..'9'`.
     *
     * Returns `null` if [raw] is `null` or contains zero digits.
     */
    fun normalizeForStorage(raw: String?): String? {
        if (raw.isNullOrEmpty()) return null
        val digits = StringBuilder(raw.length.coerceAtMost(MAX_PHONE_DIGITS))
        var hasLeadingPlus = false

        for (ch in raw) {
            if (digits.length >= MAX_PHONE_DIGITS) break
            if ((ch == '+' || ch == '\uFF0B') && digits.isEmpty()) {
                hasLeadingPlus = true
            } else {
                val asciiDigit = toAsciiDigitOrNull(ch)
                if (asciiDigit != null) {
                    digits.append(asciiDigit)
                }
            }
        }

        if (digits.isEmpty()) return null
        return if (hasLeadingPlus) "+$digits" else digits.toString()
    }

    private fun toAsciiDigitOrNull(ch: Char): Char? {
        if (!Character.isDigit(ch)) return null
        val digit = Character.digit(ch, 10)
        return if (digit in 0..9) '0' + digit else null
    }
}
