package org.literacybridge.talkingbookapp.api_services

import org.literacybridge.talkingbookapp.models.ApiResponseModel
import org.literacybridge.talkingbookapp.models.UserModel
import retrofit2.http.GET

interface UserApiService {
    @GET("/users/me")
    suspend fun me(
    ): ApiResponseModel<UserModel>
}