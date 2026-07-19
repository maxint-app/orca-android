package com.maxint.orca.generated.apis

import com.maxint.orca.generated.infrastructure.CollectionFormats.*
import com.maxint.orca.generated.models.TenantActiveEntitlementsInputBody
import com.maxint.orca.generated.models.TenantActiveEntitlementsResponseBody
import com.maxint.orca.generated.models.TenantActiveSubscriptionInputBody
import com.maxint.orca.generated.models.TenantActiveSubscriptionResponseBody
import com.maxint.orca.generated.models.TenantGoCardlessBillingRequestFlowInputBody
import com.maxint.orca.generated.models.TenantGoCardlessBillingRequestFlowResponseBody
import com.maxint.orca.generated.models.TenantIdentifyUserInputBody
import com.maxint.orca.generated.models.TenantIdentifyUserResponseBody
import com.maxint.orca.generated.models.TenantListEntitlementsResponseBody
import com.maxint.orca.generated.models.TenantListOfferingsResponseBody
import com.maxint.orca.generated.models.TenantListProductsResponseBody
import com.maxint.orca.generated.models.TenantListStripeProductsResponseBody
import com.maxint.orca.generated.models.TenantStripeCheckoutInputBody
import com.maxint.orca.generated.models.TenantStripeCheckoutResponseBody
import com.maxint.orca.generated.models.TenantSyncPlayStorePastPurchasesInputBody
import com.maxint.orca.generated.models.TenantSyncPlayStorePastPurchasesResponseBody
import retrofit2.Response
import retrofit2.http.*

interface TenantApi {
    /**
     * GET tenant/entitlements/{environment}
     * Get tenant entitlements by environment
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error
     *
     * @param environment 
     * @return [TenantListEntitlementsResponseBody]
     */
    @GET("tenant/entitlements/{environment}")
    suspend fun getTenantEntitlementsByEnvironment(@Path("environment") environment: kotlin.String): Response<TenantListEntitlementsResponseBody>

    /**
     * GET tenant/offerings
     * Get tenant offerings
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error
     *
     * @return [TenantListOfferingsResponseBody]
     */
    @GET("tenant/offerings")
    suspend fun getTenantOfferings(): Response<TenantListOfferingsResponseBody>

    /**
     * GET tenant/products
     * Get tenant products
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error
     *
     * @param apiKey  (optional)
     * @return [TenantListProductsResponseBody]
     */
    @GET("tenant/products")
    suspend fun getTenantProducts(@Header("api-key") apiKey: kotlin.String? = null): Response<TenantListProductsResponseBody>

    /**
     * GET tenant/stripe/products/{environment}
     * Get tenant stripe products by environment
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error
     *
     * @param environment 
     * @return [TenantListStripeProductsResponseBody]
     */
    @GET("tenant/stripe/products/{environment}")
    suspend fun getTenantStripeProductsByEnvironment(@Path("environment") environment: kotlin.String): Response<TenantListStripeProductsResponseBody>

    /**
     * POST tenant/entitlements/active
     * Post tenant entitlements active
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error
     *
     * @param tenantActiveEntitlementsInputBody 
     * @return [TenantActiveEntitlementsResponseBody]
     */
    @POST("tenant/entitlements/active")
    suspend fun postTenantEntitlementsActive(@Body tenantActiveEntitlementsInputBody: TenantActiveEntitlementsInputBody): Response<TenantActiveEntitlementsResponseBody>

    /**
     * POST tenant/gocardless/billing-request-flow/{environment}
     * Post tenant gocardless billing request flow by environment
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error
     *
     * @param environment 
     * @param tenantGoCardlessBillingRequestFlowInputBody 
     * @return [TenantGoCardlessBillingRequestFlowResponseBody]
     */
    @POST("tenant/gocardless/billing-request-flow/{environment}")
    suspend fun postTenantGocardlessBillingRequestFlowByEnvironment(@Path("environment") environment: kotlin.String, @Body tenantGoCardlessBillingRequestFlowInputBody: TenantGoCardlessBillingRequestFlowInputBody): Response<TenantGoCardlessBillingRequestFlowResponseBody>

    /**
     * POST tenant/identify
     * Post tenant identify
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error
     *
     * @param tenantIdentifyUserInputBody 
     * @return [TenantIdentifyUserResponseBody]
     */
    @POST("tenant/identify")
    suspend fun postTenantIdentify(@Body tenantIdentifyUserInputBody: TenantIdentifyUserInputBody): Response<TenantIdentifyUserResponseBody>

    /**
     * POST tenant/playstore/sync-purchases
     * Post tenant playstore sync purchases
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error
     *
     * @param tenantSyncPlayStorePastPurchasesInputBody 
     * @return [TenantSyncPlayStorePastPurchasesResponseBody]
     */
    @POST("tenant/playstore/sync-purchases")
    suspend fun postTenantPlaystoreSyncPurchases(@Body tenantSyncPlayStorePastPurchasesInputBody: TenantSyncPlayStorePastPurchasesInputBody): Response<TenantSyncPlayStorePastPurchasesResponseBody>

    /**
     * POST tenant/stripe/checkout/{environment}
     * Post tenant stripe checkout by environment
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error
     *
     * @param environment 
     * @param tenantStripeCheckoutInputBody 
     * @return [TenantStripeCheckoutResponseBody]
     */
    @POST("tenant/stripe/checkout/{environment}")
    suspend fun postTenantStripeCheckoutByEnvironment(@Path("environment") environment: kotlin.String, @Body tenantStripeCheckoutInputBody: TenantStripeCheckoutInputBody): Response<TenantStripeCheckoutResponseBody>

    /**
     * POST tenant/subscriptions/active
     * Post tenant subscriptions active
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error
     *
     * @param tenantActiveSubscriptionInputBody 
     * @return [TenantActiveSubscriptionResponseBody]
     */
    @POST("tenant/subscriptions/active")
    suspend fun postTenantSubscriptionsActive(@Body tenantActiveSubscriptionInputBody: TenantActiveSubscriptionInputBody): Response<TenantActiveSubscriptionResponseBody>

}
