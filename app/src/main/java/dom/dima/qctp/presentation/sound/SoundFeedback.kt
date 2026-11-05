package dom.dima.qctp.presentation.sound

import android.media.AudioManager
import android.media.ToneGenerator

/**
 * Короткий "щелчок" при успешном сохранении файла.
 * ToneGenerator создаётся лениво и переиспользуется, чтобы не аллоцировать
 * ресурсы на каждый скан.
 */
object SoundFeedback {
    private var toneGenerator: ToneGenerator? = null

    @Synchronized
    private fun ensureGenerator(): ToneGenerator? {
        if (toneGenerator == null) {
            toneGenerator = try {
                ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)
            } catch (e: RuntimeException) {
                // На некоторых устройствах ToneGenerator может быть недоступен
                null
            }
        }
        return toneGenerator
    }

    @Synchronized
    fun click() {
        ensureGenerator()?.startTone(ToneGenerator.TONE_PROP_BEEP, 80)
    }

    @Synchronized
    fun release() {
        toneGenerator?.release()
        toneGenerator = null
    }
}