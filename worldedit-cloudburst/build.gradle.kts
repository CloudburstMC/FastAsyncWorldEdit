import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

applyPlatformAndCoreConfiguration()
applyShadowConfiguration()

plugins {
    `java-library`
}

repositories {
    maven {
        name = "Cloudburst"
        url = uri("https://repo.nukkitx.com/snapshot/")
    }
    mavenLocal()
}

dependencies {
    implementation(project(":worldedit-core"))
    implementation(project(":worldedit-libs:cloudburst"))
    implementation("org.cloudburstmc:cloudburst-server:1.0.0-SNAPSHOT")
    implementation("com.google.code.gson:gson:2.8.6")
    implementation("org.apache.logging.log4j:log4j-slf4j-impl:2.8.1")
    implementation("org.slf4j:slf4j-api:1.7.10")
    implementation("org.apache.logging.log4j:log4j-core:2.8.1")
    implementation("org.apache.logging.log4j:log4j-api:2.8.1")
    implementation("com.bekvon.bukkit.residence:Residence:4.5._13.1") { isTransitive = false }
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
        relocate("org.slf4j", "com.sk89q.worldedit.slf4j")
        relocate("org.apache.logging.slf4j", "com.sk89q.worldedit.log4jbridge")
        relocate("org.antlr.v4", "com.sk89q.worldedit.antlr4")
        include(dependency(":worldedit-core"))
        include(dependency("org.slf4j:slf4j-api"))
        include(dependency("org.apache.logging.log4j:log4j-slf4j-impl"))
        include(dependency("org.antlr:antlr4-runtime"))
        include(dependency("org.apache.logging.log4j:log4j-core"))
        include(dependency("org.apache.logging.log4j:log4j-api"))
        relocate("it.unimi.dsi.fastutil", "com.sk89q.worldedit.bukkit.fastutil") {
            include(dependency("it.unimi.dsi:fastutil"))
        }
        include(dependency("com.google.code.gson:gson"))
    }
}

tasks.named("assemble").configure {
    dependsOn("shadowJar")
}
