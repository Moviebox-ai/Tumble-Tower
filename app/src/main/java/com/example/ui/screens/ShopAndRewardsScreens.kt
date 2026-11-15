package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.MissionEntity
import com.example.data.local.PowerUpInventory
import com.example.data.remote.UserProfileDoc
import com.example.game.campaign.CampaignCatalog
import com.example.game.campaign.ShopItem
import com.example.game.campaign.WheelSlice
import com.example.game.physics.PowerUpType
import com.example.services.StoreProduct
import com.example.ui.components.AdBannerBar
import com.example.ui.components.ChunkyGameButton
import com.example.ui.components.GlassCard
import com.example.ui.theme.CyanIce
import com.example.ui.theme.EmeraldGo
import com.example.ui.theme.GoldSun
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.SkyfallDeepIndigo
import com.example.ui.theme.SkyfallMidnight
import com.example.ui.theme.SkyfallSlateCard
import kotlinx.coroutines.launch
import kotlin.random.Random

@Composable
fun ShopScreen(
    profile: UserProfileDoc,
    powerUps: PowerUpInventory,
    ownedCosmetics: Triple<Set<String>, Set<String>, Set<String>>,
    storeCatalog: List<StoreProduct>,
    onBack: () -> Unit,
    onBuyOrEquipCosmetic: (ShopItem) -> Unit,
    onBuyPowerUp: (PowerUpType) -> Unit,
    onWatchAdFreePowerUp: (PowerUpType) -> Unit,
    onPurchaseSku: (String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Skins", "Themes", "Effects", "Power-Ups", "Packs")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(SkyfallMidnight, SkyfallDeepIndigo)))
            .statusBarsPadding()
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Text("Skyfall Shop", style = MaterialTheme.typography.headlineMedium, color = GoldSun)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(end = 8.dp)) {
                Text("${profile.coins} C", style = MaterialTheme.typography.labelLarge, color = GoldSun)
                Text("${profile.gems} G", style = MaterialTheme.typography.labelLarge, color = CyanIce)
            }
        }

        TabRow(selectedTabIndex = selectedTab, containerColor = SkyfallSlateCard, contentColor = GoldSun) {
            tabs.forEachIndexed { idx, label ->
                Tab(
                    selected = selectedTab == idx,
                    onClick = { selectedTab = idx },
                    text = { Text(label, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)) }
                )
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (selectedTab) {
                0, 1, 2 -> {
                    val category = when (selectedTab) {
                        0 -> "skin"
                        1 -> "theme"
                        else -> "effect"
                    }
                    val itemsList = CampaignCatalog.shopItems.filter { it.category == category }
                    items(itemsList) { item ->
                        val owned = when (category) {
                            "skin" -> item.id in ownedCosmetics.first
                            "theme" -> item.id in ownedCosmetics.second
                            else -> item.id in ownedCosmetics.third
                        }
                        val equipped = when (category) {
                            "skin" -> profile.equippedSkin == item.id
                            "theme" -> profile.equippedTheme == item.id
                            else -> profile.equippedEffect == item.id
                        }
                        GlassCard(
                            borderColor = if (equipped) GoldSun else item.previewPrimary,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Brush.linearGradient(listOf(item.previewPrimary, item.previewSecondary)))
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.name, style = MaterialTheme.typography.titleLarge, color = Color.White)
                                    Text(item.description, style = MaterialTheme.typography.bodyMedium, color = Color(0xFFCBD5E1))
                                }
                                Button(
                                    onClick = { onBuyOrEquipCosmetic(item) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = when {
                                            equipped -> EmeraldGo
                                            owned -> CyanIce
                                            else -> GoldSun
                                        },
                                        contentColor = SkyfallMidnight
                                    )
                                ) {
                                    Text(
                                        text = when {
                                            equipped -> "Equipped"
                                            owned -> "Equip"
                                            item.gemPrice > 0 -> "${item.gemPrice} Gems"
                                            else -> "${item.coinPrice} Coins"
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                3 -> {
                    items(PowerUpType.entries) { pu ->
                        val count = when (pu) {
                            PowerUpType.FREEZE -> powerUps.freeze
                            PowerUpType.GLUE -> powerUps.glue
                            PowerUpType.UNDO -> powerUps.undo
                            PowerUpType.SHIELD -> powerUps.shield
                            PowerUpType.CRANE -> powerUps.crane
                        }
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("${pu.title} (Owned: $count)", style = MaterialTheme.typography.titleLarge, color = GoldSun)
                                    Text("${pu.gemCost} Gems", style = MaterialTheme.typography.labelLarge, color = CyanIce)
                                }
                                Text(pu.description, style = MaterialTheme.typography.bodyMedium, color = Color(0xFFCBD5E1))
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Button(
                                        onClick = { onBuyPowerUp(pu) },
                                        colors = ButtonDefaults.buttonColors(containerColor = CyanIce, contentColor = SkyfallMidnight),
                                        modifier = Modifier.weight(1f)
                                    ) { Text("Buy (${pu.gemCost}G)") }
                                    Button(
                                        onClick = { onWatchAdFreePowerUp(pu) },
                                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGo, contentColor = SkyfallMidnight),
                                        modifier = Modifier.weight(1f)
                                    ) { Text("Free (Ad)") }
                                }
                            }
                        }
                    }
                }
                else -> {
                    items(storeCatalog) { prod ->
                        val isAlreadyNoAds = prod.removesAds && profile.adsRemoved
                        GlassCard(borderColor = NeonViolet, modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(prod.title, style = MaterialTheme.typography.titleLarge, color = GoldSun)
                                    Text(prod.subtitle, style = MaterialTheme.typography.bodyMedium, color = Color.White)
                                }
                                Button(
                                    onClick = { onPurchaseSku(prod.sku) },
                                    enabled = !isAlreadyNoAds,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isAlreadyNoAds) EmeraldGo else GoldSun,
                                        contentColor = SkyfallMidnight
                                    )
                                ) {
                                    Text(if (isAlreadyNoAds) "Owned" else prod.priceDisplay)
                                }
                            }
                        }
                    }
                }
            }
        }

        AdBannerBar(adsRemoved = profile.adsRemoved)
    }
}

