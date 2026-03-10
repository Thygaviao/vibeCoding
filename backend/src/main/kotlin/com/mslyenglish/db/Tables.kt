package com.mslyenglish.db

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.javatime.timestamp

object AdminUsers : LongIdTable("admin_users") {
    val username = varchar("username", 100).uniqueIndex()
    val passwordHash = varchar("password_hash", 255)
    val createdAt = timestamp("created_at")
}

object Submissions : LongIdTable("submissions") {
    val studentName = varchar("student_name", 120)
    val classGroup = varchar("class_group", 120)
    val homeworkTopic = varchar("homework_topic", 200)
    val notes = text("notes").nullable()
    val status = varchar("status", 30)
    val createdAt = timestamp("created_at")
}

object SubmissionFiles : LongIdTable("submission_files") {
    val submissionId = reference("submission_id", Submissions)
    val originalFileName = varchar("original_file_name", 255)
    val storedFileName = varchar("stored_file_name", 255)
    val mimeType = varchar("mime_type", 100)
    val sizeBytes = long("size_bytes")
    val uploadedAt = timestamp("uploaded_at")
}
