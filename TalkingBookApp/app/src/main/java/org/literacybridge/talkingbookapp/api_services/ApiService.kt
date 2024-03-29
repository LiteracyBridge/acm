package org.literacybridge.talkingbookapp.api_services

import org.literacybridge.talkingbookapp.models.ApiResponseModel
import org.literacybridge.talkingbookapp.models.TalkingBookSerial
import org.literacybridge.talkingbookapp.models.UserModel
import retrofit2.http.GET
import retrofit2.http.Query

//class ApiService {
//    public static
//fun create() {
//
//val httpClient = OkHttpClient.Builder().addInterceptor { chain ->
//    val original = chain.request()
//    val requestBuilder = original.newBuilder()
//        .header("Accept", "application/json")
//        .header("Content-Type", "application/json")
//        .header("Authorization", "Bearer ${dataStoreManager.getAccessToken()}")
//        .method(original.method, original.body)
//
//    val request = requestBuilder.build()
//    chain.proceed(request)
//}.build()

//        val httpClient: Builder = new Builder ();
//        httpClient.addInterceptor(new Interceptor () {
//            @Override
//            public Response intercept(Interceptor.Chain chain) throws IOException {
//                Request original = chain . request ();
//
//                Request request = original . newBuilder ()
//                    .header("User-Agent", "Your-App-Name")
//                    .header("Accept", "application/vnd.yourapi.v1.full+json")
//                    .method(original.method(), original.body())
//                    .build();
//
//                return chain.proceed(request);
//            }
//        }
//}


//}

interface ApiService {
    @GET("users/me")
    suspend fun getUser(): ApiResponseModel<UserModel>

    @GET("tb-loader/reserve")
    suspend fun reserveSerialNumber(@Query("n") n: Int?): ApiResponseModel<TalkingBookSerial>
}