@Composable
fun DailyRewardsAndMissionsScreen(
    profile: UserProfileDoc,
    lastSpinDate: String,
    missions: List<MissionEntity>,
    onBack: () -> Unit,
    onClaimDailyReward: () -> Unit,
    onSpinWin: (WheelSlice) -> Unit,
    onWatchAdFreeSpin: suspend () -> Boolean,
    onClaimMission: (MissionEntity) -> Unit
) {
    val today = remember { CampaignCatalog.todayDateKey() }
    val dailyClaimed = profile.lastDailyRewardDate == today
    val spinClaimed = lastSpinDate == today
    val currentDayIndex = (profile.dailyStreak.toInt() % 7)

    val wheelRotation = remember { Animatable(0f) }
    var spinning by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(SkyfallMidnight, SkyfallDeepIndigo)))
            .statusBarsPadding()
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text("Rewards, Wheel & Missions", style = MaterialTheme.typography.headlineMedium, color = GoldSun)
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. 7-Day Daily Rewards
            item {
                GlassCard(borderColor = GoldSun, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("7-Day Streak Rewards", style = MaterialTheme.typography.titleLarge, color = GoldSun)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(CampaignCatalog.dailyRewards) { tier ->
                                val idx = tier.day - 1
                                val isToday = idx == currentDayIndex && !dailyClaimed
                                val isDone = idx < currentDayIndex || (idx == currentDayIndex && dailyClaimed)
                                Column(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(
                                            when {
                                                isToday -> GoldSun.copy(alpha = 0.25f)
                                                isDone -> EmeraldGo.copy(alpha = 0.2f)
                                                else -> SkyfallMidnight.copy(alpha = 0.7f)
                                            }
                                        )
                                        .border(1.dp, if (isToday) GoldSun else Color(0xFF33447A), RoundedCornerShape(14.dp))
                                        .padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(tier.label, style = MaterialTheme.typography.labelSmall, color = Color.White)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("${tier.coins}C", style = MaterialTheme.typography.labelLarge, color = GoldSun)
                                    if (tier.gems > 0) {
                                        Text("+${tier.gems}G", style = MaterialTheme.typography.labelSmall, color = CyanIce)
                                    }
                                    if (isDone) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = EmeraldGo, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                        ChunkyGameButton(
                            text = if (dailyClaimed) "Claimed Today!" else "Claim Daily Reward",
                            onClick = onClaimDailyReward,
                            enabled = !dailyClaimed,
                            containerColor = EmeraldGo,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("claim_daily_reward_button")
                        )
                    }
                }
            }

            // 2. Lucky Spin Wheel
            item {
                GlassCard(borderColor = NeonViolet, modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Lucky Skyfall Wheel", style = MaterialTheme.typography.titleLarge, color = CyanIce)
                        Box(modifier = Modifier.size(190.dp), contentAlignment = Alignment.Center) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val slices = CampaignCatalog.spinSlices
                                val sweep = 360f / slices.size
                                rotate(wheelRotation.value) {
                                    slices.forEachIndexed { idx, slice ->
                                        drawArc(
                                            color = slice.color,
                                            startAngle = idx * sweep,
                                            sweepAngle = sweep,
                                            useCenter = true
                                        )
                                    }
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(SkyfallMidnight)
                                    .border(2.dp, GoldSun, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Casino, contentDescription = null, tint = GoldSun)
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            ChunkyGameButton(
                                text = if (spinClaimed) "Used Today" else "Free Spin",
                                enabled = !spinClaimed && !spinning,
                                onClick = {
                                    scope.launch {
                                        spinning = true
                                        val idx = Random.nextInt(CampaignCatalog.spinSlices.size)
                                        val target = wheelRotation.value + 1440f + idx * (360f / CampaignCatalog.spinSlices.size)
                                        wheelRotation.animateTo(target, tween(2200, easing = FastOutSlowInEasing))
                                        onSpinWin(CampaignCatalog.spinSlices[idx])
                                        spinning = false
                                    }
                                },
                                containerColor = GoldSun,
                                modifier = Modifier.weight(1f)
                            )
                            ChunkyGameButton(
                                text = "Ad Spin",
                                enabled = !spinning,
                                onClick = {
                                    scope.launch {
                                        if (onWatchAdFreeSpin()) {
                                            spinning = true
                                            val idx = Random.nextInt(CampaignCatalog.spinSlices.size)
                                            val target = wheelRotation.value + 1440f + idx * (360f / CampaignCatalog.spinSlices.size)
                                            wheelRotation.animateTo(target, tween(2200, easing = FastOutSlowInEasing))
                                            onSpinWin(CampaignCatalog.spinSlices[idx])
                                            spinning = false
                                        }
                                    }
                                },
                                containerColor = NeonViolet,
                                contentColor = Color.White,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // 3. Missions & Achievements
            item {
                Text("Missions & Achievements", style = MaterialTheme.typography.titleLarge, color = GoldSun)
            }
            items(missions) { m ->
                val ready = m.progress >= m.target && !m.claimed
                GlassCard(
                    borderColor = if (ready) EmeraldGo else Color(0xFF33447A),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(m.title, style = MaterialTheme.typography.titleLarge, color = Color.White)
                                Text(m.description, style = MaterialTheme.typography.bodyMedium, color = Color(0xFFCBD5E1))
                            }
                            Text("${m.progress}/${m.target}", style = MaterialTheme.typography.labelLarge, color = CyanIce)
                        }
                        LinearProgressIndicator(
                            progress = { (m.progress.toFloat() / m.target.coerceAtLeast(1)).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = EmeraldGo,
                            trackColor = SkyfallMidnight
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Reward: ${m.rewardCoins}C + ${m.rewardGems}G", style = MaterialTheme.typography.labelSmall, color = GoldSun)
                            Button(
                                onClick = { onClaimMission(m) },
                                enabled = ready,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (m.claimed) SkyfallMidnight else EmeraldGo,
                                    contentColor = if (m.claimed) Color.Gray else SkyfallMidnight
                                )
                            ) {
                                Text(if (m.claimed) "Claimed" else if (ready) "Claim" else "In Progress")
                            }
                        }
                    }
                }
            }
        }

        AdBannerBar(adsRemoved = profile.adsRemoved)
    }
}
