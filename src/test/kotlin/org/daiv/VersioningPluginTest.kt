package org.daiv

import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import kotlin.test.assertEquals

class VersioningPluginTest :Spek({
    describe("Versioning"){
        on("get version"){
            it("incrementVersion"){
                assertEquals("0.1.4", incrementVersion("0.1.3"))
                assertEquals("0.1.4-SNAPSHOT", incrementVersion("0.1.3-SNAPSHOT"))
            }
            it("getCommitHash"){
                val commitData = getCommitHash()
                println(commitData)
            }
        }
    }
})