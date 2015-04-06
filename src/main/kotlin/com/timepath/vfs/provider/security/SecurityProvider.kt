package com.timepath.vfs.provider.security

import com.timepath.vfs.SimpleVFile
import com.timepath.vfs.provider.DelegateProvider
import java.io.InputStream

/**
 * Decorates files to force all access through a {@link SecurityController}
 *
 * @author TimePath
 */
public class SecurityProvider(data: SimpleVFile, private val security: SecurityController) : DelegateProvider(data) {

    override fun list(): List<SimpleVFile> = wrap(security.list(data))

    override fun get(name: String): SimpleVFile? = wrap(security[data[name]])

    override fun wrap(file: SimpleVFile?): DelegateProvider? = when (file) {
        null -> null
        else -> SecurityProvider(file, security)
    }

    override fun add(file: SimpleVFile): SimpleVFile {
        security.add(data, file)
        return this
    }

    override fun addAll(files: Iterable<SimpleVFile>): SimpleVFile {
        for (file in files) {
            security.add(data, file)
        }
        return this
    }

    override fun openStream(): InputStream? {
        return security.openStream(data)
    }
}
