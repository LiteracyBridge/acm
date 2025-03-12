package org.literacybridge.acm.store

import kotlin.properties.Delegates

enum class DeplomentPlatform {
    TalkingBook,
    CompanionApp
}

class AudioItemModel {
    var id by Delegates.notNull<Int>()
    lateinit var title: String
    lateinit var language: String
    var variant: String? = null
    lateinit var acm_id: String
    lateinit var type: String
    var playlist_title: String? = null // NB: Only populated in join queries

    enum class ItemType {
        PlaylistPrompt,
        SystemPrompt,
        Message
    }
}