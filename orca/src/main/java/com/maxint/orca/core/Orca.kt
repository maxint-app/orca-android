package com.maxint.orca.core

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryProductDetailsResult
import com.android.billingclient.api.QueryPurchasesParams
import okhttp3.Interceptor
import java.lang.ref.WeakReference
import com.maxint.orca.generated.apis.TenantApi
import com.maxint.orca.generated.infrastructure.ApiClient
import com.maxint.orca.generated.models.PlayStorePastPurchase
import com.maxint.orca.generated.models.StorableEntitlement
import com.maxint.orca.generated.models.TenantActiveEntitlementsInputBody
import com.maxint.orca.generated.models.TenantEntitlement
import com.maxint.orca.generated.models.TenantIdentifyUserInputBody
import com.maxint.orca.generated.models.TenantSyncPlayStorePastPurchasesInputBody
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException


object Orca {
    private var isConfigured = false
    private lateinit var configuration: OrcaConfiguration
    private var applicationRef: WeakReference<Application>? = null
    private var currentActivityRef: WeakReference<Activity>? = null

    private val activityLifecycleCallbacks = object : Application.ActivityLifecycleCallbacks {
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
            currentActivityRef = WeakReference(activity)
        }

        override fun onActivityStarted(activity: Activity) {
            currentActivityRef = WeakReference(activity)
        }

        override fun onActivityResumed(activity: Activity) {
            currentActivityRef = WeakReference(activity)
        }

