package com.animeboynz.kmd.passport

import org.jmrtd.lds.icao.MRZInfo

/**
 * Parses TD3 MRZ from noisy OCR text and validates ICAO check digits on line 2.
 */
object MrzParser {

    private const val LINE_LEN = Td3Mrz.MRZ_LINE_LENGTH

    private val mrzCharRegex = Regex("^[A-Z0-9<]{44}$")

    /** Map OCR confusions toward MRZ alphabet. */
    fun normalizeMrzLine(raw: String): String {
        return raw.uppercase()
            .asSequence()
            .filter { it.isLetterOrDigit() || it == '<' }
            .joinToString("")
            .padEnd(LINE_LEN, '<')
            .take(LINE_LEN)
    }

    fun parseMrzInfo(mrz: Td3Mrz): MRZInfo = MRZInfo(mrz.combined)

    /**
     * Extracts a TD3 MRZ from flat OCR text by scanning for two plausible 44-char lines.
     */
    fun extractTd3FromOcr(ocrText: String): Td3Mrz? {
        val cleaned = ocrText.uppercase()
            .replace("\n", "")
            .replace("\r", "")
            .replace(" ", "")

        val line1Starts = mutableListOf<Pair<Int, String>>()
        var i = 0
        while (i + LINE_LEN <= cleaned.length) {
            val slice = cleaned.substring(i, i + LINE_LEN)
            if (slice.matches(mrzCharRegex) && looksLikeTd3Line1(slice)) {
                line1Starts.add(i to slice)
            }
            i++
        }

        for ((start, line1) in line1Starts) {
            val restStart = start + LINE_LEN
            if (restStart + LINE_LEN > cleaned.length) continue
            val line2 = cleaned.substring(restStart, restStart + LINE_LEN)
            if (!line2.matches(mrzCharRegex)) continue
            if (!line2Checks(line2)) continue
            val mrz = Td3Mrz(line1 = line1, line2 = line2)
            if (runCatching { MRZInfo(mrz.combined) }.isSuccess) {
                return mrz
            }
        }

        val lines = ocrText.lines()
            .map { normalizeMrzLine(it) }
            .filter { it.length == LINE_LEN && it.matches(mrzCharRegex) }

        for (a in lines.indices) {
            for (b in a + 1 until lines.size) {
                val line1 = lines[a]
                val line2 = lines[b]
                if (!looksLikeTd3Line1(line1)) continue
                if (!line2Checks(line2)) continue
                val mrz = Td3Mrz(line1, line2)
                if (runCatching { MRZInfo(mrz.combined) }.isSuccess) return mrz
            }
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
        if (!looksLikeTd3Line1(line1)) {
            return Result.failure(IllegalArgumentException("Line 1 does not look like a passport MRZ"))
        }
        if (!line2Checks(line2)) {
            return Result.failure(IllegalArgumentException("MRZ check digits failed — fix OCR errors or re-scan"))
        }
        return runCatching { Td3Mrz(line1, line2).also { MRZInfo(it.combined) } }
    }

    private fun looksLikeTd3Line1(line: String): Boolean {
        if (line.length != LINE_LEN) return false
        if (line[0] !in "PIVAC") return false
        return true
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
        // TD3 line 2 (ICAO / JMRTD): indices 28–41 = 14-char personal/optional field; [42] = its check
        // digit; [43] = composite check digit over doc+dob+expiry blocks + 14-char personal (no personal check).
        val personal = line.substring(28, 42)
        val personalCd = normalizeMrzCheckDigit(line[42])
        if (computeCheckDigit(personal) != personalCd) return false
        val composite =
            line.substring(0, 10) + line.substring(13, 20) + line.substring(21, 28) + personal
        val compCd = normalizeMrzCheckDigit(line[43])
        if (computeCheckDigit(composite) != compCd) return false
        return true
    }

    /** MRZ may print check digit 0 as '<' in some positions. */
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
}
