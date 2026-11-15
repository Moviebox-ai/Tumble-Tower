package com.example.services

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Centralized AdMob Test IDs.
 * Replace these with your production AdMob Unit IDs before publishing to Google Play.
 */
object AdConfig {
    const val ADMOB_APP_ID = "ca-app-pub-3940256099942544~3347511713"
    const val BANNER_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"
    const val INTERSTITIAL_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"
    const val REWARDED_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"
    const val APP_OPEN_UNIT_ID = "ca-app-pub-3940256099942544/9257395921"
}

enum class RewardPlacement {
    DOUBLE_COINS,
    REVIVE_TOWER,
    FREE_POWERUP,
    FREE_SPIN
}

interface AdManager {
    val adsRemoved: StateFlow<Boolean>
    fun setAdsRemoved(removed: Boolean)
    fun shouldShowInterstitial(): Boolean
    suspend fun showInterstitialIfReady(): Boolean
    suspend fun showRewardedAd(placement: RewardPlacement): Boolean
    suspend fun showAppOpenIfReady(): Boolean
}

class AdMobTestAdapter : AdManager {
    private val _adsRemoved = MutableStateFlow(false)
    override val adsRemoved: StateFlow<Boolean> = _adsRemoved.asStateFlow()

    private var runsSinceLastInterstitial = 0
    private var appOpenShownThisSession = false

    override fun setAdsRemoved(removed: Boolean) {
        _adsRemoved.value = removed
    }

    override fun shouldShowInterstitial(): Boolean {
        if (_adsRemoved.value) return false
        runsSinceLastInterstitial++
        return runsSinceLastInterstitial >= 2
    }

    override suspend fun showInterstitialIfReady(): Boolean {
        if (!shouldShowInterstitial()) return false
        runsSinceLastInterstitial = 0
        delay(250)
        return true
    }

    override suspend fun showRewardedAd(placement: RewardPlacement): Boolean {
        // Simulates rewarded video completion hook; swap with Google Mobile Ads RewardedAd.show()
        delay(450)
        return true
    }

    override suspend fun showAppOpenIfReady(): Boolean {
        if (_adsRemoved.value || appOpenShownThisSession) return false
        appOpenShownThisSession = true
        return true
    }
}

data class StoreProduct(
    val sku: String,
    val title: String,
    val subtitle: String,
    val priceDisplay: String,
    val coinsReward: Int = 0,
    val gemsReward: Int = 0,
    val removesAds: Boolean = false
)

interface BillingManager {
    val catalog: List<StoreProduct>
    suspend fun purchaseProduct(sku: String): Result<StoreProduct>
    suspend fun restorePurchases(): Result<List<String>>
}

class PlayBillingAdapter : BillingManager {
    override val catalog: List<StoreProduct> = listOf(
        StoreProduct(
            sku = "remove_ads_forever",
            title = "Remove Ads",
            subtitle = "Zero banner or interstitial ads forever",
            priceDisplay = "$2.99",
            gemsReward = 50,
            removesAds = true
        ),
        StoreProduct(
            sku = "coins_pile_2500",
            title = "Starter Coin Pile",
            subtitle = "2,500 Coins + 15 Gems",
            priceDisplay = "$0.99",
            coinsReward = 2500,
            gemsReward = 15
        ),
        StoreProduct(
            sku = "coins_vault_10000",
            title = "Skyfall Vault",
            subtitle = "10,000 Coins + 80 Gems",
            priceDisplay = "$3.99",
            coinsReward = 10000,
            gemsReward = 80
        ),
        StoreProduct(
            sku = "gems_crown_250",
            title = "Crown Gem Chest",
            subtitle = "250 Gems + 5,000 Coins",
            priceDisplay = "$4.99",
            coinsReward = 5000,
            gemsReward = 250
        )
    )

    override suspend fun purchaseProduct(sku: String): Result<StoreProduct> {
        val product = catalog.find { it.sku == sku }
            ?: return Result.failure(IllegalArgumentException("Unknown SKU: $sku"))
        delay(300)
        return Result.success(product)
    }

    override suspend fun restorePurchases(): Result<List<String>> {
        delay(250)
        return Result.success(emptyList())
    }
}
