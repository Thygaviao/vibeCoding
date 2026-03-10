package com.mslyenglish.services

import com.mslyenglish.db.SubmissionFiles
import com.mslyenglish.db.Submissions
import com.mslyenglish.models.SubmissionCreatedResponse
import com.mslyenglish.models.SubmissionFileResponse
import com.mslyenglish.models.SubmissionResponse
import com.mslyenglish.models.SubmissionStatus
import io.ktor.http.content.PartData
import io.ktor.utils.io.core.readAvailable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*

class SubmissionService(private val uploadDir: String) {
    private val logger = LoggerFactory.getLogger(SubmissionService::class.java)

    private val allowedExtensions = setOf("jpg", "jpeg", "png", "webp", "pdf")
    private val allowedMimeTypes = setOf("image/jpeg", "image/png", "image/webp", "application/pdf")
    private val extensionToMimeTypes = mapOf(
        "jpg" to setOf("image/jpeg"),
        "jpeg" to setOf("image/jpeg"),
        "png" to setOf("image/png"),
        "webp" to setOf("image/webp"),
        "pdf" to setOf("application/pdf")
    )

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
        val uploadRoot = File(uploadDir)
        val attemptFolder = File(uploadRoot, UUID.randomUUID().toString())
        val savedFiles = mutableListOf<File>()

        try {
            require(files.isNotEmpty()) { "Invalid submission payload" }
            require(files.size <= maxFiles) { "Invalid submission payload" }

            if (!attemptFolder.exists()) attemptFolder.mkdirs()

            val preparedFiles = files.map { fileItem ->
                val original = fileItem.originalFileName ?: "upload"
                val ext = original.substringAfterLast('.', "").lowercase()
                require(ext in allowedExtensions) { "Invalid submission payload" }

                val contentType = fileItem.contentType?.withoutParameters()?.toString()?.lowercase()
                if (contentType != null) {
                    require(contentType in allowedMimeTypes) { "Invalid submission payload" }
                    require(contentType in (extensionToMimeTypes[ext] ?: emptySet())) { "Invalid submission payload" }
                }

                val stored = "${UUID.randomUUID()}.$ext"
                val destination = File(attemptFolder, stored)
                val size = streamCopyWithLimit(fileItem, destination, maxFileSize)
                savedFiles.add(destination)

                StoredFile(
                    originalFileName = original,
                    storedFileName = destination.relativeTo(uploadRoot).path,
                    mimeType = contentType ?: "application/octet-stream",
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

            return SubmissionCreatedResponse(submissionId)
        } catch (e: IllegalArgumentException) {
            cleanupSavedFiles(savedFiles, attemptFolder)
            throw e
        } catch (e: Exception) {
            logger.error("Submission save failed", e)
            cleanupSavedFiles(savedFiles, attemptFolder)
            throw RuntimeException("Submission processing failed")
        } finally {
            files.forEach { file ->
                runCatching { file.dispose() }
                    .onFailure { logger.warn("Failed to dispose multipart part", it) }
            }
        }
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
            val parsedDate = runCatching { LocalDate.parse(date) }
                .getOrElse { throw IllegalArgumentException("Invalid filter value") }
            val start = parsedDate.atStartOfDay().toInstant(ZoneOffset.UTC)
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

    private fun streamCopyWithLimit(fileItem: PartData.FileItem, destination: File, maxBytes: Long): Long {
        var total = 0L
        fileItem.provider().use { input ->
            destination.outputStream().use { output ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    val read = input.readAvailable(buffer, 0, buffer.size)
                    if (read <= 0) break
                    total += read.toLong()
                    require(total <= maxBytes) { "Invalid submission payload" }
                    output.write(buffer, 0, read)
                }
            }
        }
        return total
    }

    private fun cleanupSavedFiles(files: List<File>, folder: File) {
        files.forEach { file ->
            runCatching { if (file.exists()) file.delete() }
                .onFailure { logger.warn("Failed to delete uploaded file: ${file.path}", it) }
        }
        runCatching {
            if (folder.exists()) folder.deleteRecursively()
        }.onFailure {
            logger.warn("Failed to clean upload folder: ${folder.path}", it)
        }
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
