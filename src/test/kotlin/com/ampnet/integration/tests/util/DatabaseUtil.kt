package com.ampnet.integration.tests.util

import com.zaxxer.hikari.HikariDataSource
import kotliquery.HikariCP
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import kotlin.random.Random

object DatabaseUtil {

    private const val databaseUrl = "jdbc:postgresql://localhost:5432"

    private val backendDatabase: HikariDataSource by lazy {
        HikariCP.default("$databaseUrl/crowdfunding", "crowdfunding", "password")
    }
    private val blockchainDatabase: HikariDataSource by lazy {
        HikariCP.default("$databaseUrl/blockchain", "blockchain", "password")
    }

    const val defaultUserPassword = "abcdefgh"
    private const val adminRoleId = 1
    private const val orgAdminRoleId = 3
    private const val croatiaId = 3

    fun clearUsers() {
        truncateTable(backendDatabase, "app_user")
    }

    fun clearWallets() {
        truncateTable(backendDatabase, "wallet")
    }

    fun clearOrganizations() {
        truncateTable(backendDatabase, "organization")
    }

    fun clearProjects() {
        truncateTable(backendDatabase, "project")
    }

    fun cleanBackendDb() {
        clearUsers()
        clearWallets()
        clearOrganizations()
        clearProjects()
    }

    fun cleanBlockchainDb() {
        truncateTable(blockchainDatabase, "transaction")
        truncateTable(blockchainDatabase, "wallet")
    }

    fun insertUserInDb(email: String) {
        val id = getRandomIntForDbId()
        using(sessionOf(backendDatabase)) { session ->
            session.run(queryOf("insert into app_user values ($id, '$email', '\$2a\$10\$khTDWUCcJ1Wff1D8lskfl.HO2PaC3MWOqwIx.ErMpuk/3K5.KM2Oa', 'first', 'last', $croatiaId, '+385', $adminRoleId, now(), 'EMAIL', true, null)").asExecute)
        }
    }

    fun insertOrganizationInDb(name: String, userId: Int) {
        val id = getRandomIntForDbId()
        using(sessionOf(backendDatabase)) { session ->
            session.run(queryOf("insert into organization values ($id, '$name', $userId, now(), null, true, $userId, null, null)").asExecute)
        }
    }

    fun insertOrganizationMembershipInDb(userId: Int, organizationId: Int) {
        using(sessionOf(backendDatabase)) { session ->
            session.run(queryOf("insert into organization_membership values ($organizationId, $userId, $orgAdminRoleId, now())").asExecute)
        }
    }

    fun insertProjectInDb(name: String, userId: Int, organizationId: Int) {
        val id = getRandomIntForDbId()
        using(sessionOf(backendDatabase)) { session ->
            session.run(queryOf("insert into project values ($id, $organizationId, '$name', 'description', " +
                    "'location', 'location_text', '0-1%', " +
                    "now(), now()+ interval '9 day', " +
                    "1000000, 'EUR', 100, 100000, " +
                    "null, null, $userId, now(), null, true)").asExecute)
        }
    }

    fun getUserIdForEmail(email: String): Int? {
        val query = queryOf("select id from app_user where email='$email'")
                .map { row -> row.int("id") }
                .asSingle
        var id: Int? = null
        using(sessionOf(backendDatabase)) { session ->
            id = session.run(query)
        }
        return id
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

    private fun truncateTable(database: HikariDataSource, table: String) {
        using(sessionOf(database)) { session ->
            session.run(queryOf("TRUNCATE $table CASCADE").asExecute)
        }
    }

    private fun getRandomIntForDbId() = Random.nextInt(1, 10_000)
}
