# Orca Android

Native Orca SDK for Android.

Built with Google Play Billing Library, Retrofit, Moshi, and Kotlin Coroutines, with the OpenAPI-generated client for the Orca backend API.

## Requirements

- Java 11+
- Android minSdk 24+
- Android Gradle Plugin 9.x (see `gradle/libs.versions.toml`)

## Installation

### JitPack

Add JitPack to your settings:

```kotlin
dependencyResolutionManagement {
    repositories {
        maven { url = uri("https://jitpack.io") }
        google()
        mavenCentral()
    }
}
```

Then add the dependency:

```kotlin
implementation("com.github.maxint-app:orca-android:main")
```

## Configuration

```kotlin
import com.maxint.orca.core.*

Orca.configure(context, OrcaConfiguration(
  publicKey = "your_public_key",
  environment = OrcaEnvironment.SANDBOX, // or PRODUCTION
  baseUrl = "https://api.orca.maxint.com",
  customerEmail = null
))
```

## Usage

```kotlin
// identity
Orca.identify("user@example.com")
Orca.logout()

// products & entitlements (suspend functions)
val entitlements = Orca.listEntitlements()
val products = Orca.queryProducts()
val activeProducts = Orca.activeProduct()
val activeEntitlements = Orca.activeEntitlements()

// purchase (launches Play billing flow)
Orca.purchase(entitlement)
```

## StoreProduct

`Orca.queryProducts()` returns `List<StoreProduct>`, a sealed interface covering:

- `StoreProduct.Subscription`
- `StoreProduct.Consumable`
- `StoreProduct.NonConsumable`

Each variant carries `id`, `name`, `currencyCode`, `description`, `formattedPrice`, `price`, and its backing `TenantEntitlement`.

## Structure

- `orca/` — Android library module (`com.maxint.orca`)
  - `core/` — `Orca`, `OrcaConfiguration`, `OrcaEnvironment`, `StoreProduct`
  - `generated/` — OpenAPI-generated Retrofit client and models
- `app/` — example application

## Development

The OpenAPI client is generated from the Orca server OpenAPI spec:

```bash
# pull + filter the OpenAPI spec from a running Orca server
make filter-openapi

# regenerate Kotlin Retrofit client
make generate
```

## License

MIT

© Copyright Maxint Inc. 2026