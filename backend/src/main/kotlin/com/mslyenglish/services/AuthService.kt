package com.mslyenglish.services

import at.favre.lib.crypto.bcrypt.BCrypt
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.mslyenglish.config.JwtConfig
import com.mslyenglish.db.AdminUsers
import com.mslyenglish.models.AdminMeResponse
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.util.*

class AuthService(private val jwtConfig: JwtConfig) {
    private val algorithm = Algorithm.HMAC256(jwtConfig.secret)

    fun ensureDefaultAdmin(username: String, password: String) {
        transaction {
            val existing = AdminUsers.selectAll().where { AdminUsers.username eq username }.singleOrNull()
            if (existing == null) {
                AdminUsers.insert {
                    it[AdminUsers.username] = username
                    it[passwordHash] = hashPassword(password)
                    it[createdAt] = Instant.now()
                }
            }
        }
    }

    fun login(username: String, password: String): String? = transaction {
        val row = AdminUsers.selectAll().where { AdminUsers.username eq username }.singleOrNull() ?: return@transaction null
        val ok = BCrypt.verifyer().verify(password.toCharArray(), row[AdminUsers.passwordHash]).verified
        if (!ok) return@transaction null
        generateToken(row)
    }

    fun me(userId: Long): AdminMeResponse? = transaction {
        AdminUsers.selectAll().where { AdminUsers.id eq userId }.singleOrNull()?.let {
            AdminMeResponse(id = it[AdminUsers.id].value, username = it[AdminUsers.username])
        }
    }

    private fun hashPassword(raw: String): String = BCrypt.withDefaults().hashToString(12, raw.toCharArray())

    private fun generateToken(row: ResultRow): String = JWT.create()
        .withIssuer(jwtConfig.issuer)
        .withAudience(jwtConfig.audience)
        .withClaim("userId", row[AdminUsers.id].value)
        .withExpiresAt(Date(System.currentTimeMillis() + jwtConfig.expiresInMs))
        .sign(algorithm)
}
