package org.literacybridge.talkingbookapp.view_models

import Screen
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import org.literacybridge.talkingbookapp.api_services.NetworkModule
import org.literacybridge.talkingbookapp.helpers.DataStoreManager
import org.literacybridge.talkingbookapp.helpers.LOG_TAG
import org.literacybridge.talkingbookapp.helpers.dataStoreManager
import javax.inject.Inject

@HiltViewModel
class UserViewModel @Inject constructor() : ViewModel() {
//    private val _user = mutableStateOf(UserModel())
//    val users: State<ApiState<List<User>>> = _users

    fun setToken(token: String, navController: NavController) {
        viewModelScope.launch {
            dataStoreManager.setAccessToken(token)
        }.invokeOnCompletion {
            viewModelScope.launch {
                try {
                    // TODO; If the user is set, don't trigger api call
                    // FIXME: the function is executed multiple times due to ui re-rendering
                    val response = NetworkModule().instance().getUser()
                    navController.navigate(Screen.HOME.name);
                    Log.d(LOG_TAG, "${response.data}")
                } catch (e: Exception) {
                    Log.d(LOG_TAG, "$e")
                }
            }

        }
    }


//    fun fetchUser() {
//        viewModelScope.launch {
//            try {
////                var service = NetworkModule()
////                val retro = Retrofit.Builder()
////                    .baseUrl(API_URL)
////                    .client(service.provideOkHttpClient())
////                    .addConverterFactory(GsonConverterFactory.create())
////                    .build()
//                val response = NetworkModule().instance().getUser()
////                return response.data
//                Log.d(LOG_TAG, "${response.data}")
//            } catch (e: Exception) {
//                Log.d(LOG_TAG, "$e")
////                _users.value = ApiState.Error("Failed to fetch data")
//            }
//        }
//    }

    fun test() {
        Log.d(LOG_TAG, "View model working")
    }
//
//    private fun getDetails(
////        ctx: Context
//    ) {
////        var url = "https://reqres.in/api/"
//        val api = Retrofit.Builder()
//            .baseUrl(API_URL)
//        .addConverterFactory(GsonConverterFactory.create())
//            .client(httpClient)
//            .build()
//            .create(UserApiService::class.java)
////            .me()
////        // below the line is to create an instance for our retrofit api class.
////        val retrofitAPI = retrofit.create(UserApiService::class.java)
//        // passing data from our text fields to our model class.
////        val dataModel = DataModel(userName.value.text, job.value.text)
//        // calling a method to create an update and passing our model class.
//        val call: ApiResponseModel<UserModel> = api.me()
//        // on below line we are executing our method.
//
//        call!!.enqueue(object : Callback<ApiResponseModel<UserModel>> {
//            override fun onResponse(call: Call<DataModel?>?, response: Response<DataModel?>) {
//                // this method is called when we get response from our api.
//                Toast.makeText(ctx, "Data posted to API", Toast.LENGTH_SHORT).show()
//                // we are getting a response from our body and
//                // passing it to our model class.
//                val model: DataModel? = response.body()
//                // on below line we are getting our data from model class
//                // and adding it to our string.
//                val resp =
//                    "Response Code : " + response.code() + "\n" + "User Name : " + model!!.name + "\n" + "Job : " + model!!.job
//                // below line we are setting our string to our response.
//                result.value = resp
//            }
//
//            override fun onFailure(call: Call<DataModel?>?, t: Throwable) {
//                // we get error response from API.
//                result.value = "Error found is : " + t.message
//            }
//        })
//
//    }
}