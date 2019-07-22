package com.ampnet.integration.tests.util

import com.zaxxer.hikari.HikariDataSource
import kotliquery.HikariCP
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import java.util.UUID
import kotlin.random.Random

object DatabaseUtil {

    private const val databaseUrl = "jdbc:postgresql://localhost:5432"

    private val backendDatabase: HikariDataSource by lazy {
        HikariCP.default("$databaseUrl/crowdfunding", "crowdfunding", "password")
    }
    private val blockchainDatabase: HikariDataSource by lazy {
        HikariCP.default("$databaseUrl/blockchain", "blockchain", "password")
    }
    private val userServiceDatabase: HikariDataSource by lazy {
        HikariCP.default("$databaseUrl/user_service", "user_service", "password")
    }

    const val defaultUserPassword = "abcdefgh"
    private const val adminRoleId = 1
    private const val orgAdminRoleId = 1

    fun cleanUserServiceDb() {
        clearUsers()
    }

    fun cleanBackendDb() {
        clearWallets()
        clearOrganizations()
        clearProjects()
    }

    fun cleanBlockchainDb() {
        truncateTable(blockchainDatabase, "transaction")
        truncateTable(blockchainDatabase, "wallet")
    }

    fun insertUserInDb(email: String, userUuid: UUID) {
        val id = getRandomIntForDbId()
        using(sessionOf(userServiceDatabase)) { session ->
            session.run(queryOf("insert into user_info values (" +
                    "$id, 'web_session_uuid', 'verified_email@mail.com', '+385', 'HRV', '1978-03-02', " +
                    "'ae1ee02d-8a2d-4c50-a6ca-8f0454e19f6d', 'PERSONAL_ID_CARD', '48077962579', " +
                    "'first', 'last', 'HRV', true, 'city', 'county', 'street', " +
                    "now(), true)").asExecute)

            session.run(queryOf("insert into app_user values ('$userUuid', '$email', " +
                    "'\$2a\$10\$cHyZss0hacXYrqxmVgsZ2.43ZbnW/Fey2wh1zOUtjfeOZ20loEFyq', " +
                    "$adminRoleId, now(), 'EMAIL', true, $id)").asExecute)
        }
    }

    fun insertOrganizationInDb(name: String, userUuid: UUID) {
        val id = getRandomIntForDbId()
        using(sessionOf(backendDatabase)) { session ->
            session.run(queryOf("insert into organization values (" +
                    "$id, '$name', '$userUuid', now(), null, true, '$userUuid', null, null)").asExecute)
        }
    }

    fun insertOrganizationMembershipInDb(userUuid: UUID, organizationId: Int) {
        val id = getRandomIntForDbId()
        using(sessionOf(backendDatabase)) { session ->
            session.run(queryOf("insert into organization_membership values (" +
                    "$id, $organizationId, '$userUuid', $orgAdminRoleId, now())").asExecute)
        }
    }

    fun insertProjectInDb(name: String, userUuid: UUID, organizationId: Int) {
        val id = getRandomIntForDbId()
        using(sessionOf(backendDatabase)) { session ->
            session.run(queryOf("insert into project values (" +
                    "$id, $organizationId, '$name', 'description', " +
                    "'location', 'location_text', '0-1%', " +
                    "now(), now()+ interval '9 day', " +
                    "1000000, 'EUR', 100, 100000, " +
                    "null, null, null, '$userUuid', now(), null, true)").asExecute)
        }
    }

    fun getOrganizationIdForName(name: String): Int? = getItemIdForName(name, "organization")

    fun getProjectIdForName(name: String): Int? = getItemIdForName(name, "project")

    private fun getItemIdForName(name: String, table: String): Int? {
        val query = queryOf("select id from $table where name='$name'")
                .map { row -> row.int("id") }
                .asSingle
        var id: Int? = null
        using(sessionOf(backendDatabase)) { session ->
            id = session.run(query)
        }
        return id
    }

    private fun clearUsers() {
        truncateTable(userServiceDatabase, "user_info")
        truncateTable(userServiceDatabase, "app_user")
    }

    private fun clearWallets() {
        truncateTable(backendDatabase, "wallet")
    }

    private fun clearOrganizations() {
        truncateTable(backendDatabase, "organization")
    }

    private fun clearProjects() {
        truncateTable(backendDatabase, "project")
    }

    private fun truncateTable(database: HikariDataSource, table: String) {
        using(sessionOf(database)) { session ->
            session.run(queryOf("TRUNCATE $table CASCADE").asExecute)
        }
    }

    private fun getRandomIntForDbId() = Random.nextInt(1, 10_000)
}