        override fun onActivityPaused(activity: Activity) {}
        override fun onActivityStopped(activity: Activity) {}
        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
        override fun onActivityDestroyed(activity: Activity) {
            currentActivityRef?.let {
                if (it.get() == activity) currentActivityRef = null
            }
        }
    }

    private val purchasesUpdatedListener =
        PurchasesUpdatedListener { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                handlePurchaseUpdates(purchases)
            }
        }
    private lateinit var billingClient: BillingClient
    private lateinit var api: ApiClient
    private lateinit var tenantApi: TenantApi
    private var customerId: String? = null

    private var cachedEntitlements: List<StorableEntitlement>? = null
    private var cachedStoreProduct: List<StoreProduct>? = null
    private var cachedPlatformProduct: List<ProductDetails>? = null
    private var cachedTenantEntitlements: List<TenantEntitlement>? = null

    fun configure(context: Context, config: OrcaConfiguration) {
        configuration = config
        val app = context.applicationContext as Application
        applicationRef = WeakReference(app)
        app.registerActivityLifecycleCallbacks(activityLifecycleCallbacks)

        billingClient = BillingClient.newBuilder(context)
            .setListener(purchasesUpdatedListener)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder()
                    .enablePrepaidPlans()
                    .enableOneTimeProducts()
                    .build()
            )
            .enableAutoServiceReconnection()
            .build()

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                // Connection established or will retry automatically
            }

            override fun onBillingServiceDisconnected() {
                // Will reconnect automatically
            }
        })

        api = ApiClient(baseUrl = configuration.baseUrl)
        api.addAuthorization(
            "api-key",
            Interceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("api-key", configuration.publicKey)
                    .build()
                chain.proceed(request)
            }
        )
        tenantApi = api.createService(TenantApi::class.java)
        isConfigured = true
    }

    private fun ensureConfigured() {
        check(isConfigured) {
            "Orca is not configured. Call Orca.configure(context, config) before using other methods."
        }
    }

    suspend fun identify(customerEmail: String) {
        ensureConfigured()
        val res =
            tenantApi.postTenantIdentify(TenantIdentifyUserInputBody(customerEmail = customerEmail))
        val body = res.body()
        customerId = body?.customerId
        configuration = configuration.copy(customerEmail = customerEmail)
    }

    fun logout() {
        ensureConfigured()
        customerId = null
        configuration = configuration.copy(customerEmail = null)
        cachedEntitlements = null
        cachedStoreProduct = null
        cachedPlatformProduct = null
        cachedTenantEntitlements = null
    }

    suspend fun activeProduct(): List<StoreProduct> {
        ensureConfigured()
        val activeEntitlements = activeEntitlements()
        val allEntitlements = listEntitlements()
        val platformProducts = queryPlatformProducts(allEntitlements)
        return mapPlatformToStoreProducts(platformProducts, allEntitlements)
            .filter { sp -> activeEntitlements.any { it.entitlementId == sp.entitlement.id } }
    }

    suspend fun activeEntitlements(): List<StorableEntitlement> {
        ensureConfigured()
        cachedEntitlements?.let { return it }
        val email = configuration.customerEmail
            ?: throw IllegalStateException("Customer not identified. Call Orca.identify(customerEmail) first.")
        val env = when (configuration.environment) {
            OrcaEnvironment.SANDBOX -> "sandbox"
            OrcaEnvironment.PRODUCTION -> "prod"
        }
        val res = tenantApi.postTenantEntitlementsActive(
            TenantActiveEntitlementsInputBody(
                customerEmail = email,
                environment = env,
            )
        )
        val entitlements = res.body()?.data ?: emptyList()
        cachedEntitlements = entitlements
        return entitlements
    }

    suspend fun listEntitlements(): List<TenantEntitlement> {
        ensureConfigured()
        cachedTenantEntitlements?.let { return it }
        val env = when (configuration.environment) {
            OrcaEnvironment.SANDBOX -> "sandbox"
            OrcaEnvironment.PRODUCTION -> "prod"
        }
        val res = tenantApi.getTenantEntitlementsByEnvironment(env)
        val entitlements = res.body()?.data ?: emptyList()
        cachedTenantEntitlements = entitlements
        return entitlements
    }

    suspend fun queryProducts(): List<StoreProduct> {
        ensureConfigured()
        cachedStoreProduct?.let { return it }
        val entitlements = listEntitlements()
        val platformProducts = queryPlatformProducts(entitlements)
        val storeProducts = mapPlatformToStoreProducts(platformProducts, entitlements)
        cachedStoreProduct = storeProducts
        return storeProducts
    }

    suspend fun purchase(entitlement: TenantEntitlement) {
        ensureConfigured()
        ensureBillingConnected()

        val playProductId = entitlement.products.playstore.productId
        val baseProductId = playProductId.substringBefore(":")
        val basePlanId = playProductId.substringAfter(":", "")

        val allEntitlements = listEntitlements()
        val platformProducts = queryPlatformProducts(allEntitlements)
        val platformProduct = platformProducts.firstOrNull { it.productId == baseProductId }
            ?: throw IllegalStateException("Product not found: $baseProductId")

        if (entitlement.entitlementType == TenantEntitlement.EntitlementType.non_consumable) {
            val active = activeEntitlements()
            val alreadyOwned = active.any { it.entitlementId == entitlement.id }
            if (alreadyOwned) {
                throw IllegalStateException(
                    "User already purchased non-consumable entitlement '${entitlement.name}'"
                )
            }
        }

        val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(platformProduct)

        if (platformProduct.productType == BillingClient.ProductType.SUBS) {
            val offerDetails = if (basePlanId.isNotEmpty()) {
                val offersForBasePlan = platformProduct.subscriptionOfferDetails
                    ?.filter { it.basePlanId == basePlanId }
                    .orEmpty()
                offersForBasePlan.firstOrNull { it.offerId == null } ?: offersForBasePlan.firstOrNull()
            } else {
                val allOffers = platformProduct.subscriptionOfferDetails.orEmpty()
                allOffers.firstOrNull { it.offerId == null } ?: allOffers.firstOrNull()
            }
            if (basePlanId.isNotEmpty() && offerDetails == null) {
                throw IllegalStateException(
                    "Subscription base plan '$basePlanId' not found for product '$baseProductId'."
                )
            }
            val offerToken = offerDetails?.offerToken
            if (offerToken != null) {
                productDetailsParams.setOfferToken(offerToken)
            }
        }

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParams.build()))
            .setObfuscatedAccountId(customerId ?: "")
            .build()

        val activity = currentActivityRef?.get()
            ?: throw IllegalStateException("No activity available for billing flow. Ensure an Activity is in the foreground.")
        billingClient.launchBillingFlow(activity, billingFlowParams)
    }

    private suspend fun queryPlatformProducts(entitlements: List<TenantEntitlement>): List<ProductDetails> {
        cachedPlatformProduct?.let { return it }
        ensureBillingConnected()

        val subscriptionProducts = entitlements
            .filter { it.entitlementType == TenantEntitlement.EntitlementType.subscription }
            .map { it to it.products.playstore.productId.substringBefore(":") }
            .distinctBy { it.second }
            .map { (_, productId) ->
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(productId)
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build()
            }

        val inAppProducts = entitlements
            .filter { it.entitlementType != TenantEntitlement.EntitlementType.subscription }
            .map { it to it.products.playstore.productId }
            .distinctBy { it.second }
            .map { (_, productId) ->
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(productId)
                    .setProductType(BillingClient.ProductType.INAPP)
                    .build()
            }

        val products = mutableListOf<ProductDetails>()

        if (subscriptionProducts.isNotEmpty()) {
            val subsParams = QueryProductDetailsParams.newBuilder()
                .setProductList(subscriptionProducts)
                .build()

            val subsResult = suspendCancellableCoroutine { cont ->
                billingClient.queryProductDetailsAsync(subsParams) { billingResult, queryResult ->
                    if (cont.isActive) {
                        cont.resume(Triple(billingResult, queryResult.productDetailsList, true))
                    }
                }
            }

            if (subsResult.first.responseCode != BillingClient.BillingResponseCode.OK) {
                throw IllegalStateException("Failed to query products: ${subsResult.first.debugMessage}")
            }
            if (subsResult.second.isEmpty()) {
                throw IllegalStateException(
                    "No subscription products returned from Play Billing. " +
                        "Ensure this build is installed from Play Internal/App Sharing and product IDs are correct."
                )
            }

            products.addAll(subsResult.second)
        }

        if (inAppProducts.isNotEmpty()) {
            val inAppParams = QueryProductDetailsParams.newBuilder()
                .setProductList(inAppProducts)
                .build()

            val inAppResult = suspendCancellableCoroutine { cont ->
                billingClient.queryProductDetailsAsync(inAppParams) { billingResult, queryResult ->
                    if (cont.isActive) {
                        cont.resume(Triple(billingResult, queryResult.productDetailsList, true))
                    }
                }
            }

            if (inAppResult.first.responseCode != BillingClient.BillingResponseCode.OK) {
                throw IllegalStateException("Failed to query products: ${inAppResult.first.debugMessage}")
            }
            if (inAppResult.second.isEmpty()) {
                throw IllegalStateException(
                    "No one-time products returned from Play Billing. " +
                        "Ensure this build is installed from Play Internal/App Sharing and product IDs are correct."
                )
            }

            products.addAll(inAppResult.second)
        }

        cachedPlatformProduct = products
        return products
    }

    private suspend fun ensureBillingConnected() {
        if (billingClient.connectionState == BillingClient.ConnectionState.CONNECTED) return
        suspendCancellableCoroutine { cont ->
            billingClient.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        if (cont.isActive) cont.resumeWith(Result.success(Unit))
                    } else {
                        if (cont.isActive) cont.resumeWithException(
                            IllegalStateException("Billing connection failed: ${billingResult.debugMessage}")
                        )
                    }
                }

                override fun onBillingServiceDisconnected() {
                    if (cont.isActive) cont.resumeWithException(
                        IllegalStateException("Billing service disconnected")
                    )
                }
            })
        }
    }

    private suspend fun queryCurrentPurchases(): List<Purchase> {
        ensureBillingConnected()
        val result = suspendCancellableCoroutine { cont ->
            val params = QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
            billingClient.queryPurchasesAsync(params) { billingResult, purchaseList ->
                if (cont.isActive) {
                    cont.resume(billingResult to purchaseList)
                }
            }
        }
        return if (result.first.responseCode == BillingClient.BillingResponseCode.OK) {
            result.second
        } else {
            emptyList()
        }
    }

    private fun handlePurchaseUpdates(purchases: List<Purchase>) {
        val email = configuration.customerEmail ?: return
        val successfulPurchases = purchases.filter {
            it.purchaseState == Purchase.PurchaseState.PURCHASED
        }
        if (successfulPurchases.isEmpty()) return

        CoroutineScope(Dispatchers.IO).launch {
            syncPurchasesWithServer(email, successfulPurchases)
        }

        for (purchase in successfulPurchases) {
            if (!purchase.isAcknowledged) {
                val isConsumable = isConsumablePurchase(purchase)
                if (isConsumable) {
                    consumePurchase(purchase)
                } else {
                    acknowledgePurchase(purchase)
                }
            }
        }

        cachedEntitlements = null
    }

    private suspend fun syncPurchasesWithServer(email: String, purchases: List<Purchase>) {
        val pastPurchases = purchases.map { purchase ->
            PlayStorePastPurchase(
                productId = purchase.products.firstOrNull() ?: "",
                purchaseId = purchase.orderId ?: "",
                purchaseToken = purchase.purchaseToken,
                transactionDate = purchase.purchaseTime.toString(),
            )
        }
        tenantApi.postTenantPlaystoreSyncPurchases(
            TenantSyncPlayStorePastPurchasesInputBody(
                customerEmail = email,
                pastPurchases = pastPurchases,
            )
        )
    }

    private fun isConsumablePurchase(purchase: Purchase): Boolean {
        val entitlementId = purchase.products.firstOrNull() ?: return false
        return cachedTenantEntitlements?.any {
            it.products.playstore.id == entitlementId
                    && it.entitlementType == TenantEntitlement.EntitlementType.consumable
        } ?: false
    }

    private fun consumePurchase(purchase: Purchase) {
        val params = ConsumeParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        billingClient.consumeAsync(params) { _, _ -> }
    }

    private fun acknowledgePurchase(purchase: Purchase) {
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        billingClient.acknowledgePurchase(params) { _ -> }
    }

    private fun mapPlatformToStoreProducts(
        platformProducts: List<ProductDetails>,
        entitlements: List<TenantEntitlement>,
    ): List<StoreProduct> {
        val productById = platformProducts.associateBy { it.productId }

        return entitlements.flatMap { entitlement ->
            val resolvedId = entitlement.products.playstore.productId
            val baseProductId = resolvedId.substringBefore(":")
            val requestedBasePlanId = resolvedId.substringAfter(":", "")
            val platformProduct = productById[baseProductId] ?: return@flatMap emptyList()

            when (entitlement.entitlementType) {
                TenantEntitlement.EntitlementType.subscription -> {
                    val offers = platformProduct.subscriptionOfferDetails.orEmpty()
                    val groupedByBasePlan = offers.groupBy { it.basePlanId }
                    val basePlans = if (requestedBasePlanId.isNotEmpty()) {
                        listOf(requestedBasePlanId)
                    } else {
                        groupedByBasePlan.keys.toList()
                    }

                    basePlans.mapNotNull { basePlanId ->
                        val offersForBasePlan = groupedByBasePlan[basePlanId].orEmpty()
                        val defaultOffer = offersForBasePlan.firstOrNull { it.offerId == null }
                            ?: offersForBasePlan.firstOrNull()
                            ?: return@mapNotNull null

                        val pricingPhase = defaultOffer.pricingPhases.pricingPhaseList.firstOrNull()
                        val offerEntitlement = entitlement.copy(
                            products = entitlement.products.copy(
                                playstore = entitlement.products.playstore.copy(
                                    productId = "${platformProduct.productId}:$basePlanId"
                                )
                            )
                        )
                        StoreProduct.Subscription(
                            id = "${platformProduct.productId}:$basePlanId",
                            name = platformProduct.title,
                            currencyCode = pricingPhase?.priceCurrencyCode ?: "",
                            description = platformProduct.description,
                            entitlement = offerEntitlement,
                            price = pricingPhase?.priceAmountMicros?.div(1_000_000f) ?: 0f,
                            formattedPrice = pricingPhase?.formattedPrice ?: "",
                            subscriptionPeriodDays = entitlement.periodMs?.let { msToDays(it) },
                        )
                    }
                }

                TenantEntitlement.EntitlementType.consumable -> {
                    val offer = platformProduct.oneTimePurchaseOfferDetails
                    listOf(
                        StoreProduct.Consumable(
                            id = baseProductId,
                            name = platformProduct.title,
                            currencyCode = offer?.priceCurrencyCode ?: "",
                            description = platformProduct.description,
                            entitlement = entitlement,
                            price = offer?.priceAmountMicros?.div(1_000_000f) ?: 0f,
                            formattedPrice = offer?.formattedPrice ?: "",
                        )
                    )
                }

                TenantEntitlement.EntitlementType.non_consumable -> {
                    val offer = platformProduct.oneTimePurchaseOfferDetails
                    listOf(
                        StoreProduct.NonConsumable(
                            id = baseProductId,
                            name = platformProduct.title,
                            currencyCode = offer?.priceCurrencyCode ?: "",
                            description = platformProduct.description,
                            entitlement = entitlement,
                            price = offer?.priceAmountMicros?.div(1_000_000f) ?: 0f,
                            formattedPrice = offer?.formattedPrice ?: "",
                        )
                    )
                }
            }
        }
    }

    private fun msToDays(ms: Long): Int = (ms / (1000L * 60 * 60 * 24)).toInt()
}
