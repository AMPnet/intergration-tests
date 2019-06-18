package com.ampnet.integration.tests

import org.testcontainers.containers.DockerComposeContainer
import org.testcontainers.containers.wait.strategy.Wait
import java.io.File
import java.time.Duration

abstract class BaseTest {

    protected fun suppose(@Suppress("UNUSED_PARAMETER") description: String, function: () -> Unit) {
        function.invoke()
    }

    protected fun verify(@Suppress("UNUSED_PARAMETER") description: String, function: () -> Unit) {
        function.invoke()
    }

    companion object {

        private val statupTimeout = Duration.ofSeconds(120)
        private val instance: KDockerComposeContainer by lazy { defineDockerCompose()}
        class KDockerComposeContainer(file: File) : DockerComposeContainer<KDockerComposeContainer>(file)

        private fun defineDockerCompose() = KDockerComposeContainer(File("src/test/resources/compose-test.yml"))
                .waitingFor("backend-service",
                        Wait.forHttp("/actuator/health")
                                .forStatusCode(200)
                                .withStartupTimeout(statupTimeout))
                .waitingFor("blockchain-service",
                        Wait.forHttp("/actuator/health")
                                .forStatusCode(200)
                                .withStartupTimeout(statupTimeout))
                .waitingFor("user-service",
                        Wait.forHttp("/actuator/health")
                                .forStatusCode(200)
                                .withStartupTimeout(statupTimeout))
                .withLocalCompose(true)

        init {
            instance.start()
        }
    }
}
