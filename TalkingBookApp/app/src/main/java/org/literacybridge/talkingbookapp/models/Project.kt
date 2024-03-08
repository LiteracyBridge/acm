package org.literacybridge.talkingbookapp.models

data class Project(
    val active: Boolean,
    val code: String,
    val deployments: List<Deployment>,
    val id: Int,
    val name: String
)