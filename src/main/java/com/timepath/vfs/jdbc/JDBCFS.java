package com.timepath.vfs.jdbc;

import com.timepath.vfs.SimpleVFile;
import com.timepath.vfs.VFSStub;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author TimePath
 */
public abstract class JDBCFS extends VFSStub {

    private static final Logger LOG = Logger.getLogger(JDBCFS.class.getName());

    protected final String url;
    private final LinkedList<JDBCTable> tableList;

    public JDBCFS(String url) throws SQLException {
        this.url = url;
        Connection conn = DriverManager.getConnection(url);
        tableList = new LinkedList<>();
        DatabaseMetaData dbmd = conn.getMetaData();
        String[] types = {"TABLE"};
        ResultSet rs = dbmd.getTables(null, null, "%", types);
        while (rs.next()) {
            tableList.add(new JDBCTable(rs.getString("TABLE_NAME")));
        }
        LOG.log(Level.INFO, "Tables: {0}", tableList);
    }

    @Override
    public Collection<? extends SimpleVFile> list() {
        return tableList;
    }

    class JDBCTable extends VFSStub {

        JDBCTable(String name) {
            super(name);
        }

    }

}
