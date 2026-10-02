package com.nuvio.app.core.ui

import coil3.ImageLoader

internal val platformProvidesImageLoader: Boolean = false

internal actual fun ImageLoader.Builder.configurePlatformImageLoader(): ImageLoader.Builder {
    return components {
        add(SkiaGifDecoder.Factory())
    }
}
