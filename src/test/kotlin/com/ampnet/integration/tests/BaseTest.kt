package com.ampnet.integration.tests

import org.junit.AfterClass
import org.junit.BeforeClass
import org.testcontainers.containers.DockerComposeContainer
import org.testcontainers.containers.wait.strategy.Wait
import java.io.File

abstract class BaseTest {

    protected fun suppose(@Suppress("UNUSED_PARAMETER") description: String, function: () -> Unit) {
        function.invoke()
    }

    protected fun verify(@Suppress("UNUSED_PARAMETER") description: String, function: () -> Unit) {
        function.invoke()
    }

    companion object {
        private val instance: KDockerComposeContainer by lazy { defineDockerCompose()}
        class KDockerComposeContainer(file: File) : DockerComposeContainer<KDockerComposeContainer>(file)

        private fun defineDockerCompose() = KDockerComposeContainer(File("src/test/resources/compose-test.yml"))
                .waitingFor("backend-service", Wait.forHttp("/actuator/health").forStatusCode(200))
                .waitingFor("blockchain-service", Wait.forHttp("/actuator/health").forStatusCode(200))
                .withLocalCompose(true)

        @BeforeClass
        @JvmStatic
        internal fun beforeAll() {
            instance.start()
        }

        @AfterClass
        @JvmStatic
        internal fun afterAll() {
            instance.stop()
        }
    }
}
