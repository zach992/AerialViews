package com.neilturner.aerialviews.models

import com.neilturner.aerialviews.models.music.MusicPlaylist
import com.neilturner.aerialviews.models.videos.AerialMedia
import com.neilturner.aerialviews.utils.CitySpotlight

data class MediaFetchResult(
    val mediaPlaylist: MediaPlaylist,
    val musicPlaylist: MusicPlaylist?,
    val musicResumeIndex: Int = 0,
    val isFromCache: Boolean = false,
    val citySpotlight: CitySpotlight<AerialMedia>? = null,
)

enum class LoadingStatus {
    LOADING,
    BUILDING,
    RESUMING,
}
