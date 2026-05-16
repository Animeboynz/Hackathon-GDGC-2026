package com.animeboynz.kmd.passport

import org.jmrtd.lds.icao.MRZInfo

/**
 * Parses TD3 MRZ text (from Dynamsoft, manual entry, or flat OCR strings) and validates ICAO check digits on line 2.
 */
object MrzParser {

    private const val LINE_LEN = Td3Mrz.MRZ_LINE_LENGTH

    private val mrzCharRegex = Regex("^[A-Z0-9<]{44}$")

    private val td3Line1Regex = Regex(
        "^[PIVAC][A-Z0-9<][A-Z<]{3}[A-Z0-9<]{39}$",
    )

    private val td3Line2Regex = Regex(
        "^[A-Z0-9<]{9}[0-9<][A-Z<]{3}[0-9]{6}[0-9<][FM<][0-9]{6}[0-9<][A-Z0-9<]{14}[0-9<][0-9<]$",
    )

    fun normalizeMrzLine(raw: String): String {
        return raw.uppercase()
            .asSequence()
            .filter { it.isLetterOrDigit() || it == '<' }
            .joinToString("")
            .padEnd(LINE_LEN, '<')
            .take(LINE_LEN)
    }

    fun parseMrzInfo(mrz: Td3Mrz): MRZInfo = MRZInfo(mrz.combined)

    fun extractTd3FromOcr(ocrText: String): Td3Mrz? {
        val cleaned = stripToMrzAlphabet(
            ocrText.uppercase()
                .replace("\n", "")
                .replace("\r", "")
                .replace(" ", ""),
        )
        val tail = cleaned.takeLast(minOf(cleaned.length, 900))
        extractFromMergedStrip(tail)?.let { return it }

        for (line in ocrText.lines()) {
            extractFromMergedStrip(stripToMrzAlphabet(line))?.let { return it }
        }

        return null
    }

    fun parseManualLines(line1Raw: String, line2Raw: String): Result<Td3Mrz> {
        val line1 = normalizeMrzLine(line1Raw)
        val line2 = normalizeMrzLine(line2Raw)
        if (!line1.matches(mrzCharRegex)) {
            return Result.failure(IllegalArgumentException("Line 1 must be exactly $LINE_LEN MRZ characters"))
        }
        if (!line2.matches(mrzCharRegex)) {
            return Result.failure(IllegalArgumentException("Line 2 must be exactly $LINE_LEN MRZ characters"))
        }
        val l1 = applyOcrCorrectionsLine1(line1)
        val l2 = applyOcrCorrectionsLine2(line2)
        if (!looksLikeTd3Line1Strict(l1)) {
            return Result.failure(IllegalArgumentException("Line 1 does not look like a passport MRZ"))
        }
        if (!line2Checks(l2)) {
            return Result.failure(IllegalArgumentException("MRZ check digits failed — fix OCR errors or re-scan"))
        }
        return runCatching { Td3Mrz(l1, l2).also { MRZInfo(it.combined) } }
    }

    private fun stripToMrzAlphabet(s: String): String {
        return s.uppercase()
            .asSequence()
            .filter { it.isLetterOrDigit() || it == '<' }
            .joinToString("")
    }

    private fun extractFromMergedStrip(strip: String): Td3Mrz? {
        if (strip.length < LINE_LEN * 2) return null
        for (i in 0..strip.length - LINE_LEN * 2) {
            val raw1 = strip.substring(i, i + LINE_LEN)
            val raw2 = strip.substring(i + LINE_LEN, i + LINE_LEN * 2)
            if (!raw1.matches(mrzCharRegex) || !raw2.matches(mrzCharRegex)) continue
            val l1 = applyOcrCorrectionsLine1(raw1)
            val l2 = applyOcrCorrectionsLine2(raw2)
            if (!td3Line1Regex.matches(l1)) continue
            if (!td3Line2Regex.matches(l2)) continue
            if (!looksLikeTd3Line1Strict(l1)) continue
            if (!line2Checks(l2)) continue
            if (validatePair(l1, l2)) return Td3Mrz(l1, l2)
        }
        return null
    }

