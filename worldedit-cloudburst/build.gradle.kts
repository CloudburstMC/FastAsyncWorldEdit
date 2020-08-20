import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    `java-library`
}

applyPlatformAndCoreConfiguration()
applyShadowConfiguration()

repositories {
    maven {
        name = "Cloudburst"
        url = uri("https://repo.nukkitx.com/snapshot/")
    }
    maven { url = uri("https://maven.enginehub.org/repo/") }
    maven {
        this.name = "JitPack"
        this.url = uri("https://jitpack.io")
    }
    mavenLocal()
}

dependencies {
    "api"(project(":worldedit-core"))
    "api"(project(":worldedit-libs:cloudburst"))
    "api"("org.cloudburstmc:cloudburst-server:1.0.0-SNAPSHOT") {
        exclude("junit", "junit")
        isTransitive = false
        isChanging = true
    }
    "compileOnly"("org.jetbrains:annotations:19.0.0")
    "implementation"("com.thevoxelbox.voxelsniper:voxelsniper:5.171.0") { isTransitive = false }
    "implementation"("it.unimi.dsi:fastutil:${Versions.FAST_UTIL}")
}

tasks.named<Copy>("processResources") {
    filesMatching("plugin.yml") {
        expand("internalVersion" to project.ext["internalVersion"])
    }
}

tasks.named<Jar>("jar") {
    manifest {
        attributes("Class-Path" to CLASSPATH,
                "WorldEdit-Version" to project.version)
    }
}

tasks.named<ShadowJar>("shadowJar") {
    dependencies {
        relocate("org.antlr.v4", "com.sk89q.worldedit.antlr4")
        include(dependency(":worldedit-core"))
        include(dependency(":worldedit-libs:cloudburst"))
        include(dependency("org.antlr:antlr4-runtime"))
        include(dependency("com.google.code.gson:gson"))
        relocate("it.unimi.dsi.fastutil", "com.sk89q.worldedit.bukkit.fastutil") {
            include(dependency("it.unimi.dsi:fastutil"))
        }
    }
}

tasks.named("assemble").configure {
    dependsOn("shadowJar")
}
