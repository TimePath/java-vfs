package com.timepath.vfs.provider.jdbc;

import com.timepath.vfs.MockFile;
import com.timepath.vfs.SimpleVFile;
import com.timepath.vfs.VFSStub;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.logging.Level;

/**
* @author TimePath
*/
class JDBCTable extends VFSStub {

    private JDBCProvider jdbcProvider;

    JDBCTable(JDBCProvider jdbcProvider, String name) {
        super(name);
        this.jdbcProvider = jdbcProvider;
    }

    @Nullable
    @Override
    public SimpleVFile get(@NotNull final String name) {
        for (@NotNull SimpleVFile f : list()) if (name.equals(f.getName())) return f;
        return null;
    }

    @NotNull
    @Override
    public Collection<? extends SimpleVFile> list() {
        @NotNull LinkedList<MockFile> rows = new LinkedList<>();
        try {
            PreparedStatement st = jdbcProvider.conn.prepareStatement(String.format("SELECT * FROM %s", getName()));
            ResultSet rs = st.executeQuery();
            int len = rs.getMetaData().getColumnCount();
            while (rs.next()) {
                @NotNull StringBuilder sb = new StringBuilder();
                for (int i = 0; i < len; i++) {
                    sb.append('\t').append(rs.getString(i + 1));
                }
                rows.add(new MockFile(rs.getString(1), sb.substring(1)));
            }
            rs.close();
            st.close();
        } catch (SQLException e) {
            JDBCProvider.LOG.log(Level.SEVERE, "Reading from table {0}", getName());
            JDBCProvider.LOG.log(Level.SEVERE, null, e);
        }
        return rows;
    }
}
