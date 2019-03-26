package org.daiv

import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import java.io.File
import java.time.Instant
import java.util.*

data class CommitData(
    val longCommitHash: String,
    val shortCommitHash: String,
    val userName: String,
    val userEmail: String
)

fun getCommitHash(): CommitData {
    val repositoryBuilder = FileRepositoryBuilder()
    val repository = repositoryBuilder.setGitDir(File(".git"))
        .readEnvironment() // scan environment GIT_* variables
        .findGitDir() // scan up the file system tree
        .setMustExist(true)
        .build()
    val lastCommitId = repository.resolve(Constants.HEAD)
    val config = repository.config
    return CommitData(
        lastCommitId.name(),
        lastCommitId.abbreviate(7).name(),
        config.getString("user", null, "name"),
        config.getString("user", null, "email")
    )
}

internal class VersionBuilder(val last: String, val split: List<String>) {
    constructor(split: List<String>) : this(split.last(), split)

    fun versionData(): VersionData {
        return if (last.endsWith("SNAPSHOT")) {
            val snapshotSplit = last.split("-")[0]
            VersionData(true, snapshotSplit.toInt(), split)
        } else {
            VersionData(false, last.toInt(), split)
        }
    }

    fun nextVersion(): String {
        return versionData().version()
    }
}

data class VersionData(val isSnapshot: Boolean, val minor: Int, val split: List<String>) {
    fun version(): String {
        val join = split.toList().take(split.size - 1).joinToString(".")
        val nextBase = "$join.${minor + 1}"
        return if (isSnapshot) "$nextBase-SNAPSHOT" else nextBase
    }
}

fun incrementVersion(version: String): String {
    return VersionBuilder(version.split(".")).nextVersion()
}

open class VersionConfiguration {
    var versionProperty: String? = null
    var propertiesFile: String = "gradle.properties"
    var releaseFile: String = "version.properties"
    var getCommitHash: Boolean = true
    var versionUpdate: Boolean = false
    override fun toString(): String {
        return "versionProp: $versionProperty"
    }
}

class VersioningPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val config =
            project.extensions.create<VersionConfiguration>("versionConfiguration", VersionConfiguration::class.java)
        project.task("incrementVersion") { task: Task ->
            task.doLast {
                val versionProp = Properties()
                val propertiesFile = project.file(config.propertiesFile!!)
                val property = config.versionProperty!!
                val propProp = Properties()
                propProp.load(propertiesFile.inputStream())
                val version = propProp.getProperty(property)
                if (config.getCommitHash) {
                    val commit = getCommitHash()
                    versionProp.setProperty(property, version)
                    versionProp.setProperty("longCommitHash", commit.longCommitHash)
                    versionProp.setProperty("shortCommitHash", commit.shortCommitHash)
                    versionProp.setProperty("userName", commit.userName)
                    versionProp.setProperty("userEmail", commit.userEmail)
                    versionProp.setProperty("date UTC", Instant.now().toString())
                    val releasePath = "${project.buildDir}/${config.releaseFile!!}"
                    println(releasePath)
                    val releaseFile = project.file(releasePath)
                    versionProp.store(releaseFile.bufferedWriter(), null)
                }
                if (config.versionUpdate) {
                    propProp.load(propertiesFile.inputStream())
                    propProp.setProperty(property, incrementVersion(version))
                    propProp.store(propertiesFile.bufferedWriter(), null)
                    println("next minor version set: ${incrementVersion(version)}")
                }
            }
        }
    }
}
