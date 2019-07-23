package com.ampnet.integration.tests.backend

import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.jackson.responseObject
import kotlin.test.fail

object UserService {

    private const val userServiceUrl = "http://localhost:8125"

    fun getJwtToken(email: String, password: String): String {
        val response = Fuel.post("$userServiceUrl/token")
                .jsonBody("""{
                    |"login_method": "EMAIL",
                    |"credentials": {
                    |       "email": "$email",
                    |       "password": "$password"
                    |   }
                    |}
                """.trimMargin())
                .responseObject<AccessRefreshTokenResponse>(JsonMapper.mapper)
        return response.third.get().accessToken
    }

    /* Identyum */
    fun getIdentyumToken(): IdentyumToken {
        val response = Fuel.get("$userServiceUrl/identyum/token")
                .responseObject<IdentyumToken>(JsonMapper.mapper)
        if (response.second.statusCode != 200) fail("Could not get Identyum token")
        return response.third.get()
    }
}
