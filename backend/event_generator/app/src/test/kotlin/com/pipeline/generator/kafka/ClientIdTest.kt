package com.pipeline.generator.kafka

import kotlin.test.Test
import kotlin.test.assertEquals

class ClientIdTest {

    @Test
    fun `buildClientId joins prefix and instance id`() {
        assertEquals("event-generator-pod-7", buildClientId("event-generator", "pod-7"))
    }

    @Test
    fun `resolveInstanceId prefers HOSTNAME`() {
        val envHostname = "generator-abc"
        val id = resolveInstanceId(
            env = { name -> if (name == "HOSTNAME") envHostname else null },
            osHostName = { "should-not-be-used" }
        )
        assertEquals(envHostname, id)
    }

    @Test
    fun `resolveInstanceId falls back to the OS hostname`() {
        val id = resolveInstanceId(env = { null }, osHostName = { "my-macbook" })
        assertEquals("my-macbook", id)
    }

    @Test
    fun `resolveInstanceId falls back to local when nothing is available`() {
        val id = resolveInstanceId(env = { null }, osHostName = { null })
        assertEquals("local", id)
    }

    @Test
    fun `resolveInstanceId ignores a blank HOSTNAME`() {
        val id = resolveInstanceId(
            env = { name -> if (name == "HOSTNAME") "   " else null },
            osHostName = { "fallback-host" },
        )
        assertEquals("fallback-host", id)
    }

    @Test
    fun `resolveInstanceId ignores a blank OS hostname`() {
        val id = resolveInstanceId(env = { null }, osHostName = { "  " })
        assertEquals("local", id)
    }
}
