package com.maxint.orca.core

import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.PurchasesUpdatedListener
import com.maxint.orca.generated.apis.TenantApi
import com.maxint.orca.generated.infrastructure.ApiClient
import com.maxint.orca.generated.models.StorableEntitlement
import com.maxint.orca.generated.models.TenantEntitlement
import com.maxint.orca.generated.models.TenantIdentifyUserInputBody


object Orca {
    private lateinit var configuration: OrcaConfiguration
    private val purchasesUpdatedListener =
        PurchasesUpdatedListener { billingResult, purchases ->
            // To be implemented in a later section.
        }
    private lateinit var billingClient: BillingClient
    private lateinit var api: ApiClient
    private lateinit var tenantApi: TenantApi
    private var customerId: String? = null

    private var cachedEntitlements: List<StorableEntitlement>? = null
    private var cachedStoreProduct: List<StoreProduct>? = null
    private var cachedPlatformProduct: List<ProductDetails>? = null

    fun configure(context: Context, config: OrcaConfiguration) {
        configuration = config
        billingClient = BillingClient.newBuilder(context)
            .setListener(purchasesUpdatedListener)
            .build()

        api = ApiClient(baseUrl = configuration.baseUrl)
        tenantApi = api.createService(TenantApi::class.java)
    }

    suspend fun identify(customerEmail: String) {
        val res =
            tenantApi.postTenantIdentify(TenantIdentifyUserInputBody(customerEmail = customerEmail))
        val body = res.body()
        customerId = body?.customerId
        configuration = configuration.copy(customerEmail = customerEmail)
    }

    suspend fun logout() {
        customerId = null
        configuration = configuration.copy(customerEmail = null)
    }

    suspend fun activeProduct(): List<StoreProduct> {

    }

    suspend fun activeEntitlements(): List<StorableEntitlement> {

    }

    suspend fun listEntitlements(): List<TenantEntitlement> {

    }

    suspend fun queryProducts(): List<StoreProduct> {

    }

    suspend fun purchase(entitlement: TenantEntitlement) {

    }
}