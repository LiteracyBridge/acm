package org.literacybridge.tbloaderandroid.models

data class Release(
    val url: String? = null,
    val html_url: String? = null,
    val assets_url: String? = null,
    val upload_url: String? = null,
    val tarball_url: String? = null,
    val zipball_url: String? = null,
    val id: Float = 0f,
    val node_id: String? = null,
    val tag_name: String? = null,
    val target_commitish: String? = null,
    val name: String? = null,
    val body: String? = null,
    val draft: Boolean = false,
    val prerelease: Boolean = false,
    val created_at: String? = null,
    val published_at: String? = null,
    var assets: List<Asset> = emptyList()
)

data class Asset(
    val url: String,
    val browser_download_url: String,
    val id: Float,
    val node_id: String,
    val name: String,
    val label: String,
    val state: String,
    val content_type: String,
    val created_at: String,
    val size: Float,
    val download_count: Float,
)