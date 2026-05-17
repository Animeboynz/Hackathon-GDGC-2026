package com.animeboynz.kmd.ui.onboarding

import android.graphics.Bitmap
import android.nfc.Tag
import android.util.Base64
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.animeboynz.kmd.crypto.VerIdCredentialCrypto
import com.animeboynz.kmd.network.SupabaseUsersApi
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
import okhttp3.OkHttpClient
import java.io.ByteArrayOutputStream

class PassportOnboardingScreenModel(
    private val generalPreferences: GeneralPreferences,
    private val okHttpClient: OkHttpClient,
) : ScreenModel {

    enum class DocumentEvidence(val label: String, val reliability: Long) {
        Passport("Passport", 90L),
        DriversLicence("Driver's Licence", 75L),
        BirthCertificate("Birth Certificate", 65L),
        BankStatement("Bank Statement", 45L),
        MailedLetter("Mailed Letter", 30L),
    }

    sealed class State {
        data object Welcome : State()

        data class DocumentOnboarding(
            val name: String = "",
            val dateOfBirth: String = "",
            val submittedDocuments: Set<DocumentEvidence> = emptySet(),
            val selfieBase64: String = "",
            val lastDocumentAction: String? = null,
            val error: String? = null,
        ) : State()

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

    fun canGoBack(): Boolean = _state.value !is State.Welcome

    fun goBack(): Boolean {
        when (val s = _state.value) {
            is State.Welcome -> return false
            is State.DocumentOnboarding -> _state.value = State.Welcome
            is State.ScanPhoto -> _state.value = State.Welcome
            is State.ReviewMrz -> _state.value = State.ScanPhoto
            is State.ManualMrz -> _state.value = State.ScanPhoto
            is State.WaitNfc -> _state.value = State.ReviewMrz(s.mrz)
            is State.ChipRead -> _state.value = State.ScanPhoto
            is State.SelfieCheck -> _state.value = State.ChipRead(s.summary)
            is State.DigitalIdIssue -> _state.value = State.SelfieCheck(s.summary)
            is State.Fatal -> {
                val mrz = lastMrzForNfc
                _state.value = if (mrz != null) State.WaitNfc(mrz) else State.Welcome
            }
        }
        return true
    }

    fun goToScan() {
        _state.value = State.ScanPhoto
    }

    fun openDocumentOnboarding() {
        _state.value = State.DocumentOnboarding()
    }

    fun updateDocumentName(value: String) {
        val s = _state.value as? State.DocumentOnboarding ?: return
        _state.value = s.copy(name = value, error = null)
    }

    fun updateDocumentDateOfBirth(value: String) {
        val s = _state.value as? State.DocumentOnboarding ?: return
        _state.value = s.copy(dateOfBirth = value, error = null)
    }

    fun markDocumentSubmitted(document: DocumentEvidence, source: String) {
        val s = _state.value as? State.DocumentOnboarding ?: return
        _state.value = s.copy(
            submittedDocuments = s.submittedDocuments + document,
            lastDocumentAction = "${document.label} added from $source",
            error = null,
        )
    }

    fun markDocumentSelfieCaptured(bitmap: Bitmap) {
        val s = _state.value as? State.DocumentOnboarding ?: return
        _state.value = s.copy(
            selfieBase64 = bitmap.toJpegBase64(),
            lastDocumentAction = "Profile selfie added",
            error = null,
        )
    }

    fun completeDocumentDigitalId(): Boolean {
        val s = _state.value as? State.DocumentOnboarding ?: return false
        val holderName = s.name.trim().ifBlank {
            _state.value = s.copy(error = "Enter the ID holder's name")
            return false
        }
        val dob = s.dateOfBirth.trim().ifBlank {
            _state.value = s.copy(error = "Enter the ID holder's date of birth")
            return false
        }
        val numericId = generalPreferences.digitalIdNumericId.get().takeIf { it > 0L } ?: generateEidNumber()
        val credentialId = "EID-$numericId"
        val keyPair = VerIdCredentialCrypto.generateKeyPair()
        val reliability = documentReliability(s.submittedDocuments)

        generalPreferences.digitalIdNumericId.set(numericId)
        generalPreferences.digitalIdHolderName.set(holderName)
        generalPreferences.digitalIdDocumentNumber.set(s.submittedDocuments.bestDocumentLabel())
        generalPreferences.digitalIdVerificationSources.set(s.submittedDocuments.documentLabels())
        generalPreferences.digitalIdDateOfBirth.set(dob)
        generalPreferences.digitalIdExpiry.set("Document review")
        generalPreferences.digitalIdCredentialId.set(credentialId)
        generalPreferences.digitalIdPublicKeyBase64.set(keyPair.publicKeyBase64)
        generalPreferences.digitalIdPrivateKeyBase64.set(keyPair.privateKeyBase64)
        generalPreferences.digitalIdReliability.set(reliability)
        generalPreferences.digitalIdPortraitBase64.set(s.selfieBase64)
        generalPreferences.digitalIdGenerated.set(true)
        generalPreferences.passportOnboardingCompleted.set(true)

        screenModelScope.launch {
            SupabaseUsersApi.upsertUser(
                client = okHttpClient,
                id = numericId,
                name = holderName,
                dob = dob,
                reliability = reliability,
                publicKey = keyPair.publicKeyBase64,
            )
        }
        return true
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
        val numericId = generalPreferences.digitalIdNumericId.get().takeIf { it > 0L } ?: generateEidNumber()
        val credentialId = "EID-$numericId"
        val dob = mrz.dateOfBirth.toDisplayDate()
        val reliability = 100L

        generalPreferences.digitalIdNumericId.set(numericId)
        generalPreferences.digitalIdHolderName.set(holderName)
        generalPreferences.digitalIdDocumentNumber.set(documentNumber)
        generalPreferences.digitalIdVerificationSources.set("Passport NFC")
        generalPreferences.digitalIdDateOfBirth.set(dob)
        generalPreferences.digitalIdExpiry.set(mrz.dateOfExpiry.ifBlank { "2030-01-01" })
        generalPreferences.digitalIdCredentialId.set(credentialId)
        val keyPair = VerIdCredentialCrypto.generateKeyPair()
        generalPreferences.digitalIdPublicKeyBase64.set(keyPair.publicKeyBase64)
        generalPreferences.digitalIdPrivateKeyBase64.set(keyPair.privateKeyBase64)
        generalPreferences.digitalIdReliability.set(reliability)
        summary.portrait?.let { portrait ->
            generalPreferences.digitalIdPortraitBase64.set(portrait.toJpegBase64())
        }
        generalPreferences.digitalIdGenerated.set(true)
        generalPreferences.passportOnboardingCompleted.set(true)

        screenModelScope.launch {
            SupabaseUsersApi.upsertUser(
                client = okHttpClient,
                id = numericId,
                name = holderName,
                dob = dob,
                reliability = reliability,
                publicKey = keyPair.publicKeyBase64,
            )
        }
    }

    fun markOnboardingComplete() {
        generalPreferences.passportOnboardingCompleted.set(true)
    }

    private fun Bitmap.toJpegBase64(): String {
        val output = ByteArrayOutputStream()
        compress(Bitmap.CompressFormat.JPEG, 86, output)
        return Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
    }

    private fun String.toDisplayDate(): String {
        if (length != 6) return ifBlank { "Unknown" }
        return "${substring(4, 6)}/${substring(2, 4)}/${substring(0, 2)}"
    }

    private fun generateEidNumber(): Long {
        return System.currentTimeMillis()
    }

    private fun documentReliability(documents: Set<DocumentEvidence>): Long {
        return documents.maxOfOrNull { it.reliability } ?: 10L
    }

    private fun Set<DocumentEvidence>.bestDocumentLabel(): String {
        return maxByOrNull { it.reliability }?.label ?: "Self-declared"
    }

    private fun Set<DocumentEvidence>.documentLabels(): String {
        return sortedByDescending { it.reliability }
            .joinToString(", ") { it.label }
            .ifBlank { "Self-declared" }
    }
}
