package org.literacybridge.acm.store

import kotlin.properties.Delegates

enum class RGB { RED, GREEN, BLUE }


class AudioItemModel {
    var id by Delegates.notNull<Int>()
    lateinit var title: String
    lateinit var language: String
    lateinit var variant: String
    lateinit var acm_id: String
    lateinit var type: String
    lateinit var playlist_title: String // NB: Only populated in join queries

    enum class ItemType {
        PlaylistPrompt,
        SystemPrompt,
        Message
    }
}