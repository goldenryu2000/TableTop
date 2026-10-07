package dev.tabletop.feedback

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

enum class FeedbackEvent { Pickup, Place, Flip, Draw, Foundation, Invalid, Deal, Win }

interface Feedback {
    fun play(event: FeedbackEvent)
}

object NoFeedback : Feedback {
    override fun play(event: FeedbackEvent) = Unit
}

/** Plays short calm sounds through SoundPool and light haptics through the vibrator. */
class AndroidFeedback(context: Context) : Feedback {
    var soundEnabled = true
    var hapticsEnabled = true

    private val pool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()

    private val sounds: Map<FeedbackEvent, Int> = mapOf(
        FeedbackEvent.Pickup to R.raw.pickup,
        FeedbackEvent.Place to R.raw.place,
        FeedbackEvent.Flip to R.raw.flip,
        FeedbackEvent.Draw to R.raw.draw,
        FeedbackEvent.Foundation to R.raw.foundation,
        FeedbackEvent.Invalid to R.raw.invalid,
        FeedbackEvent.Deal to R.raw.deal,
        FeedbackEvent.Win to R.raw.win,
    ).mapValues { (_, res) -> pool.load(context, res, 1) }

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= 31) {
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Vibrator::class.java)
    }

    override fun play(event: FeedbackEvent) {
        if (soundEnabled) sounds[event]?.let { pool.play(it, 0.7f, 0.7f, 1, 0, 1f) }
        if (hapticsEnabled) vibrate(event)
    }

    private fun vibrate(event: FeedbackEvent) {
        val v = vibrator?.takeIf { it.hasVibrator() } ?: return
        val effect = if (Build.VERSION.SDK_INT >= 29) {
            when (event) {
                FeedbackEvent.Pickup, FeedbackEvent.Draw -> VibrationEffect.EFFECT_TICK
                FeedbackEvent.Place, FeedbackEvent.Foundation -> VibrationEffect.EFFECT_CLICK
                FeedbackEvent.Invalid -> VibrationEffect.EFFECT_DOUBLE_CLICK
                FeedbackEvent.Win -> VibrationEffect.EFFECT_HEAVY_CLICK
                FeedbackEvent.Flip, FeedbackEvent.Deal -> return
            }.let(VibrationEffect::createPredefined)
        } else {
            val ms = when (event) {
                FeedbackEvent.Pickup, FeedbackEvent.Draw -> 8L
                FeedbackEvent.Place, FeedbackEvent.Foundation -> 14L
                FeedbackEvent.Invalid -> 30L
                FeedbackEvent.Win -> 40L
                FeedbackEvent.Flip, FeedbackEvent.Deal -> return
            }
            VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE)
        }
        v.vibrate(effect)
    }
}
