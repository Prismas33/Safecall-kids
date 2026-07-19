package com.safecallkids.app.billing

import android.app.Activity
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.safecallkids.app.monetization.PremiumAccessManager

class PlayBillingManager(
    private val activity: Activity,
    private val callbacks: Callbacks
) : PurchasesUpdatedListener {

    private var premiumOffer: PremiumOffer? = null

    private val billingClient: BillingClient = BillingClient.newBuilder(activity.applicationContext)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build()
        )
        .enableAutoServiceReconnection()
        .build()

    fun start() {
        if (billingClient.isReady) {
            queryPremiumProduct()
            syncExistingPurchases()
            return
        }

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryPremiumProduct()
                    syncExistingPurchases()
                } else {
                    callbacks.onBillingError(billingResult.debugMessage)
                }
            }

            override fun onBillingServiceDisconnected() {
                callbacks.onBillingError("Google Play Billing service disconnected")
            }
        })
    }

    fun launchPremiumPurchase() {
        val offer = premiumOffer
        if (offer == null) {
            callbacks.onBillingError("Premium product is not available from Google Play")
            start()
            return
        }

        val productDetailsParamsBuilder = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(offer.productDetails)

        if (!offer.offerToken.isNullOrBlank()) {
            productDetailsParamsBuilder.setOfferToken(offer.offerToken)
        }

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParamsBuilder.build()))
            .build()

        val billingResult = billingClient.launchBillingFlow(activity, billingFlowParams)
        if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
            callbacks.onBillingError(billingResult.debugMessage)
        }
    }

    fun syncExistingPurchases() {
        if (!billingClient.isReady) {
            start()
            return
        }

        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val foundPremium = handlePremiumPurchases(purchases)
                if (!foundPremium) {
                    callbacks.onNoActivePremiumPurchase()
                }
            } else {
                callbacks.onBillingError(billingResult.debugMessage)
            }
        }
    }

    fun endConnection() {
        billingClient.endConnection()
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> handlePremiumPurchases(purchases.orEmpty())
            BillingClient.BillingResponseCode.USER_CANCELED -> callbacks.onPurchaseCancelled()
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> syncExistingPurchases()
            else -> callbacks.onBillingError(billingResult.debugMessage)
        }
    }

    private fun queryPremiumProduct() {
        val product = QueryProductDetailsParams.Product.newBuilder()
            .setProductId(PremiumAccessManager.PREMIUM_PRODUCT_ID)
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(listOf(product))
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsResult ->
            if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                callbacks.onBillingError(billingResult.debugMessage)
                return@queryProductDetailsAsync
            }

            val details = productDetailsResult.productDetailsList
                .firstOrNull { it.productId == PremiumAccessManager.PREMIUM_PRODUCT_ID }

            if (details == null) {
                callbacks.onBillingError("premium_lifetime was not returned by Google Play")
                return@queryProductDetailsAsync
            }

            val offerDetails = details.oneTimePurchaseOfferDetailsList?.firstOrNull()
                ?: details.oneTimePurchaseOfferDetails

            if (offerDetails == null) {
                callbacks.onBillingError("premium_lifetime has no available purchase offer")
                return@queryProductDetailsAsync
            }

            premiumOffer = PremiumOffer(
                productDetails = details,
                offerToken = offerDetails.offerToken,
                formattedPrice = offerDetails.formattedPrice
            )
            callbacks.onProductLoaded(offerDetails.formattedPrice)
        }
    }

    private fun handlePremiumPurchases(purchases: List<Purchase>): Boolean {
        var foundPremium = false

        purchases
            .filter { purchase -> purchase.products.contains(PremiumAccessManager.PREMIUM_PRODUCT_ID) }
            .forEach { purchase ->
                foundPremium = true
                when (purchase.purchaseState) {
                    Purchase.PurchaseState.PURCHASED -> acknowledgePremiumPurchase(purchase)
                    Purchase.PurchaseState.PENDING -> callbacks.onPurchasePending()
                    else -> Unit
                }
            }

        return foundPremium
    }

    private fun acknowledgePremiumPurchase(purchase: Purchase) {
        if (purchase.isAcknowledged) {
            callbacks.onPurchaseCompleted(purchase.purchaseToken, purchase.orderId)
            return
        }

        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        billingClient.acknowledgePurchase(params) { billingResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                callbacks.onPurchaseCompleted(purchase.purchaseToken, purchase.orderId)
            } else {
                callbacks.onBillingError(billingResult.debugMessage)
            }
        }
    }

    interface Callbacks {
        fun onProductLoaded(formattedPrice: String)
        fun onPurchaseCompleted(purchaseToken: String, orderId: String?)
        fun onPurchasePending()
        fun onPurchaseCancelled()
        fun onNoActivePremiumPurchase()
        fun onBillingError(message: String)
    }

    private data class PremiumOffer(
        val productDetails: ProductDetails,
        val offerToken: String?,
        val formattedPrice: String
    )
}
