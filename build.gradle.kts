import com.jfrog.bintray.gradle.BintrayExtension
import java.util.*
import org._10ne.gradle.rest.RestTask

plugins {
    kotlin("js") version "1.3.71"
    `maven-publish`
    id("com.jfrog.bintray") version "1.8.1"
    id("org.tenne.rest") version "0.4.2"
}

repositories {
    jcenter()
}

dependencies {
    implementation(kotlin("stdlib-js"))
    testImplementation(kotlin("test-js"))
}

group = "ch.delconte.screeps-kotlin"

kotlin {
    target {
        useCommonJs()
        nodejs()
    }
}


val kotlinSourcesJar by tasks

publishing {
    publications {
        register("kotlin", MavenPublication::class) {
            from(components["kotlin"])
            artifact(kotlinSourcesJar)
        }
    }
}

val bintrayUser: String? by project
val bintrayApiKey: String? by project

bintray {
    user = bintrayUser ?: ""
    key = bintrayApiKey ?: ""
    publish = true
    setPublications("kotlin")
    pkg(delegateClosureOf<BintrayExtension.PackageConfig> {
        repo = "screeps-kotlin"
        name = "screeps-kotlin-types"
        websiteUrl = "https://github.com/exaV/screeps-kotlin-types"
        githubRepo = "exaV/screeps-kotlin-types"
        vcsUrl = "https://github.com/exaV/screeps-kotlin-types"
        setLabels("kotlin")
        setLicenses("MIT")
    })
}


//----------------------------------------------------------
// deployment block
//----------------------------------------------------------

val screepsUser: String? by project
val screepsPassword: String? by project
val screepsToken: String? by project
val screepsHost: String? by project
val screepsBranch: String? by project
val branch = screepsBranch ?: System.getenv("screepsBranch") ?: "kotlin-start"
val host = screepsHost ?: "https://screeps.com"
val minifiedJsDirectory: String = File(buildDir, "minified-js").absolutePath


kotlin {
    target {
        useCommonJs()
        browser {
            dceTask {
                dceOptions {
                    outputDirectory = minifiedJsDirectory
                }
                keep(
                    "${project.name}.loop"
                )
            }

            testTask {
                useMocha()
            }
        }
    }
}

val processDceKotlinJs by tasks.getting(org.jetbrains.kotlin.gradle.dsl.KotlinJsDce::class)
fun String.encodeBase64() = Base64.getEncoder().encodeToString(this.toByteArray())

tasks.register<RestTask>("deploy") {
    group = "screeps"
    dependsOn(processDceKotlinJs)
    val modules = mutableMapOf<String, String>()

    httpMethod = "post"
    uri = "$host/api/user/code"
    requestHeaders = if (screepsToken != null)
        mapOf("X-Token" to screepsToken)
    else
        mapOf("Authorization" to "Basic " + "$screepsUser:$screepsPassword".encodeBase64())
    contentType = groovyx.net.http.ContentType.JSON
    requestBody = mapOf("branch" to branch, "modules" to modules)

    val minifiedCodeLocation = File(minifiedJsDirectory)

    doFirst {
        if (screepsToken == null && (screepsUser == null || screepsPassword == null)) {
            throw InvalidUserDataException("you need to supply either screepsUser and screepsPassword or screepsToken before you can upload code")
        }
        if (!minifiedCodeLocation.isDirectory) {
            throw InvalidUserDataException("found no code to upload at ${minifiedCodeLocation.path}")
        }

        val jsFiles = minifiedCodeLocation.listFiles { _, name -> name.endsWith(".js") }.orEmpty()
        val (mainModule, otherModules) = jsFiles.partition { it.nameWithoutExtension == project.name }

        val main = mainModule.firstOrNull()
            ?: throw IllegalStateException("Could not find js file corresponding to main module in ${minifiedCodeLocation.absolutePath}. Was looking for ${project.name}.js")

        modules["main"] = main.readText()
        modules.putAll(otherModules.associate { it.nameWithoutExtension to it.readText() })

        logger.lifecycle("uploading ${jsFiles.count()} files to branch '$branch' on server $host")
    }

}