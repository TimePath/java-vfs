package com.timepath.vfs.provider.jdbc;

import com.timepath.vfs.provider.ProviderStub;
import com.timepath.vfs.SimpleVFile;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.*;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author TimePath
 */
public abstract class JDBCProvider extends ProviderStub {

    static final Logger LOG = Logger.getLogger(JDBCProvider.class.getName());
    @NotNull
    protected final String url;
    protected final Connection conn;

    public JDBCProvider(@NotNull String url) throws SQLException {
        this.url = url;
        this.name = url.replace('/', '\\');
        conn = DriverManager.getConnection(url);
    }

    @Nullable
    @Override
    public SimpleVFile get(@NonNls @NotNull String name) {
        for (@NotNull SimpleVFile file : list()) if (name.equals(file.getName())) return file;
        return null;
    }

    @NotNull
    @Override
    public Collection<? extends SimpleVFile> list() {
        Collection<JDBCTable> tableList = new LinkedList<>();
        try {
            DatabaseMetaData dbmd = conn.getMetaData();
            String[] types = {"TABLE"};
            ResultSet rs = dbmd.getTables(null, null, "%", types);
            while (rs.next()) {
                tableList.add(new JDBCTable(this, rs.getString("TABLE_NAME")));
            }
            LOG.log(Level.INFO, "Tables: {0}", tableList);
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Reading from metadata", getName());
            LOG.log(Level.SEVERE, null, e);
        }
        return tableList;
    }

}
