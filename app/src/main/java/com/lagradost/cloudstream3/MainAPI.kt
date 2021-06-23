package com.lagradost.cloudstream3

import android.app.Activity
import androidx.preference.PreferenceManager
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.lagradost.cloudstream3.animeproviders.DubbedAnimeProvider
import com.lagradost.cloudstream3.animeproviders.ShiroProvider
import com.lagradost.cloudstream3.movieproviders.MeloMovieProvider
import com.lagradost.cloudstream3.utils.ExtractorLink
import java.util.*
import kotlin.collections.ArrayList

const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; rv:68.0) Gecko/20100101 Firefox/68.0"
val baseHeader = mapOf("User-Agent" to USER_AGENT)
val mapper = JsonMapper.builder().addModule(KotlinModule())
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).build()!!

object APIHolder {
    val unixTime: Long
        get() = System.currentTimeMillis() / 1000L

    val allApi = AllProvider()

    private const val defProvider = 0

    val apis = arrayListOf(
        ShiroProvider(),
        MeloMovieProvider(),
        DubbedAnimeProvider(),
    )

    fun getApiFromName(apiName: String?): MainAPI {
        for (api in apis) {
            if (apiName == api.name)
                return api
        }
        return apis[defProvider]
    }

    fun Activity.getApiSettings(): HashSet<String> {
        val settingsManager = PreferenceManager.getDefaultSharedPreferences(this)

        return settingsManager.getStringSet(this.getString(R.string.search_providers_list_key),
            setOf(apis[defProvider].name))?.toHashSet() ?: hashSetOf(apis[defProvider].name)
    }
}


abstract class MainAPI {
    open val name = "NONE"
    open val mainUrl = "NONE"
    open val instantLinkLoading = false // THIS IS IF THE LINK IS STORED IN THE "DATA"
    open val hasQuickSearch = false
    open fun search(query: String): ArrayList<Any>? { // SearchResponse
        return null
    }

    open fun quickSearch(query: String) : ArrayList<Any>? {
        return null
    }

    open fun load(slug: String): Any? { //LoadResponse
        return null
    }

    // callback is fired once a link is found, will return true if method is executed successfully
    open fun loadLinks(data: String, isCasting: Boolean, callback: (ExtractorLink) -> Unit): Boolean {
        return false
    }
}

fun MainAPI.fixUrl(url: String): String {
    if(url.startsWith("http")) {
        return url
    }

    val startsWithNoHttp = url.startsWith("//")
    if(startsWithNoHttp) {
        return "https:$url"
    }
    else {
        if(url.startsWith('/')) {
            return mainUrl + url
        }
        return "$mainUrl/$url"
    }
}

fun sortUrls(urls: List<ExtractorLink>): List<ExtractorLink> {
    return urls.sortedBy { t -> -t.quality }
}

enum class ShowStatus {
    Completed,
    Ongoing,
}

enum class DubStatus {
    Dubbed,
    Subbed,
}

enum class TvType {
    Movie,
    TvSeries,
    Anime,
    ONA,
}

interface SearchResponse {
    val name: String
    val url: String // PUBLIC URL FOR OPEN IN APP
    val slug: String // USED FOR INTERNAL DATA
    val apiName: String
    val type: TvType
    val posterUrl: String?
    val year: Int?
}

data class AnimeSearchResponse(
    override val name: String,
    override val url: String,
    override val slug: String,
    override val apiName: String,
    override val type: TvType,

    override val posterUrl: String?,
    override val year: Int?,

    val otherName: String?,
    val dubStatus: EnumSet<DubStatus>?,
    val dubEpisodes: Int?,
    val subEpisodes: Int?,
) : SearchResponse

data class MovieSearchResponse(
    override val name: String,
    override val url: String,
    override val slug: String,
    override val apiName: String,
    override val type: TvType,

    override val posterUrl: String?,
    override val year: Int?,
) : SearchResponse

data class TvSeriesSearchResponse(
    override val name: String,
    override val url: String,
    override val slug: String,
    override val apiName: String,
    override val type: TvType,

    override val posterUrl: String?,
    override val year: Int?,
    val episodes: Int?,
) : SearchResponse

interface LoadResponse {
    val name: String
    val url: String
    val apiName: String
    val type: TvType
    val posterUrl: String?
    val year: Int?
    val plot: String?
}

fun LoadResponse?.isEpisodeBased(): Boolean {
    if (this == null) return false
    return (this is AnimeLoadResponse || this is TvSeriesLoadResponse) && (this.type == TvType.TvSeries || this.type == TvType.Anime)
}

fun LoadResponse?.isAnimeBased(): Boolean {
    if (this == null) return false
    return (this.type == TvType.Anime || this.type == TvType.ONA) // && (this is AnimeLoadResponse)
}

data class AnimeEpisode(val url: String, val name : String? = null)

data class AnimeLoadResponse(
    val engName: String?,
    val japName: String?,
    override val name: String,
    override val url: String,
    override val apiName: String,
    override val type: TvType,

    override val posterUrl: String?,
    override val year: Int?,

    val dubEpisodes: ArrayList<AnimeEpisode>?,
    val subEpisodes: ArrayList<AnimeEpisode>?,
    val showStatus: ShowStatus?,

    override val plot: String?,
    val tags: ArrayList<String>? = null,
    val synonyms: ArrayList<String>? = null,

    val malId: Int? = null,
    val anilistId: Int? = null,
) : LoadResponse

data class MovieLoadResponse(
    override val name: String,
    override val url: String,
    override val apiName: String,
    override val type: TvType,
    val dataUrl: String,

    override val posterUrl: String?,
    override val year: Int?,
    override val plot: String?,

    val imdbId: Int?,
) : LoadResponse

data class TvSeriesEpisode(val name: String?, val season: Int?, val episode: Int?, val data: String)

data class TvSeriesLoadResponse(
    override val name: String,
    override val url: String,
    override val apiName: String,
    override val type: TvType,
    val episodes: ArrayList<TvSeriesEpisode>,

    override val posterUrl: String?,
    override val year: Int?,
    override val plot: String?,

    val showStatus: ShowStatus?,
    val imdbId: Int?,
) : LoadResponse