package com.mslyenglish.models

import kotlinx.serialization.Serializable

@Serializable
enum class SubmissionStatus { NEW, REVIEWED, ARCHIVED }

@Serializable
data class SubmissionFileResponse(
    val id: Long,
    val originalFileName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val uploadedAt: String
)

@Serializable
data class SubmissionResponse(
    val id: Long,
    val studentName: String,
    val classGroup: String,
    val homeworkTopic: String,
    val notes: String?,
    val status: SubmissionStatus,
    val createdAt: String,
    val files: List<SubmissionFileResponse> = emptyList()
)

@Serializable
data class LoginRequest(val username: String, val password: String)

@Serializable
data class LoginResponse(val token: String)

@Serializable
data class AdminMeResponse(val id: Long, val username: String)

@Serializable
data class UpdateStatusRequest(val status: SubmissionStatus)

@Serializable
data class ApiMessage(val message: String)

@Serializable
data class SubmissionCreatedResponse(val id: Long)
