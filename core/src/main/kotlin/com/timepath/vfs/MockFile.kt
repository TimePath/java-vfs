package com.timepath.vfs

public class MockFile(override val name: String, contents: String? = null) : SimpleVFile() {
    private val bytes = contents?.toByteArray()

    override val isDirectory = bytes == null

    override fun openStream() = bytes?.inputStream
}
