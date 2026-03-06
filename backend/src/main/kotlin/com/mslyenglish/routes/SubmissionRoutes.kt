package com.mslyenglish.routes

import com.mslyenglish.models.ApiMessage
import com.mslyenglish.models.SubmissionStatus
import com.mslyenglish.models.UpdateStatusRequest
import com.mslyenglish.services.SubmissionService
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.submissionRoutes(submissionService: SubmissionService) {
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
                }
                is PartData.FileItem -> if (part.name == "files") files.add(part) else part.dispose()
                else -> part.dispose()
            }
        }

        if (studentName.isBlank() || classGroup.isBlank() || homeworkTopic.isBlank()) {
            files.forEach { it.dispose() }
            call.respond(HttpStatusCode.BadRequest, ApiMessage("Required fields are missing"))
            return@post
        }

        runCatching { submissionService.createSubmission(studentName, classGroup, homeworkTopic, notes, files) }
            .onSuccess { call.respond(HttpStatusCode.Created, it) }
            .onFailure { call.respond(HttpStatusCode.BadRequest, ApiMessage(it.message ?: "Submission failed")) }
    }

    authenticate("auth-jwt") {
        route("/api/admin") {
            get("/submissions") {
                val statusRaw = call.request.queryParameters["status"]
                val status = statusRaw?.takeIf { it.isNotBlank() }?.let { SubmissionStatus.valueOf(it) }
                val data = submissionService.listSubmissions(
                    search = call.request.queryParameters["search"],
                    classGroup = call.request.queryParameters["classGroup"],
                    topic = call.request.queryParameters["homeworkTopic"],
                    status = status,
                    date = call.request.queryParameters["date"]
                )
                call.respond(data)
            }

            get("/submissions/{id}") {
                val id = call.parameters["id"]?.toLongOrNull() ?: return@get call.respond(HttpStatusCode.BadRequest, ApiMessage("Invalid id"))
                val item = submissionService.getSubmission(id)
                if (item == null) call.respond(HttpStatusCode.NotFound, ApiMessage("Not found")) else call.respond(item)
            }

            patch("/submissions/{id}/status") {
                val id = call.parameters["id"]?.toLongOrNull() ?: return@patch call.respond(HttpStatusCode.BadRequest, ApiMessage("Invalid id"))
                val body = call.receive<UpdateStatusRequest>()
                val updated = submissionService.updateStatus(id, body.status)
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
