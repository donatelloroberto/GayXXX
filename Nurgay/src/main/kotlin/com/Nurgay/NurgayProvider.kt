package com.Nurgay

import android.content.Context
import com.lagradost.cloudstream3.extractors.VidHidePro3
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class NurgayProvider: Plugin() {
    override fun load(context: Context) {
        registerMainAPI(Nurgay())
        registerExtractorAPI(Ds2play())
        registerExtractorAPI(Vidsp())
        registerExtractorAPI(VidHidePro3())
        registerExtractorAPI(VidHideplus())
        registerExtractorAPI(VidHidedht())
        registerExtractorAPI(Vidhidehub())
        registerExtractorAPI(Bigwarp())
    }
}
