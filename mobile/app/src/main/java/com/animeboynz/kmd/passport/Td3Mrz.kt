package com.animeboynz.kmd.passport

/** Two-line TD3 (passport) MRZ, 44 characters each. */
data class Td3Mrz(
    val line1: String,
    val line2: String,
) {
    init {
        require(line1.length == MRZ_LINE_LENGTH) { "Line 1 must be $MRZ_LINE_LENGTH chars" }
        require(line2.length == MRZ_LINE_LENGTH) { "Line 2 must be $MRZ_LINE_LENGTH chars" }
    }

    val combined: String get() = line1 + line2

    companion object {
        const val MRZ_LINE_LENGTH = 44
    }
}
