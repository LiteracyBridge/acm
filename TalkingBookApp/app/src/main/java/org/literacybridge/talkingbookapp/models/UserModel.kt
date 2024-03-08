package org.literacybridge.talkingbookapp.models

data class UserProgramModel(
    val created_at: String,
    val program_id: Int,
    val updated_at: String,
    val user_id: Int,
    val program: Program
)

data class UserModel(
    val email: String? = null,
    val first_name: String? = null,
    val id: Int? = null,
    val last_name: String? = null,
    val organisation: Organisation? = null,
    val organisation_id: Int? = null,
    val permissions: Map<String, Boolean>? = null,
    val phone_number: String? = null,
    val programs: List<UserProgramModel> = listOf(),
//    val roles: List<Any> = intArrayOf()
)