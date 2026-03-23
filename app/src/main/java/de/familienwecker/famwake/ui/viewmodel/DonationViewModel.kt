package de.familienwecker.famwake.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.revenuecat.purchases.*
import com.revenuecat.purchases.interfaces.PurchaseCallback
import com.revenuecat.purchases.interfaces.ReceiveOfferingsCallback
import com.revenuecat.purchases.models.StoreTransaction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import de.familienwecker.famwake.ui.util.UiText
import de.familienwecker.famwake.R

sealed class PurchaseState {
    object Idle : PurchaseState()
    object Loading : PurchaseState()
    object Success : PurchaseState()
    data class Error(val uiText: UiText) : PurchaseState()
}

class DonationViewModel : ViewModel() {
    private val _offerings = MutableStateFlow<Offerings?>(null)
    val offerings: StateFlow<Offerings?> = _offerings

    private val _purchaseState = MutableStateFlow<PurchaseState>(PurchaseState.Idle)
    val purchaseState: StateFlow<PurchaseState> = _purchaseState

    init {
        android.util.Log.d("FamWakeDonation", "DonationViewModel init")
        try {
            fetchOfferings()
        } catch (e: Exception) {
            android.util.Log.e("FamWakeDonation", "Exception in init: ${e.message}")
            _purchaseState.value = PurchaseState.Error(UiText.DynamicString("RevenueCat not ready"))
        }
    }

    fun fetchOfferings() {
        android.util.Log.d("FamWakeDonation", "Fetching offerings from RevenueCat...")
        try {
            Purchases.sharedInstance.getOfferings(object : ReceiveOfferingsCallback {
                override fun onReceived(offerings: Offerings) {
                    android.util.Log.d("FamWakeDonation", "Offerings received: ${offerings.all.keys}")
                    _offerings.value = offerings
                    if (offerings.current == null) {
                        android.util.Log.w("FamWakeDonation", "Warning: No 'current' offering set in RevenueCat dashboard!")
                    }
                }
                override fun onError(error: PurchasesError) {
                    android.util.Log.e("FamWakeDonation", "Error fetching offerings: ${error.message} (Underlying: ${error.underlyingErrorMessage})")
                    val errorMsg = "${error.message} ${error.underlyingErrorMessage ?: ""}"
                    _purchaseState.value = PurchaseState.Error(UiText.DynamicString(errorMsg))
                }
            })
        } catch (e: Exception) {
            android.util.Log.e("FamWakeDonation", "Purchases.sharedInstance access failed: ${e.message}")
            _purchaseState.value = PurchaseState.Error(UiText.DynamicString("RevenueCat not configured"))
        }
    }

    fun purchasePackage(activity: android.app.Activity, packageToPurchase: Package) {
        _purchaseState.value = PurchaseState.Loading
        Purchases.sharedInstance.purchasePackage(
            activity,
            packageToPurchase,
            object : PurchaseCallback {
                override fun onCompleted(storeTransaction: StoreTransaction, customerInfo: CustomerInfo) {
                    _purchaseState.value = PurchaseState.Success
                }
                override fun onError(error: PurchasesError, userCancelled: Boolean) {
                    if (!userCancelled) {
                        val uiText = when (error.code) {
                            PurchasesErrorCode.PurchaseNotAllowedError -> 
                                UiText.StringResource(R.string.settings_donate_error_not_allowed)
                            PurchasesErrorCode.StoreProblemError -> 
                                UiText.StringResource(R.string.settings_donate_error_store_problem)
                            PurchasesErrorCode.ProductAlreadyPurchasedError -> 
                                UiText.StringResource(R.string.settings_donate_error_already_purchased)
                            PurchasesErrorCode.PurchaseInvalidError -> 
                                UiText.StringResource(R.string.settings_donate_error_invalid)
                            else -> 
                                UiText.StringResource(R.string.settings_donate_error_generic)
                        }
                        _purchaseState.value = PurchaseState.Error(uiText)
                    } else {
                        _purchaseState.value = PurchaseState.Idle
                    }
                }
            }
        )
    }

    fun resetState() {
        _purchaseState.value = PurchaseState.Idle
    }
}
