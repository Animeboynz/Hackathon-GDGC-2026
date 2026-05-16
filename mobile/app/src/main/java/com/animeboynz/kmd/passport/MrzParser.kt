package com.animeboynz.kmd.passport

import android.graphics.Rect
import com.google.mlkit.vision.text.Text
import org.jmrtd.lds.icao.MRZInfo

/**
 * Parses TD3 MRZ from noisy OCR text and validates ICAO check digits on line 2.
 *
 * Extraction prefers ML Kit line geometry + strict TD3 patterns (similar idea to
 * [AndroidPassportReader](https://github.com/jllarraz/AndroidPassportReader)) instead of sliding
 * a window over the entire flattened OCR string, which picks up random text from the visa page.
 */
object MrzParser {

    private const val LINE_LEN = Td3Mrz.MRZ_LINE_LENGTH

    private val mrzCharRegex = Regex("^[A-Z0-9<]{44}$")

    /** TD3 line 1: document code (2) + issuer (3) + name field (39). */
    private val td3Line1Regex = Regex(
        "^[PIVAC][A-Z0-9<][A-Z<]{3}[A-Z0-9<]{39}$",
    )

    /**
     * TD3 line 2 layout (44 chars), allowing `<` as printed substitute for check digit 0.
     * Mirrors the strict pattern used in AndroidPassportReader's [MRZUtil].
     */
    private val td3Line2Regex = Regex(
        "^[A-Z0-9<]{9}[0-9<][A-Z<]{3}[0-9]{6}[0-9<][FM<][0-9]{6}[0-9<][A-Z0-9<]{14}[0-9<][0-9<]$",
    )

    private data class OcrLine(val raw: String, val top: Int, val left: Int)

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
     * Best entry point: uses ML Kit [Text] lines in reading order (top-to-bottom) and only joins
     * neighbouring lines, so unrelated passport text rarely forms a false 88-character MRZ.
     */
    fun extractTd3FromMlKit(text: Text): Td3Mrz? {
        val lines = collectSortedLines(text)
        if (lines.isEmpty()) return null

        // 1) Each line alone (sliding window up to 44 inside long noisy strings)
        for (line in lines) {
            extractFromSingleStrip(stripToMrzAlphabet(line.raw))?.let { return it }
        }

        // 2) Join 2–3 consecutive lines (MRZ sometimes split across baselines)
        for (i in lines.indices) {
            for (span in 2..3) {
                val end = i + span - 1
                if (end > lines.lastIndex) break
                if (!areLinesVisuallyAdjacent(lines, i, end)) continue
                val merged = lines.subList(i, end + 1).joinToString("") { stripToMrzAlphabet(it.raw) }
                extractFromMergedStrip(merged)?.let { return it }
            }
        }

        // 3) Text from blocks biased toward bottom of the frame (MRZ strip)
        val bottomSnippet = buildBottomWeightedSnippet(text)
        extractFromMergedStrip(bottomSnippet)?.let { return it }

        return null
    }

    /**
     * Legacy flat OCR: only scans the tail of the string (MRZ is usually last in reading order)
     * plus per-line attempts, to avoid matching random 44-char slices from the name / headers.
     */
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

    private fun collectSortedLines(text: Text): List<OcrLine> {
        val out = ArrayList<OcrLine>(32)
        for (block in text.textBlocks) {
            for (line in block.lines) {
                val rect: Rect = line.boundingBox ?: continue
                val t = line.text?.trim().orEmpty()
                if (t.isNotEmpty()) {
                    out.add(OcrLine(t, rect.top, rect.left))
                }
            }
        }
        out.sortWith(compareBy({ it.top }, { it.left }))
        return out
    }

    private fun areLinesVisuallyAdjacent(lines: List<OcrLine>, from: Int, to: Int): Boolean {
        if (from >= to) return true
        var maxH = 1
        for (i in from..to) {
            maxH = maxOf(maxH, estimateLineHeight(lines, i))
        }
        val gapLimit = (maxH * 2.2f).toInt().coerceAtLeast(28)
        for (i in from until to) {
            if (lines[i + 1].top - lines[i].top > gapLimit) return false
        }
        return true
    }

    private fun estimateLineHeight(lines: List<OcrLine>, index: Int): Int {
        val cur = lines.getOrNull(index) ?: return 20
        val next = lines.getOrNull(index + 1)
        val prev = lines.getOrNull(index - 1)
        val d1 = next?.let { (it.top - cur.top).coerceAtLeast(12) }
        val d2 = prev?.let { (cur.top - it.top).coerceAtLeast(12) }
        return listOfNotNull(d1, d2).minOrNull() ?: 24
    }

    private fun buildBottomWeightedSnippet(text: Text, maxChars: Int = 320): String {
        data class BlockSnippet(val top: Int, val text: String)
        val blocks = ArrayList<BlockSnippet>(text.textBlocks.size)
        for (block in text.textBlocks) {
            val box = block.boundingBox ?: continue
            val t = block.text?.let { stripToMrzAlphabet(it) }.orEmpty()
            if (t.isNotEmpty()) blocks.add(BlockSnippet(box.top, t))
        }
        if (blocks.isEmpty()) return ""
        val maxTop = blocks.maxOf { it.top }
        blocks.sortByDescending { it.top }
        val sb = StringBuilder()
        for (b in blocks) {
            if (b.top < maxTop - maxTop / 3) continue
            sb.append(b.text)
            if (sb.length >= maxChars) break
        }
        return sb.toString().take(maxChars)
    }

    private fun stripToMrzAlphabet(s: String): String {
        return s.uppercase()
            .asSequence()
            .filter { it.isLetterOrDigit() || it == '<' }
            .joinToString("")
    }

    /** Contiguous TD3 (88 chars) inside one OCR line, or line1 immediately followed by line2. */
    private fun extractFromSingleStrip(strip: String): Td3Mrz? {
        extractFromMergedStrip(strip)?.let { return it }
        return null
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

    /** Issuing state (chars 2–4) is alpha; OCR often reads O as 0 etc. */
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
