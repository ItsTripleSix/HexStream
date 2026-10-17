package com.github.andreyasadchy.xtra.ui.player

import com.github.andreyasadchy.xtra.model.VideoQuality

/** Xtra 2.60.8's variant eligibility, optional metadata and recovery ordering. */
internal object UnlistedVideoQualities {
    fun supportsReason(reason: String): Boolean = reason != "FR_CODEC_NOT_REQUESTED" &&
        reason != "FR_DUALFORMAT_INCOMPATIBLE"

    fun variant(name: String?, resolution: String?, frameRate: Float?, bitrate: Int, codecs: String?): VideoQuality? {
        if (name.isNullOrBlank()) return null
        return VideoQuality(
            name = name,
            resolution = resolution?.takeIf { it.isNotBlank() }?.substringAfter('x')?.toIntOrNull(),
            frameRate = frameRate?.takeIf { it > 0 },
            bitrate = bitrate.takeIf { it > 0 },
            codecs = codecs?.takeIf { it.isNotBlank() },
        )
    }

    fun ordered(qualities: List<VideoQuality>): List<VideoQuality> = qualities.sortedWith(
        compareByDescending<VideoQuality> { it.resolution }
            .thenByDescending { it.frameRate }
            .thenByDescending { it.bitrate }
    ).toMutableList().apply {
        find { it.name?.startsWith("audio", true) == true }?.let { audio ->
            remove(audio)
            add(audio)
        }
    }
}
