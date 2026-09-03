package com.pipeline.generator.kafka

import java.net.InetAddress

internal fun resolveInstanceId(
    env: (String) -> String? = System::getenv,
    osHostName: () -> String? = {
        runCatching {
            InetAddress.getLocalHost().hostName
        }.getOrNull()
    },
): String =
    env("HOSTNAME")?.trim()?.takeIf { it.isNotEmpty() }
        ?: osHostName()?.trim()?.takeIf { it.isNotEmpty() }
        ?: "local"

internal fun buildClientId(prefix: String, instanceId: String): String = "$prefix-$instanceId"