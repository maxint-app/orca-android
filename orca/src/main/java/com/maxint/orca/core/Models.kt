package com.maxint.orca.core

import com.maxint.orca.generated.models.TenantEntitlement
import com.squareup.moshi.Json

enum class OrcaEnvironment {
    @Json(name = "sandbox")
    SANDBOX,

    @Json(name = "prod")
    PRODUCTION
}

data class OrcaConfiguration(
    val publicKey: String,
    val environment: OrcaEnvironment,
    val baseUrl: String = "https://api.orca.maxint.com",
    val customerEmail: String?

)

sealed interface StoreProduct {
    val id: String
    val name: String
    val currencyCode: String
    val description: String
    val entitlement: TenantEntitlement
    val price: Float
    val formattedPrice: String

    data class Subscription(
        override val id: String,
        override val name: String,
        override val currencyCode: String,
        override val description: String,
        override val entitlement: TenantEntitlement,
        override val price: Float,
        override val formattedPrice: String,
        val subscriptionPeriodDays: Int?
    ) : StoreProduct

    data class Consumable(
        override val id: String,
        override val name: String,
        override val currencyCode: String,
        override val description: String,
        override val entitlement: TenantEntitlement,
        override val price: Float,
        override val formattedPrice: String,
    ) : StoreProduct

    data class NonConsumable(
        override val id: String,
        override val name: String,
        override val currencyCode: String,
        override val description: String,
        override val entitlement: TenantEntitlement,
        override val price: Float,
        override val formattedPrice: String,
    ) : StoreProduct
}