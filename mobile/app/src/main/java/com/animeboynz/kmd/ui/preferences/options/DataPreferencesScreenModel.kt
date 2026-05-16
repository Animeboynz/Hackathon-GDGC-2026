package com.animeboynz.kmd.ui.preferences.options

import android.util.Log
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.animeboynz.kmd.domain.EmployeeRepository
import com.animeboynz.kmd.preferences.GeneralPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DataPreferencesScreenModel(
    private val employeeRepository: EmployeeRepository,
) : ScreenModel {

    fun deleteAllEmployees() {
        screenModelScope.launch(Dispatchers.IO) {
            employeeRepository.deleteAllEmployees()
        }
    }

}
