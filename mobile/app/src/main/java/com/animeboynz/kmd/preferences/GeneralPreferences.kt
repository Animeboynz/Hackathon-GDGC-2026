package com.animeboynz.kmd.preferences

import com.animeboynz.kmd.preferences.preference.Preference
import com.animeboynz.kmd.preferences.preference.PreferenceStore

class GeneralPreferences(preferenceStore: PreferenceStore) {
    val storeName = preferenceStore.getString("store-name", "Kathmandu")
    val storeNumber = preferenceStore.getString("store-number", "000")
    val countryCode = preferenceStore.getString("country-code", "NZ")
    val orderNumberPadding = preferenceStore.getInt("order-number-padding", 4)
    val lastUsedOrderCategory = preferenceStore.getInt("last_used_order_category", 0)
    val stockCheckRegion = preferenceStore.getString("stock_check_region", "1010")
    val productsListImported = preferenceStore.getBoolean("products_list_imported", false)
    val passportOnboardingCompleted = preferenceStore.getBoolean("passport_onboarding_completed", false)
    val digitalIdGenerated = preferenceStore.getBoolean("digital_id_generated", false)
    val digitalIdHolderName = preferenceStore.getString("digital_id_holder_name", "N.Z. Traveller")
    val digitalIdDocumentNumber = preferenceStore.getString("digital_id_document_number", "EID-4729")
    val digitalIdExpiry = preferenceStore.getString("digital_id_expiry", "2030-01-01")
    val digitalIdCredentialId = preferenceStore.getString("digital_id_credential_id", "EID-4729")
}
