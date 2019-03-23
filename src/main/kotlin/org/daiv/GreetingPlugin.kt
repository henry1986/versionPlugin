package org.daiv

import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import java.io.File
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
    var propertiesFile: String = "version.properties"
    var getCommitHash: Boolean = true
    override fun toString(): String {
        return "versionProp: $versionProperty"
    }
}

class GreetingPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val versionConfiguration =
            project.extensions.create<VersionConfiguration>("versionConfiguration", VersionConfiguration::class.java)
        project.task("incrementVersion") { task: Task ->
            task.doLast {
                if (versionConfiguration.getCommitHash) {

                }
                val prop = Properties()
                val file = project.file(versionConfiguration.propertiesFile!!)
                prop.load(file.inputStream())
                val property = versionConfiguration.versionProperty!!
                val version = prop.getProperty(property)
                prop.setProperty(property, incrementVersion(version))
                prop.store(file.bufferedWriter(), null)
                println("next minor version set: ${incrementVersion(version)}")
            }
        }
    }
}
