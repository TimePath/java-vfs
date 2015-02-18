package com.timepath.vfs

import java.io.ByteArrayInputStream

public class MockFile(override val name: String, contents: String? = null) : SimpleVFile() {
    private val bytes = contents?.toByteArray()

    override val isDirectory = bytes == null

    override fun openStream() = when {
        bytes != null -> ByteArrayInputStream(bytes)
        else -> null
    }
}
