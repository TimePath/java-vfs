package com.timepath.vfs.provider.jdbc

import com.timepath.vfs.MockFile
import com.timepath.vfs.SimpleVFile
import com.timepath.vfs.provider.ProviderStub
import org.jetbrains.annotations.NonNls

import java.sql.SQLException
import java.text.MessageFormat
import java.util.LinkedList
import java.util.logging.Level

/**
 * @author TimePath
 */
class JDBCTable(private val jdbcProvider: JDBCProvider, name: String) : ProviderStub(name) {

    override fun get(NonNls name: String) = list().firstOrNull { name == it.name }

    SuppressWarnings("NestedTryStatement")
    override fun list(): Collection<SimpleVFile> {
        val rows = LinkedList<MockFile>()
        try {
            jdbcProvider.conn.prepareStatement("SELECT * FROM ${name}").let { st ->
                st.executeQuery().let { rs ->
                    val len = rs.getMetaData().getColumnCount()
                    while (rs.next()) {
                        val sb = StringBuilder()
                        for (i in len.indices) {
                            sb.append('\t').append(rs.getString(i + 1))
                        }
                        rows.add(MockFile(rs.getString(1), sb.substring(1)))
                    }
                }
            }
        } catch (e: SQLException) {
            JDBCProvider.LOG.log(Level.SEVERE, MessageFormat.format("Reading from table {0}", name), e)
        }
        return rows
    }
}
