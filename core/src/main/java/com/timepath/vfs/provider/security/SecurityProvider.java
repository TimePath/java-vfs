package com.timepath.vfs.provider.security;

import com.timepath.vfs.SimpleVFile;
import com.timepath.vfs.provider.DelegateProvider;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.util.Collection;

/**
 * Decorates files to force all access through a {@link SecurityController}
 *
 * @author TimePath
 */
public class SecurityProvider extends DelegateProvider {

    @NotNull
    private final SecurityController security;

    public SecurityProvider(@NotNull SimpleVFile data, @NotNull SecurityController policy) {
        super(data);
        security = policy;
    }

    @NotNull
    @Override
    public Collection<? extends SimpleVFile> list() {
        return wrap(security.list(data));
    }

    @Nullable
    @Override
    public SimpleVFile get(@NotNull String name) {
        return wrap(security.get(data.get(name)));
    }

    @SuppressWarnings({"ConstantConditions", "ReturnOfNull"})
    @Contract("null -> null")
    @Override
    protected DelegateProvider wrap(@Nullable SimpleVFile file) {
        return (file == null) ? null : new SecurityProvider(file, security);
    }

    @NotNull
    @Override
    public SimpleVFile add(@NotNull SimpleVFile file) {
        security.add(data, file);
        return this;
    }

    @NotNull
    @Override
    public SimpleVFile addAll(@NotNull Iterable<? extends SimpleVFile> files) {
        for (SimpleVFile file : files) {
            security.add(data, file);
        }
        return this;
    }

    @Override
    public InputStream openStream() {
        return security.openStream(data);
    }
}
