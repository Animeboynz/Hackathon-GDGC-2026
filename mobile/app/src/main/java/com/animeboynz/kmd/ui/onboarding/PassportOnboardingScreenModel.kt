package com.animeboynz.kmd.ui.onboarding

import android.nfc.Tag
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.animeboynz.kmd.passport.DynamsoftMrzMapper
import com.animeboynz.kmd.passport.MrzParser
import com.animeboynz.kmd.passport.PassportChipSummary
import com.animeboynz.kmd.passport.PassportNfcReader
import com.animeboynz.kmd.passport.Td3Mrz
import com.animeboynz.kmd.preferences.GeneralPreferences
import com.dynamsoft.mrzscannerbundle.ui.MRZScanResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PassportOnboardingScreenModel(
    private val generalPreferences: GeneralPreferences,
) : ScreenModel {

    sealed class State {
        data object Welcome : State()

        data object ScanPhoto : State()

        data class ReviewMrz(
            val mrz: Td3Mrz,
        ) : State()

        data class ManualMrz(
            val line1: String,
            val line2: String,
            val error: String? = null,
        ) : State()

        data class WaitNfc(
            val mrz: Td3Mrz,
            val isReading: Boolean = false,
        ) : State()

        data class ChipRead(
            val summary: PassportChipSummary,
        ) : State()

        data class SelfieCheck(
            val summary: PassportChipSummary,
        ) : State()

        data class DigitalIdIssue(
            val summary: PassportChipSummary,
        ) : State()

        data class Fatal(
            val message: String,
        ) : State()
    }

    private val _state = MutableStateFlow<State>(State.Welcome)
    val state: StateFlow<State> = _state.asStateFlow()

    private var lastMrzForNfc: Td3Mrz? = null

    fun goToScan() {
        _state.value = State.ScanPhoto
    }

    fun openManualEntry() {
        _state.value = State.ManualMrz(line1 = "", line2 = "")
    }

    fun updateManualLine1(value: String) {
        val s = _state.value
        if (s is State.ManualMrz) {
            _state.value = s.copy(line1 = value, error = null)
        }
    }

    fun updateManualLine2(value: String) {
        val s = _state.value
        if (s is State.ManualMrz) {
            _state.value = s.copy(line2 = value, error = null)
        }
    }

    fun submitManualMrz() {
        val s = _state.value as? State.ManualMrz ?: return
        MrzParser.parseManualLines(s.line1, s.line2).fold(
            onSuccess = { mrz -> _state.value = State.ReviewMrz(mrz) },
            onFailure = { e ->
                _state.value = s.copy(error = e.message ?: "Invalid MRZ")
            },
        )
    }

    fun onDynamsoftMrzResult(result: MRZScanResult?) {
        if (_state.value !is State.ScanPhoto) return
        if (result == null) return
        when (result.resultStatus) {
            MRZScanResult.EnumResultStatus.RS_CANCELED -> return
            MRZScanResult.EnumResultStatus.RS_EXCEPTION -> {
                _state.value = State.ManualMrz(
                    line1 = "",
                    line2 = "",
                    error = result.errorString.ifBlank { "MRZ scan failed" },
                )
            }
            MRZScanResult.EnumResultStatus.RS_FINISHED -> {
                val mrz = DynamsoftMrzMapper.passportTd3OrNull(result)
                if (mrz != null) {
                    _state.value = State.ReviewMrz(mrz)
                } else {
                    val err = DynamsoftMrzMapper.humanReadableFailure(result) ?: "Invalid MRZ"
                    _state.value = State.ManualMrz(line1 = "", line2 = "", error = err)
                }
            }
        }
    }

    fun confirmMrzForNfc(mrz: Td3Mrz) {
        lastMrzForNfc = mrz
        _state.value = State.WaitNfc(mrz)
    }

    fun editMrzAgain(mrz: Td3Mrz) {
        _state.value = State.ManualMrz(line1 = mrz.line1, line2 = mrz.line2)
    }

    fun onNfcTag(tag: Tag) {
        val s = _state.value as? State.WaitNfc ?: return
        if (s.isReading) return
        _state.value = s.copy(isReading = true)
        screenModelScope.launch {
            val result = PassportNfcReader.readDg1(tag, s.mrz)
            result.fold(
                onSuccess = { summary -> _state.value = State.SelfieCheck(summary) },
                onFailure = { e ->
                    _state.value = State.Fatal(e.message ?: "NFC read failed")
                },
            )
        }
    }

    fun retryNfcAfterError() {
        val mrz = lastMrzForNfc ?: return
        _state.value = State.WaitNfc(mrz)
    }

    /** Return to MRZ review from the NFC wait screen (e.g. user needs to adjust positioning). */
    fun cancelWaitNfc() {
        val s = _state.value as? State.WaitNfc ?: return
        _state.value = State.ReviewMrz(s.mrz)
    }

    fun beginSelfieCheck(summary: PassportChipSummary) {
        _state.value = State.SelfieCheck(summary)
    }

    fun backToPassportSummary(summary: PassportChipSummary) {
        _state.value = State.ChipRead(summary)
    }

    fun confirmSelfieMatch(summary: PassportChipSummary) {
        _state.value = State.DigitalIdIssue(summary)
    }

    fun completeDigitalId(summary: PassportChipSummary) {
        val mrz = summary.mrzInfo
        val holderName = listOf(mrz.primaryIdentifier, mrz.secondaryIdentifier)
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .ifBlank { mrz.nameOfHolder }
            .ifBlank { "N.Z. Traveller" }
        val documentNumber = mrz.documentNumber.ifBlank { "UNKNOWN" }
        val credentialId = "EID-${documentNumber.takeLast(4).padStart(4, '0')}"

        generalPreferences.digitalIdHolderName.set(holderName)
        generalPreferences.digitalIdDocumentNumber.set(documentNumber)
        generalPreferences.digitalIdExpiry.set(mrz.dateOfExpiry.ifBlank { "2030-01-01" })
        generalPreferences.digitalIdCredentialId.set(credentialId)
        generalPreferences.digitalIdGenerated.set(true)
        generalPreferences.passportOnboardingCompleted.set(true)
    }

    fun markOnboardingComplete() {
        generalPreferences.passportOnboardingCompleted.set(true)
    }
}
