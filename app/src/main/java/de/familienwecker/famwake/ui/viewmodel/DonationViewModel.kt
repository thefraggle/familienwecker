package de.familienwecker.famwake.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.revenuecat.purchases.*
import com.revenuecat.purchases.interfaces.PurchaseCallback
import com.revenuecat.purchases.interfaces.ReceiveOfferingsCallback
import com.revenuecat.purchases.models.StoreTransaction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

sealed class PurchaseState {
    object Idle : PurchaseState()
    object Loading : PurchaseState()
    object Success : PurchaseState()
    data class Error(val message: String) : PurchaseState()
}

class DonationViewModel : ViewModel() {
    private val _offerings = MutableStateFlow<Offerings?>(null)
    val offerings: StateFlow<Offerings?> = _offerings

    private val _purchaseState = MutableStateFlow<PurchaseState>(PurchaseState.Idle)
    val purchaseState: StateFlow<PurchaseState> = _purchaseState

    init {
        fetchOfferings()
    }

    private fun fetchOfferings() {
        Purchases.sharedInstance.getOfferings(object : ReceiveOfferingsCallback {
            override fun onReceived(offerings: Offerings) {
                _offerings.value = offerings
            }
            override fun onError(error: PurchasesError) {
                // Silently ignore or log error
            }
        })
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
                        _purchaseState.value = PurchaseState.Error(error.message)
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
