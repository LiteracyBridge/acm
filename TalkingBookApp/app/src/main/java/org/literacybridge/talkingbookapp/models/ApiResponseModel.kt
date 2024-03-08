package org.literacybridge.talkingbookapp.models

data class ApiResponseModel<T>(
    val data: List<T>
)