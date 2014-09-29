package com.timepath.vfs.provider.jdbc;

import com.timepath.vfs.MockFile;
import com.timepath.vfs.SimpleVFile;
import com.timepath.vfs.provider.ProviderStub;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.LinkedList;
import java.util.logging.Level;

/**
 * @author TimePath
 */
class JDBCTable extends ProviderStub {

    private final JDBCProvider jdbcProvider;

    JDBCTable(JDBCProvider jdbcProvider, String name) {
        super(name);
        this.jdbcProvider = jdbcProvider;
    }

    @Nullable
    @Override
    public SimpleVFile get(@NonNls @NotNull String name) {
        for (@NotNull SimpleVFile file : list()) if (name.equals(file.getName())) return file;
        return null;
    }

    @SuppressWarnings("NestedTryStatement")
    @NotNull
    @Override
    public Collection<? extends SimpleVFile> list() {
        Collection<MockFile> rows = new LinkedList<>();
        try (PreparedStatement st = jdbcProvider.conn.prepareStatement(String.format("SELECT * FROM %s", getName()))) {
            try (ResultSet rs = st.executeQuery()) {
                int len = rs.getMetaData().getColumnCount();
                while (rs.next()) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < len; i++) {
                        sb.append('\t').append(rs.getString(i + 1));
                    }
                    rows.add(new MockFile(rs.getString(1), sb.substring(1)));
                }
            }
        } catch (SQLException e) {
            JDBCProvider.LOG.log(Level.SEVERE, MessageFormat.format("Reading from table {0}", getName()), e);
        }
        return rows;
    }
}
