import java.util.Locale

pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://repo.papermc.io/repository/maven-public/")
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
}

if (!file(".git").exists()) {
    val errorText = """
        
        =====================[ ERROR ]=====================
         The Pufferfish project directory is not a properly cloned Git repository.
         
         In order to build Pufferfish from source you must clone
         the Pufferfish repository using Git, not download a code
         zip from GitHub.
         
         Built Pufferfish jars are available for download at
         https://github.com/SerlithNetwork/Puffernot
        ===================================================
    """.trimIndent()
    error(errorText)
}

rootProject.name = "pufferfish"
for (name in listOf("pufferfish-api", "pufferfish-server")) {
    val projName = name.lowercase(Locale.ENGLISH)
    include(projName)
    findProject(":$projName")!!.projectDir = file(name)
}
