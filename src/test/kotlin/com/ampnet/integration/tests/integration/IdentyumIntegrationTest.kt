package com.ampnet.integration.tests.integration

import com.ampnet.integration.tests.BaseTest
import com.ampnet.integration.tests.backend.BackendService
import kotlin.test.Test
import kotlin.test.assertNotNull

class IdentyumIntegrationTest : BaseTest() {

    @Test
    fun mustBeAbleToGetIdentyumToken() {
        verify("User can get Identyum token") {
            val token = BackendService.getIdentyumToken()
            assertNotNull(token, "Identyum token must not be null")
        }
    }
}
