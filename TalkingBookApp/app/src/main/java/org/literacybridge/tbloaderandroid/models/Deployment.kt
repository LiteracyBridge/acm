package org.literacybridge.tbloaderandroid.models

data class Deployment(
    val comment: Any,
    val component: Any,
    val deployment: String,
    val deploymentname: String,
    val deploymentnumber: Int,
    val distribution: Any,
    val end_date: String,
    val id: Int,
    val project_id: String,
    val start_date: String
)