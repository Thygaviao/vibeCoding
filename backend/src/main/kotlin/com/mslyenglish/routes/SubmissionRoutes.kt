package com.mslyenglish.routes

import com.mslyenglish.models.ApiMessage
import com.mslyenglish.models.SubmissionStatus
import com.mslyenglish.services.SubmissionService
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory

@Serializable
data class UpdateStatusPayload(val status: String)

fun Route.submissionRoutes(submissionService: SubmissionService) {
    val logger = LoggerFactory.getLogger("SubmissionRoutes")

    post("/api/submissions") {
        val multipart = call.receiveMultipart()
        val files = mutableListOf<PartData.FileItem>()
        var studentName = ""
        var classGroup = ""
        var homeworkTopic = ""
        var notes: String? = null

        multipart.forEachPart { part: PartData ->
            when (part) {
                is PartData.FormItem -> when (part.name) {
                    "studentName" -> studentName = part.value
                    "classGroup" -> classGroup = part.value
                    "homeworkTopic" -> homeworkTopic = part.value
                    "notes" -> notes = part.value
                    else -> part.dispose()
                }
                is PartData.FileItem -> if (part.name == "files") files.add(part) else part.dispose()
                else -> part.dispose()
            }
        }

        if (studentName.isBlank() || classGroup.isBlank() || homeworkTopic.isBlank()) {
            files.forEach { it.dispose() }
            call.respond(HttpStatusCode.BadRequest, ApiMessage("Invalid submission payload"))
            return@post
        }

        runCatching { submissionService.createSubmission(studentName, classGroup, homeworkTopic, notes, files) }
            .onSuccess { call.respond(HttpStatusCode.Created, it) }
            .onFailure { error ->
                logger.warn("Submission request failed", error)
                val status = if (error is IllegalArgumentException) HttpStatusCode.BadRequest else HttpStatusCode.InternalServerError
                val message = if (status == HttpStatusCode.BadRequest) "Invalid submission payload" else "Submission failed"
                call.respond(status, ApiMessage(message))
            }
    }

    authenticate("auth-jwt") {
        route("/api/admin") {
            get("/submissions") {
                val statusRaw = call.request.queryParameters["status"]
                val status = parseStatus(statusRaw)
                if (statusRaw != null && statusRaw.isNotBlank() && status == null) {
                    call.respond(HttpStatusCode.BadRequest, ApiMessage("Invalid status value"))
                    return@get
                }

                val data = runCatching {
                    submissionService.listSubmissions(
                        search = call.request.queryParameters["search"],
                        classGroup = call.request.queryParameters["classGroup"],
                        topic = call.request.queryParameters["homeworkTopic"],
                        status = status,
                        date = call.request.queryParameters["date"]
                    )
                }.getOrElse { error ->
                    logger.warn("Failed to list submissions", error)
                    if (error is IllegalArgumentException) {
                        call.respond(HttpStatusCode.BadRequest, ApiMessage("Invalid filter value"))
                    } else {
                        call.respond(HttpStatusCode.InternalServerError, ApiMessage("Failed to load submissions"))
                    }
                    return@get
                }

                call.respond(data)
            }

            get("/submissions/{id}") {
                val id = call.parameters["id"]?.toLongOrNull() ?: return@get call.respond(HttpStatusCode.BadRequest, ApiMessage("Invalid id"))
                val item = submissionService.getSubmission(id)
                if (item == null) call.respond(HttpStatusCode.NotFound, ApiMessage("Not found")) else call.respond(item)
            }

            patch("/submissions/{id}/status") {
                val id = call.parameters["id"]?.toLongOrNull() ?: return@patch call.respond(HttpStatusCode.BadRequest, ApiMessage("Invalid id"))
                val body = runCatching { call.receive<UpdateStatusPayload>() }
                    .getOrElse {
                        call.respond(HttpStatusCode.BadRequest, ApiMessage("Invalid request body"))
                        return@patch
                    }

                val status = parseStatus(body.status)
                if (status == null) {
                    call.respond(HttpStatusCode.BadRequest, ApiMessage("Invalid status value"))
                    return@patch
                }

                val updated = submissionService.updateStatus(id, status)
                if (updated) call.respond(ApiMessage("Updated")) else call.respond(HttpStatusCode.NotFound, ApiMessage("Not found"))
            }

            get("/files/{id}/download") {
                val id = call.parameters["id"]?.toLongOrNull() ?: return@get call.respond(HttpStatusCode.BadRequest, ApiMessage("Invalid id"))
                val file = submissionService.getFileById(id)
                if (file == null || !file.first.exists()) return@get call.respond(HttpStatusCode.NotFound, ApiMessage("File not found"))
                call.response.header(HttpHeaders.ContentDisposition, ContentDisposition.Attachment.withParameter(ContentDisposition.Parameters.FileName, file.second).toString())
                call.respondFile(file.first)
            }
        }
    }
}

private fun parseStatus(value: String?): SubmissionStatus? {
    if (value.isNullOrBlank()) return null
    return SubmissionStatus.entries.firstOrNull { it.name == value }
}
