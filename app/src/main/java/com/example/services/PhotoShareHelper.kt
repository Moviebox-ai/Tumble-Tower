package com.example.services

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.core.content.FileProvider
import com.example.game.physics.BlockState
import com.example.game.physics.BlockType
import java.io.File
import java.io.FileOutputStream

enum class PhotoBackdrop(val label: String, val topColor: Int, val bottomColor: Int) {
    DEFAULT("Default", 0xFF0E1B47.toInt(), 0xFF281E4B.toInt()),
    SUNSET("Sunset", 0xFF4A1942.toInt(), 0xFFF97316.toInt()),
    NIGHT("Night", 0xFF050816.toInt(), 0xFF172554.toInt()),
    SPACE("Space", 0xFF1A0B2E.toInt(), 0xFF3B0764.toInt())
}

object PhotoShareHelper {

    fun renderTowerSnapshot(
        blocks: List<BlockState>,
        score: Long,
        height: Int,
        backdrop: PhotoBackdrop,
        showWatermark: Boolean
    ): Bitmap {
        val width = 1080
        val heightPx = 1350
        val bitmap = Bitmap.createBitmap(width, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Background gradient
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, 0f, heightPx.toFloat(),
                backdrop.topColor, backdrop.bottomColor, Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), heightPx.toFloat(), bgPaint)

        // Decorative stars
        val starPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(140, 255, 255, 255)
        }
        for (i in 0 until 36) {
            val sx = ((i * 197 + 43) % width).toFloat()
            val sy = ((i * 271 + 89) % (heightPx * 0.65f).toInt()).toFloat()
            val sr = ((i % 3) + 1.5f)
            canvas.drawCircle(sx, sy, sr, starPaint)
        }

        // Pedestal
        val baseY = heightPx * 0.78f
        val pedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF1E293B.toInt()
        }
        canvas.drawRoundRect(RectF(260f, baseY, 820f, baseY + 70f), 18f, 18f, pedPaint)

        // Draw tower blocks
        val blockPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 3f
            color = Color.argb(110, 255, 255, 255)
        }

        val maxLayer = (blocks.maxOfOrNull { it.layer } ?: 8).coerceAtLeast(8)
        val scale = (620f / (maxLayer * 34f)).coerceIn(0.65f, 1.35f)
        val centerX = width / 2f

        blocks.sortedBy { it.layer }.forEach { block ->
            if (block.isShattered) return@forEach
            val bx = centerX + block.x * 95f * scale
            val by = baseY - (block.layer + 1) * 32f * scale + block.y * 40f * scale
            val bw = 72f * scale
            val bh = 28f * scale
            val colorInt = when (block.type) {
                BlockType.WOOD -> 0xFFD97706.toInt()
                BlockType.ICE -> 0xFF38BDF8.toInt()
                BlockType.RUBBER -> 0xFFEC4899.toInt()
                BlockType.MAGNET -> 0xFFEF4444.toInt()
                BlockType.GOLD -> 0xFFFACC15.toInt()
                BlockType.BOMB -> 0xFFDC2626.toInt()
                BlockType.GLASS -> 0xFF7DD3FC.toInt()
            }
            blockPaint.color = colorInt
            canvas.save()
            canvas.rotate(block.angleDeg, bx, by)
            val rect = RectF(bx - bw / 2f, by - bh / 2f, bx + bw / 2f, by + bh / 2f)
            canvas.drawRoundRect(rect, 8f, 8f, blockPaint)
            canvas.drawRoundRect(rect, 8f, 8f, strokePaint)
            canvas.restore()
        }

        // Stats badge
        val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(190, 15, 23, 42)
        }
        canvas.drawRoundRect(RectF(80f, 1130f, 1000f, 1265f), 28f, 28f, badgePaint)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFCA28.toInt()
            textSize = 42f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 32f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("Height: $height Blocks", 125f, 1190f, titlePaint)
        canvas.drawText("Score: $score", 125f, 1238f, subPaint)

        if (showWatermark) {
            val wmPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(230, 255, 255, 255)
                textSize = 38f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.RIGHT
            }
            canvas.drawText("Tumble Tower: Skyfall", 965f, 1215f, wmPaint)
        }

        return bitmap
    }

    fun shareTowerBitmap(context: Context, bitmap: Bitmap, message: String) {
        val cacheDir = File(context.cacheDir, "shared_images").apply { mkdirs() }
        val file = File(cacheDir, "tumble_tower_snapshot.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 95, out)
        }
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, message)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Tower Snapshot"))
    }
}
