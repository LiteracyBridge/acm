package org.literacybridge.tbloaderandroid.view_models

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amplifyframework.core.Amplify
import dagger.hilt.android.lifecycle.HiltViewModel
import io.sentry.Sentry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.literacybridge.tbloaderandroid.database.AppDatabase
import org.literacybridge.tbloaderandroid.database.S3SyncEntity
import org.literacybridge.tbloaderandroid.database.S3SyncEntityDao
import javax.inject.Inject


@HiltViewModel
class UploadStatusViewModel @Inject constructor() : ViewModel() {
    val files: MutableLiveData<List<S3SyncEntity>> = MutableLiveData(listOf())
    val inProgressFiles: MutableLiveData<List<S3SyncEntity>> = MutableLiveData(listOf())

    suspend fun queryFiles() {
        viewModelScope.launch(Dispatchers.IO) {
            val data = AppDatabase.getDatabase().s3SyncDoa().getAll()
            files.postValue(data)
            inProgressFiles.postValue(data.filter { it.status == S3SyncEntityDao.S3SyncStatus.Uploading })

            // Query amplify storage for pending transfers
            inProgressFiles.value?.forEach { file ->
                Amplify.Storage.getTransfer(
                    file.awsTransferId,
                    { operation ->
                        Log.i("MyAmplifyApp", "Current State" + operation.transferState)
                        // set listener to receive updates
                        operation.setOnProgress {
                            viewModelScope.launch {
                                AppDatabase.getDatabase().s3SyncDoa()
                                    .updateProgress(file.awsTransferId, it.currentBytes)
                            }
                        }
                        operation.setOnSuccess {
                            viewModelScope.launch {
                                AppDatabase.getDatabase().s3SyncDoa()
                                    .uploadCompleted(file.awsTransferId)
                            }
                        }

                        operation.setOnError { err ->
                            Sentry.captureException(err)

                            viewModelScope.launch {
                                AppDatabase.getDatabase().s3SyncDoa()
                                    .updateStatus(
                                        file.awsTransferId,
                                        S3SyncEntityDao.S3SyncStatus.Failed
                                    )
                            }
                        }
                    },
                    { err ->
                        Sentry.captureException(err)

                        // Completed transfers are deleted from aws db, causing error when querying by transferId
                        // When this happens, we assume the transfer is completed
                        if (err.recoverySuggestion.contains("transfer id is valid and the transfer is not completed")) {
                            viewModelScope.launch {
                                AppDatabase.getDatabase().s3SyncDoa()
                                    .uploadCompleted(file.awsTransferId)
                            }
                        }
                    }
                )
            }
        }
    }

//    val files = MutableLiveData<LiveData<List<S3SyncEntity>>>()
//
//
//    init {
//        files.observe.observe(lifecycleOwner) {
//            portfolioDataMap.clear()
//            portfolioDataMap.putAll(it)
//        }
//    }
//    val viewModel = ViewModelProvider(this)[UploadStatusViewModel::class.java]
//    viewModel.fi.observe(viewLifecycleOwner) {
//    }

//    val nameObserver: Observer<String?> = object : Observer<String?>() {
//        fun onChanged(@Nullable newName: String?) {
//            // Update the UI, in this case, a TextView.
//            mNameTextView.setText(newName)
//        }
//    }


}
