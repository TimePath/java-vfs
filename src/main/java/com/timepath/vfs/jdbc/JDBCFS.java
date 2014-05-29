package com.timepath.vfs.jdbc;

import com.timepath.vfs.MockFile;
import com.timepath.vfs.SimpleVFile;
import com.timepath.vfs.VFSStub;

import java.sql.*;
import java.util.Collection;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author TimePath
 */
public abstract class JDBCFS extends VFSStub {

    private static final Logger LOG = Logger.getLogger(JDBCFS.class.getName());
    protected final String     url;
    protected final Connection conn;

    public JDBCFS(String url) throws SQLException {
        this.url = url;
        this.name = url.replace('/', '\\');
        conn = DriverManager.getConnection(url);
    }

    class JDBCTable extends VFSStub {

        JDBCTable(String name) {
            super(name);
        }

        @Override
        public SimpleVFile get(final String name) {
            for(SimpleVFile f : list()) if(name.equals(f.getName())) return f;
            return null;
        }

        @Override
        public Collection<? extends SimpleVFile> list() {
            LinkedList<MockFile> rows = new LinkedList<>();
            try {
                PreparedStatement st = conn.prepareStatement(String.format("SELECT * FROM %s", getName()));
                ResultSet rs = st.executeQuery();
                int len = rs.getMetaData().getColumnCount();
                while(rs.next()) {
                    StringBuilder sb = new StringBuilder();
                    for(int i = 0; i < len; i++) {
                        sb.append('\t').append(rs.getString(i + 1));
                    }
                    rows.add(new MockFile(rs.getString(1), sb.substring(1)));
                }
                rs.close();
                st.close();
            } catch(SQLException e) {
                LOG.log(Level.SEVERE, "Reading from table {0}", getName());
                LOG.log(Level.SEVERE, null, e);
            }
            return rows;
        }
    }

    @Override
    public SimpleVFile get(final String name) {
        for(SimpleVFile f : list()) if(name.equals(f.getName())) return f;
        return null;
    }

    @Override
    public Collection<? extends SimpleVFile> list() {
        LinkedList<JDBCTable> tableList = new LinkedList<>();
        try {
            DatabaseMetaData dbmd = conn.getMetaData();
            String[] types = { "TABLE" };
            ResultSet rs = dbmd.getTables(null, null, "%", types);
            while(rs.next()) {
                tableList.add(new JDBCTable(rs.getString("TABLE_NAME")));
            }
            LOG.log(Level.INFO, "Tables: {0}", tableList);
        } catch(SQLException e) {
            LOG.log(Level.SEVERE, "Reading from metadata", getName());
            LOG.log(Level.SEVERE, null, e);
        }
        return tableList;
    }
}
