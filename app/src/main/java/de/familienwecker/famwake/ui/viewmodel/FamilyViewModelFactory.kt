package de.familienwecker.famwake.ui.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import de.familienwecker.famwake.FamWakeApplication

class FamilyViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FamilyViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            val app = application as FamWakeApplication
            @Suppress("UNCHECKED_CAST")
            return FamilyViewModel(
                application,
                app.firebaseRepository,
                app.appSettings,
                app.memberRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