    private fun validatePair(line1: String, line2: String): Boolean {
        return runCatching {
            MRZInfo(line1 + line2)
            true
        }.getOrDefault(false)
    }

    private fun looksLikeTd3Line1Strict(line: String): Boolean {
        if (line.length != LINE_LEN) return false
        if (line[0] !in "PIVAC") return false
        return td3Line1Regex.matches(line)
    }

    private fun line2Checks(line: String): Boolean {
        if (line.length != LINE_LEN) return false
        val doc = line.substring(0, 9)
        val docCd = normalizeMrzCheckDigit(line[9])
        if (computeCheckDigit(doc) != docCd) return false
        val dob = line.substring(13, 19)
        val dobCd = normalizeMrzCheckDigit(line[19])
        if (computeCheckDigit(dob) != dobCd) return false
        val exp = line.substring(21, 27)
        val expCd = normalizeMrzCheckDigit(line[27])
        if (computeCheckDigit(exp) != expCd) return false
        val personal = line.substring(28, 42)
        val personalCd = normalizeMrzCheckDigit(line[42])
        if (computeCheckDigit(personal) != personalCd) return false
        val composite =
            line.substring(0, 10) + line.substring(13, 20) + line.substring(21, 28) + personal
        val compCd = normalizeMrzCheckDigit(line[43])
        if (computeCheckDigit(composite) != compCd) return false
        return true
    }

    private fun normalizeMrzCheckDigit(c: Char): Char = if (c == '<') '0' else c

    private fun mrzCharValue(c: Char): Int = when (c) {
        in '0'..'9' -> c - '0'
        in 'A'..'Z' -> c - 'A' + 10
        '<' -> 0
        else -> 0
    }

    private fun computeCheckDigit(data: String): Char {
        val weights = intArrayOf(7, 3, 1)
        var sum = 0
        data.forEachIndexed { idx, ch ->
            sum += mrzCharValue(ch) * weights[idx % 3]
        }
        return ('0'.code + (sum % 10)).toChar()
    }

    private fun applyOcrCorrectionsLine1(line: String): String {
        if (line.length != LINE_LEN) return line
        val group1 = line.substring(0, 2)
        var group2 = line.substring(2, 5)
        val group3 = line.substring(5, LINE_LEN)
        group2 = replaceDigitsWithLettersForState(group2)
        return group1 + group2 + group3
    }

    private fun applyOcrCorrectionsLine2(line: String): String {
        if (line.length != LINE_LEN) return line
        val group1 = line.substring(0, 9)
        var g2 = line.substring(9, 10)
        var g3 = line.substring(10, 13)
        var g4 = line.substring(13, 19)
        var g5 = line.substring(19, 20)
        val g6 = line.substring(20, 21)
        var g7 = line.substring(21, 27)
        var g8 = line.substring(27, 28)
        val g9 = line.substring(28, 42)
        var g10 = line.substring(42, 43)
        var g11 = line.substring(43, 44)
        g2 = replaceLettersWithDigitsForChecks(g2)
        g3 = replaceDigitsWithLettersForState(g3)
        g4 = replaceLettersWithDigitsForChecks(g4)
        g5 = replaceLettersWithDigitsForChecks(g5)
        g7 = replaceLettersWithDigitsForChecks(g7)
        g8 = replaceLettersWithDigitsForChecks(g8)
        g10 = replaceLettersWithDigitsForChecks(g10)
        g11 = replaceLettersWithDigitsForChecks(g11)
        return group1 + g2 + g3 + g4 + g5 + g6 + g7 + g8 + g9 + g10 + g11
    }

    private fun replaceDigitsWithLettersForState(str: String): String {
        return str
            .replace('0', 'O')
            .replace('1', 'I')
            .replace('2', 'Z')
            .replace('5', 'S')
    }

    private fun replaceLettersWithDigitsForChecks(str: String): String {
        return str
            .replace('O', '0')
            .replace('I', '1')
            .replace('Z', '2')
            .replace('S', '5')
    }
}
