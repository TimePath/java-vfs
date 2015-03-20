package com.timepath.vfs.provider

import com.timepath.vfs.SimpleVFile
import org.jetbrains.annotations.NonNls

import java.io.InputStream

public open class ProviderStub protected(NonNls name: String? = null) : SimpleVFile() {

    override val name: String = when (name) {
        null -> toString()
        else -> name
    }

    override fun openStream() = null

    override val path = ""

    override val isDirectory = true

    override var lastModified = System.currentTimeMillis()
        private set

    override var length = 0L
        private set

    override val owner = NOBODY

    companion object {

        NonNls
        private val NOBODY = System.getProperty("user.name", "nobody")
    }
}
