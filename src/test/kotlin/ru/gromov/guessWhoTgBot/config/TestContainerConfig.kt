package ru.gromov.guessWhoTgBot.config

import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName


class TestContainerConfig {
    @Volatile
    private var instance: TestContainerConfig? = null

    var url: String? = null

    var username: String? = null

    var password: String? = null

    private fun TestContainersInitializer(): TestContainerConfig {
        val myImage = DockerImageName
            .parse("postgres:latest")
            .asCompatibleSubstituteFor("postgres")
        val dbContainer: PostgreSQLContainer<Nothing> = PostgreSQLContainer(myImage)
        dbContainer.withTmpFs(mapOf(Pair("/var/lib/postgresql/data", "rw")))
        dbContainer.withReuse(true)
        dbContainer.start()
        url = dbContainer.jdbcUrl
        username = dbContainer.username
        password = dbContainer.password
        return this
    }

    fun getInstance(): TestContainerConfig? {
        var localInstance = instance
        if (localInstance == null) {
            synchronized(TestContainerConfig::class.java) {
                localInstance = instance
                if (localInstance == null) {
                    localInstance = TestContainersInitializer()
                    instance = localInstance
                }
            }
        }
        return localInstance
    }
}
