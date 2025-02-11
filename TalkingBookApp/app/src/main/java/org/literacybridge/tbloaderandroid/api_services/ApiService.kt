package org.literacybridge.tbloaderandroid.api_services

import org.literacybridge.tbloaderandroid.models.ApiResponseModel
import org.literacybridge.tbloaderandroid.models.TalkingBookSerial
import org.literacybridge.tbloaderandroid.models.UserModel
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {
    @GET("users/me")
    suspend fun getUser(): ApiResponseModel<UserModel>

    @GET("tb-loader/reserve")
    suspend fun reserveSerialNumber(@Query("n") n: Int?): ApiResponseModel<TalkingBookSerial>
}