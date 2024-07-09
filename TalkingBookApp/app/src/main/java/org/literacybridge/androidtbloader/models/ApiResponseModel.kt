package org.literacybridge.androidtbloader.models

data class ApiResponseModel<T>(
    val data: List<T>
)

data class TalkingBookSerial(
    val begin: Int?,
    val end: Int?,
    val n: Int?,
    val id: Int?,
    val hexid: String?,
    val msec: Float?,
    val status: String?
)