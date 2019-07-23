package com.ampnet.integration.tests.integration

import com.ampnet.integration.tests.BaseTest
import com.ampnet.integration.tests.backend.UserService
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertNotNull

@Ignore("Identyum side is not working")
class IdentyumIntegrationTest : BaseTest() {

    @Test
    fun mustBeAbleToGetIdentyumToken() {
        verify("User can get Identyum token") {
            val token = UserService.getIdentyumToken()
            assertNotNull(token, "Identyum token must not be null")
        }
    }
}
