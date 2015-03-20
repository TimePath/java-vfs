package com.timepath.vfs.provider.jdbc

import com.timepath.vfs.provider.ProviderStub
import com.timepath.vfs.SimpleVFile
import org.jetbrains.annotations.NonNls

import java.sql.*
import java.util.LinkedList
import java.util.logging.Level
import java.util.logging.Logger

/**
 * @author TimePath
 */
public abstract class JDBCProvider [throws(javaClass<SQLException>())](protected val url: String) : ProviderStub() {
    val conn: Connection = DriverManager.getConnection(url)

    override val name = url.replace('/', '\\')

    override fun get(NonNls name: String) = list().firstOrNull { it.name == name }

    override fun list(): Collection<SimpleVFile> {
        val tableList = LinkedList<JDBCTable>()
        try {
            val dbmd = conn.getMetaData()
            val types = array("TABLE")
            val rs = dbmd.getTables(null, null, "%", types)
            while (rs.next()) {
                tableList.add(JDBCTable(this, rs.getString("TABLE_NAME")))
            }
            LOG.log(Level.INFO, "Tables: {0}", tableList)
        } catch (e: SQLException) {
            LOG.log(Level.SEVERE, "Reading from metadata", name)
            LOG.log(Level.SEVERE, null, e)
        }
        return tableList
    }

    companion object {
        val LOG = Logger.getLogger(javaClass<JDBCProvider>().getName())
    }

}
