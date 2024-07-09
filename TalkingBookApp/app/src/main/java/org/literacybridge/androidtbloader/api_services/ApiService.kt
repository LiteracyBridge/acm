package org.literacybridge.androidtbloader.api_services

import org.literacybridge.androidtbloader.models.ApiResponseModel
import org.literacybridge.androidtbloader.models.TalkingBookSerial
import org.literacybridge.androidtbloader.models.UserModel
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {
    @GET("users/me")
    suspend fun getUser(): ApiResponseModel<UserModel>

    @GET("tb-loader/reserve")
    suspend fun reserveSerialNumber(@Query("n") n: Int?): ApiResponseModel<TalkingBookSerial>
}