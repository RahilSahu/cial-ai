package ai.cial.chat.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import kotlin.math.max
import kotlin.math.min

/**
 * A simple bar waveform indicator. Bar heights are driven by the live RMS
 * level reported by SpeechRecognizer while the user speaks.
 */
class WaveformView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val barCount = 28
    private val levels = FloatArray(barCount) { 0.08f }
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF8B5CF6.toInt()
        style = Paint.Style.FILL
    }
    private val rect = RectF()

    /** Feeds a new RMS value (roughly -2 to 10 dB from SpeechRecognizer). */
    fun onRmsChanged(rmsdB: Float) {
        val normalized = ((rmsdB + 2f) / 12f).coerceIn(0.05f, 1f)
        System.arraycopy(levels, 1, levels, 0, barCount - 1)
        levels[barCount - 1] = normalized
        invalidate()
    }

    /** Settles the bars back to a quiet line. */
    fun reset() {
        for (i in levels.indices) levels[i] = 0.08f
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width == 0 || height == 0) return
        val gap = 3f * resources.displayMetrics.density
        val barWidth = max(2f, (width - gap * (barCount - 1)) / barCount)
        val centerY = height / 2f
        val maxHalf = height / 2f - 2f
        var x = 0f
        for (i in 0 until barCount) {
            // Bars near the center read slightly taller for a nicer shape.
            val shape = 0.75f + 0.25f * (1f - kotlin.math.abs(i - barCount / 2f) / (barCount / 2f))
            val half = min(maxHalf, max(2f, levels[i] * maxHalf * shape))
            rect.set(x, centerY - half, x + barWidth, centerY + half)
            canvas.drawRoundRect(rect, barWidth / 2f, barWidth / 2f, paint)
            x += barWidth + gap
        }
    }
}
