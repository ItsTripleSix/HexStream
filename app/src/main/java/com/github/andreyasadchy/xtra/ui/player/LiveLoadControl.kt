package com.github.andreyasadchy.xtra.ui.player

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.LoadControl
import androidx.media3.exoplayer.analytics.PlayerId
import androidx.media3.exoplayer.source.MediaSource.MediaPeriodId
import androidx.media3.exoplayer.source.TrackGroupArray
import androidx.media3.exoplayer.trackselection.ExoTrackSelection

/** Explicit forwarding is required: Kotlin delegation does not forward Java default methods. */
@OptIn(UnstableApi::class)
internal class LiveLoadControl(private val original: LoadControl, private val lowDelay: () -> Boolean) : LoadControl {
    override fun onPrepared(playerId: PlayerId) = original.onPrepared(playerId)
    override fun onTracksSelected(parameters: LoadControl.Parameters, trackGroups: TrackGroupArray, trackSelections: Array<out ExoTrackSelection?>) =
        original.onTracksSelected(parameters, trackGroups, trackSelections)
    override fun onStopped(playerId: PlayerId) = original.onStopped(playerId)
    override fun onReleased(playerId: PlayerId) = original.onReleased(playerId)
    override fun getAllocator(playerId: PlayerId) = original.getAllocator(playerId)
    override fun getBackBufferDurationUs(playerId: PlayerId) = original.getBackBufferDurationUs(playerId)
    override fun retainBackBufferFromKeyframe(playerId: PlayerId) = original.retainBackBufferFromKeyframe(playerId)
    override fun shouldContinueLoading(parameters: LoadControl.Parameters) = original.shouldContinueLoading(parameters)
    override fun shouldContinuePreloading(playerId: PlayerId, timeline: Timeline, mediaPeriodId: MediaPeriodId, bufferedDurationUs: Long) =
        original.shouldContinuePreloading(playerId, timeline, mediaPeriodId, bufferedDurationUs)
    override fun shouldStartPlayback(parameters: LoadControl.Parameters): Boolean =
        LiveBufferPolicy.shouldStart(lowDelay(), parameters.targetLiveOffsetUs != C.TIME_UNSET,
            parameters.bufferedDurationUs, parameters.playbackSpeed, parameters.rebuffering)
            ?: original.shouldStartPlayback(parameters)
}
