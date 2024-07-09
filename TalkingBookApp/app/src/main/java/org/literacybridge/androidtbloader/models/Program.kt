package org.literacybridge.androidtbloader.models

data class Program(
    val country: String,
    val deployments_count: Int,
    val deployments_first: String,
    val deployments_length: String,
    val direct_beneficiaries_additional_map: DirectBeneficiariesAdditionalMap,
    val direct_beneficiaries_map: DirectBeneficiariesMap,
    val feedback_frequency: String,
    val id: Int,
    val languages: List<String>,
    val listening_models: List<String>,
    val partner: String,
    val program_id: String,
    val project: Project,
    val region: List<String>,
    val sustainable_development_goals: List<Any>
)