package com.mslyenglish.services

import com.mslyenglish.db.SubmissionFiles
import com.mslyenglish.db.Submissions
import com.mslyenglish.models.SubmissionCreatedResponse
import com.mslyenglish.models.SubmissionFileResponse
import com.mslyenglish.models.SubmissionResponse
import com.mslyenglish.models.SubmissionStatus
import io.ktor.http.content.PartData
import io.ktor.utils.io.core.readBytes
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*

class SubmissionService(private val uploadDir: String) {
    private val allowedExtensions = setOf("jpg", "jpeg", "png", "webp", "pdf")
    private val maxFileSize = 25L * 1024L * 1024L
    private val maxFiles = 10

    init {
        File(uploadDir).mkdirs()
    }

    fun createSubmission(
        studentName: String,
        classGroup: String,
        homeworkTopic: String,
        notes: String?,
        files: List<PartData.FileItem>
    ): SubmissionCreatedResponse {
        require(files.isNotEmpty()) { "At least one file is required" }
        require(files.size <= maxFiles) { "Maximum 10 files allowed" }

        val preparedFiles = files.map { fileItem ->
            val original = fileItem.originalFileName ?: "upload"
            val ext = original.substringAfterLast('.', "").lowercase()
            require(ext in allowedExtensions) { "File type not allowed: $original" }
            val stored = "${UUID.randomUUID()}.$ext"
            val submissionFolder = File(uploadDir, Instant.now().epochSecond.toString())
            submissionFolder.mkdirs()
            val destination = File(submissionFolder, stored)
            var size = 0L
            val bytes = fileItem.provider().readBytes()
            size = bytes.size.toLong()
            require(size <= maxFileSize) { "File too large: $original" }
            destination.writeBytes(bytes)
            StoredFile(
                originalFileName = original,
                storedFileName = destination.relativeTo(File(uploadDir)).path,
                mimeType = fileItem.contentType?.toString() ?: "application/octet-stream",
                sizeBytes = size
            )
        }

        val submissionId = transaction {
            val id = Submissions.insertAndGetId {
                it[Submissions.studentName] = studentName.trim()
                it[Submissions.classGroup] = classGroup.trim()
                it[Submissions.homeworkTopic] = homeworkTopic.trim()
                it[Submissions.notes] = notes?.trim()?.ifEmpty { null }
                it[status] = SubmissionStatus.NEW.name
                it[createdAt] = Instant.now()
            }.value
            preparedFiles.forEach { f ->
                SubmissionFiles.insert {
                    it[submissionId] = id
                    it[originalFileName] = f.originalFileName
                    it[storedFileName] = f.storedFileName
                    it[mimeType] = f.mimeType
                    it[sizeBytes] = f.sizeBytes
                    it[uploadedAt] = Instant.now()
                }
            }
            id
        }
        files.forEach { it.dispose() }
        return SubmissionCreatedResponse(submissionId)
    }

    fun listSubmissions(
        search: String?,
        classGroup: String?,
        topic: String?,
        status: SubmissionStatus?,
        date: String?
    ): List<SubmissionResponse> = transaction {
        var query: Query = Submissions.selectAll()
        if (!search.isNullOrBlank()) query = query.andWhere { Submissions.studentName like "%$search%" }
        if (!classGroup.isNullOrBlank()) query = query.andWhere { Submissions.classGroup eq classGroup }
        if (!topic.isNullOrBlank()) query = query.andWhere { Submissions.homeworkTopic eq topic }
        if (status != null) query = query.andWhere { Submissions.status eq status.name }
        if (!date.isNullOrBlank()) {
            val start = java.time.LocalDate.parse(date).atStartOfDay().toInstant(java.time.ZoneOffset.UTC)
            val end = start.plusSeconds(86400)
            query = query.andWhere { Submissions.createdAt greaterEq start and (Submissions.createdAt less end) }
        }

        query.orderBy(Submissions.createdAt, SortOrder.DESC).map { it.toSubmissionResponse() }
    }

    fun getSubmission(id: Long): SubmissionResponse? = transaction {
        val row = Submissions.selectAll().where { Submissions.id eq id }.singleOrNull() ?: return@transaction null
        val files = SubmissionFiles.selectAll().where { SubmissionFiles.submissionId eq id }.map {
            SubmissionFileResponse(
                id = it[SubmissionFiles.id].value,
                originalFileName = it[SubmissionFiles.originalFileName],
                mimeType = it[SubmissionFiles.mimeType],
                sizeBytes = it[SubmissionFiles.sizeBytes],
                uploadedAt = it[SubmissionFiles.uploadedAt].formatIso()
            )
        }
        row.toSubmissionResponse(files)
    }

    fun updateStatus(id: Long, status: SubmissionStatus): Boolean = transaction {
        Submissions.update({ Submissions.id eq id }) { it[Submissions.status] = status.name } > 0
    }

    fun getFileById(id: Long): Pair<File, String>? = transaction {
        val row = SubmissionFiles.selectAll().where { SubmissionFiles.id eq id }.singleOrNull() ?: return@transaction null
        Pair(File(uploadDir, row[SubmissionFiles.storedFileName]), row[SubmissionFiles.originalFileName])
    }

    private fun ResultRow.toSubmissionResponse(files: List<SubmissionFileResponse> = emptyList()) = SubmissionResponse(
        id = this[Submissions.id].value,
        studentName = this[Submissions.studentName],
        classGroup = this[Submissions.classGroup],
        homeworkTopic = this[Submissions.homeworkTopic],
        notes = this[Submissions.notes],
        status = SubmissionStatus.valueOf(this[Submissions.status]),
        createdAt = this[Submissions.createdAt].formatIso(),
        files = files
    )

    private fun Instant.formatIso(): String = DateTimeFormatter.ISO_INSTANT.withZone(ZoneOffset.UTC).format(this)

    data class StoredFile(
        val originalFileName: String,
        val storedFileName: String,
        val mimeType: String,
        val sizeBytes: Long
    )
}
