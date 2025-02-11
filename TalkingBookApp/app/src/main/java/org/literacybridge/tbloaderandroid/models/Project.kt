package org.literacybridge.tbloaderandroid.models

data class Project(
    val active: Boolean,
    val code: String,
    val deployments: List<Deployment>,
    val id: Int,
    val name: String
)