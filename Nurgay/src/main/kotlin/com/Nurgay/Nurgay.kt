package com.Nurgay

import android.annotation.SuppressLint
import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.mainPageOf
import com.lagradost.cloudstream3.utils.AppUtils.tryParseJson
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.loadExtractor
import org.jsoup.Jsoup
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.AppUtils.toJson
import org.jsoup.nodes.Element
import java.time.Year

class Nurgay : MainAPI() {
    override var mainUrl = "https://nurgay.to"
    override var name = "Nurgay"
    override val hasMainPage = true
    override val hasDownloadSupport = true
    override val supportedTypes = setOf(TvType.NSFW)

    override val mainPage = mainPageOf(
        "" to "Latest",
        "?filter=random" to "Random",
        "?filter=longest" to "Longest",
        "?filter=most-viewed" to "Most Viewed"
    )

override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse {
        val url = if(page == 1) "$mainUrl/${request.data}/" else "$mainUrl/${request.data}/page/$page/"
        val document = app.get(url).document
        val home =
            document.select("div.post-thumbnail-container").mapNotNull {
                it.toSearchResult()
            }
        return newHomePageResponse(request.name, home)
    }

    

    private fun Element.toSearchResult(): SearchResponse? {
        val titleElement = this.selectFirst(".entry-header .video-title")
        val title = titleElement?.text() ?: return null
        val href = getProperLink(fixUrl(titleElement?.attr("href") ?: return null))
        var posterUrl = this.select("img").last()?.getImageAttr()
        if (posterUrl != null) {
            if (posterUrl.contains(".gif")) {
                posterUrl = fixUrlNull(this.select("div.poster img").attr("data-wpfc-original-src"))
            }
        }
        val quality = getQualityFromString(this.select("span.quality").text()) ?: "")
        return newMovieSearchResponse(title, href, TvType.Movie) {
            this.posterUrl = posterUrl
            this.quality = quality
        }

    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("$mainUrl/search/$query").document
        return document.select("div.result-item").map {
            val titleElement = it.selectFirst("div.title > a")
            val title = titleElement?.text()?.replace(Regex("\\(\\d{4}\\)"), "")?.trim()
            val href = getProperLink(fixUrl(titleElement?.attr("href")
            val posterUrl = it.selectFirst("img")!!.attr("src")
            newMovieSearchResponse(title, href, TvType.TvSeries) {
                this.posterUrl = posterUrl
            }
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val request = app.get(url)
        val document = request.document
        val title =
            document.selectFirst("div.data > h1")?.text()?.trim().toString()
        var posterUrl = fixUrlNull(document.selectFirst("meta[property=og:image]")?.attr("content"))
        if (posterUrl.isNullOrEmpty()) {
                posterUrl = fixUrlNull(document.select("div.poster img").attr("src"))
        }
        val description = document.select("div.wp-content > p").text().trim()
            val episodes =
                document.select("ul#playeroptionsul > li").map {
                    val name = it.selectFirst("span.title")?.text()
                    val type = it.attr("data-type")
                    val post = it.attr("data-post")
                    val nume = it.attr("data-nume")
                    Episode(
                        LinkData(type, post,nume).toJson(),
                        name,
                    )
                }

            return newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
                this.posterUrl = posterUrl
                this.plot = description
            }
        }


    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
            val loadData = tryParseJson<LinkData>(data)
            val source = app.post(
                url = "$mainUrl/wp-admin/admin-ajax.php", data = mapOf(
                    "action" to "doo_player_ajax", "post" to "${loadData?.post}", "nume" to "${loadData?.nume}", "type" to "${loadData?.type}"
                ), referer = data, headers = mapOf("Accept" to "*/*", "X-Requested-With" to "XMLHttpRequest"
                )).parsed<ResponseHash>().embed_url.getIframe()
            if (!source.contains("youtube")) loadExtractor(
                source,
                "$directUrl/",
                subtitleCallback,
                callback
            )
        return true
        }

    private fun Element.getImageAttr(): String {
        return when {
            this.hasAttr("data-src") -> this.attr("abs:data-src")
            this.hasAttr("data-lazy-src") -> this.attr("abs:data-lazy-src")
            this.hasAttr("srcset") -> this.attr("abs:srcset").substringBefore(" ")
            else -> this.attr("abs:src")
        }
    }

    private fun String.getIframe(): String {
        return Jsoup.parse(this).select("iframe").attr("src")
    }

    data class LinkData(
        val type: String? = null,
        val post: String? = null,
        val nume: String? = null,
    )

    data class ResponseHash(
        @JsonProperty("embed_url") val embed_url: String,
        @JsonProperty("type") val type: String?,
    )
}    
