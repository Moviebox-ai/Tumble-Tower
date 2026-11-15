package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.physics.PhysicsUiState
import com.example.game.physics.PlayPhase
import com.example.game.physics.PullVerdict

private val HudBg = Color(0xFF0B1433).copy(alpha = 0.9f)

/**
 * HUD for the "readable tower" mechanic:
 *  - Stress Vision toggle with its Focus meter (Focus drains while on, refills from skilful play)
 *  - Live pull-risk badge while a block is being dragged (only with Stress Vision on)
 *  - Calm Drop hint while holding a block, Daredevil streak chip
 */
@Composable
fun StressVisionHud(
    state: PhysicsUiState,
    onToggleXray: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    if (state.phase != PlayPhase.PLAYING && state.phase != PlayPhase.HOLDING_TO_PLACE) return

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (state.pullRiskPercent >= 0 && state.isDraggingBlock) {
            val verdict = state.pullVerdict ?: PullVerdict.SAFE
            val color = verdictColor(verdict)
            Text(
                text = "PULL RISK ${state.pullRiskPercent}%  -  ${verdict.label.uppercase()}",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(color.copy(alpha = 0.92f))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            )
        }

        if (state.phase == PlayPhase.HOLDING_TO_PLACE) {
            val calm = state.swayCalm
            val ready = calm >= 0.8f
            Text(
                text = if (ready) "CALM - release now for a CALM DROP" else "Tower is swaying - wait for it to settle",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background((if (ready) Color(0xFF16A34A) else Color(0xFFB45309)).copy(alpha = 0.9f))
                    .padding(horizontal = 12.dp, vertical = 5.dp)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val on = state.xrayActive
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (on) Color(0xFF0E7490).copy(alpha = 0.95f) else HudBg)
                    .border(1.4.dp, Color(0xFF22D3EE).copy(alpha = if (on) 1f else 0.6f), RoundedCornerShape(16.dp))
                    .clickable { onToggleXray(!on) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (on) "STRESS VISION ON" else "STRESS VISION",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Box(
                    modifier = Modifier
                        .width(64.dp)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.5f))
                ) {
                    Box(
                        modifier = Modifier
                            .width((64f * state.focusPercent / 100f).dp)
                            .height(8.dp)
                            .background(if (state.focusPercent > 25) Color(0xFF22D3EE) else Color(0xFFEF4444))
                    )
                }
            }
            if (state.daredevilStreak > 0) {
                Text(
                    text = "DAREDEVIL x${state.daredevilStreak}",
                    color = Color(0xFF111827),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFFFACC15))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}

private fun verdictColor(v: PullVerdict): Color = when (v) {
    PullVerdict.SAFE -> Color(0xFF16A34A)
    PullVerdict.RISKY -> Color(0xFFCA8A04)
    PullVerdict.DANGEROUS -> Color(0xFFEA580C)
    PullVerdict.FATAL -> Color(0xFFDC2626)
}
