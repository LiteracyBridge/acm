package org.literacybridge.talkingbookapp.models

data class UserProgramModel(
    val created_at: String,
    val program_id: Int,
    val updated_at: String,
    val user_id: Int,
    val program: Program
)

data class UserModel(
    val email: String,
    val first_name: String,
    val id: Int,
    val last_name: String?,
    val organisation: Organisation,
    val organisation_id: Int,
    val permissions: Map<String, Boolean>,
    val phone_number: String?,
    val programs: List<UserProgramModel>,
    val roles: List<Any>
)