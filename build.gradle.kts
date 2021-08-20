plugins {
    java
    id("maven-publish")
    id("fabric-loom") version "0.9.+"
    kotlin("jvm") version "1.5.21"
    id("com.github.johnrengelman.shadow") version "7.0.0"
}

configurations.implementation.get().extendsFrom(configurations.shadow.get())

allprojects {
    val modId: String by project
    val modName: String by project
    val modVersion: String by project
    val mavenGroup: String by project

    apply(plugin = "fabric-loom")

    base.archivesName.set(modId)
    group = mavenGroup
    version = "$modVersion${getVersionMetadata()}"

    java {
        sourceCompatibility = JavaVersion.VERSION_16
        targetCompatibility = JavaVersion.VERSION_16
    }

    // Declare dependencies
    dependencies {
        // Fabric
        minecraft(rootProject.libs.minecraft)
        mappings(variantOf(rootProject.libs.yarn.mappings) { classifier("v2") })
        modImplementation(rootProject.libs.fabric.loader)

        // Mods
        modImplementation(rootProject.libs.fabric.api)
    }

    // Produce a sources distribution
    java {
        withSourcesJar()
    }

    // Add the licence to all distributions
    tasks.withType<AbstractArchiveTask> {
        from(file("LICENSE.txt"))
    }

    // Process any resources
    tasks.processResources {
        inputs.property("id", modId)
        inputs.property("name", modName)
        inputs.property("version", modVersion)

        // fabric.mod.json
        filesMatching("fabric.mod.json") {
            expand(
                mapOf(
                    "id" to modId,
                    "name" to modName,
                    "version" to modVersion,
                    "fabricLoader" to libs.versions.fabric.loader.get(),
                    "fabricApi" to libs.versions.fabric.api.get(),
                )
            )
        }
    }

    // Add any additional repositories
    repositories {
        mavenCentral()
        maven {
            name = "Fabric"
            url = uri("https://maven.fabricmc.net/")
        }
    }
}

repositories {
    maven("https://maven.nucleoid.xyz/")
    maven("https://maven.kotlindiscord.com/repository/maven-public/")
    maven("https://maven.tomthegeek.ml/releases/")
}

dependencies {
    modImplementation(libs.fabric.kotlin)

    modImplementation(libs.placeholder.api)
    include(libs.placeholder.api)

    modImplementation(libs.mcDiscordReserializer)
    include(libs.mcDiscordReserializer)

    shadow(libs.kord.extensions)
    shadow(libs.emoji)

    shadow(libs.konf.base)
    shadow(libs.konf.toml)

    subprojects.forEach {
        implementation(project(":${it.name}"))
        include(project("${it.name}:")) // nest within distribution
    }
}

tasks {
    remapJar {
        dependsOn(shadowJar)
        input.set(shadowJar.get().archiveFile)
    }

    compileKotlin {
        kotlinOptions {
            jvmTarget = "16"
        }
    }

    shadowJar {
        from("LICENSE")

        configurations = listOf(
            project.configurations.shadow.get()
        )
        archiveClassifier.set("dev-all")

        exclude("kotlin/**", "kotlinx/coroutines/**", "kotlinx/serialization/**", "javax/**", "META-INF")
        exclude("org/checkerframework/**", "org/intellij/**", "org/jetbrains/annotations/**")
        exclude("com/google/gson/**")
        exclude("net/kyori/**")
        exclude("org/slf4j/**")
        exclude("com/ibm/**")

        val relocPath = "io.github.quiltservertools.blockbotdiscord.libs."
        relocate("com.fasterxml", relocPath + "com.fasterxml")
        relocate("com.moandjiezana", relocPath + "com.moandjiezana")
        relocate("com.uchuhimo", relocPath + "com.uchuhimo")
        relocate("com.googlecode", relocPath + "com.googlecode")
        relocate("com.kotlindiscord", relocPath + "com.kotlindiscord")
        relocate("com.sun", relocPath + "com.sun")
        relocate("com.typesafe", relocPath + "com.typesafe")
        relocate("com.vdurmont", relocPath + "com.vdurmont")
        relocate("javassist", relocPath + "javassist")
        relocate("dev.kord", relocPath + "dev.kord")
        relocate("io.ktor", relocPath + "io.ktor")
        relocate("io.sentry", relocPath + "io.sentry")
        relocate("org.apache.commons", relocPath + "org.apache.commons")
        relocate("org.eclipse", relocPath + "org.eclipse")
        relocate("org.gjt", relocPath + "org.gjt")
        relocate("org.jaxen", relocPath + "org.jaxen")
        relocate("org.json", relocPath + "org.json")
        relocate("org.koin", relocPath + "org.koin")
        relocate("org.relaxng", relocPath + "org.relaxng")
        relocate("org.xml", relocPath + "org.xml")
        relocate("org.xmlpull", relocPath + "org.xmlpull")
        relocate("org.yaml", relocPath + "org.yaml")
        relocate("org.dom4j", relocPath + "org.dom4j")

        relocate("org.reflections", relocPath + "org.reflections")
    }
}

fun getVersionMetadata(): String {
    val buildId = System.getenv("GITHUB_RUN_NUMBER")

    // CI builds only
    if (buildId != null) {
        return "+build.$buildId"
    }

    // No tracking information could be found about the build
    return ""
}