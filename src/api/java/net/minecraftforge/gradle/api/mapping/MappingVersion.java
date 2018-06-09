package net.minecraftforge.gradle.api.mapping;

import org.gradle.internal.impldep.org.apache.commons.lang.builder.ToStringBuilder;
import org.gradle.internal.impldep.org.apache.commons.lang.builder.ToStringStyle;

import java.util.function.Supplier;

/**
 * Represents a mapping version (provider, channel, version, MC version and mapping type)
 */
public class MappingVersion {

    private final String provider, channel, version, mcVersion, mapping;

    public MappingVersion(String provider, String channel, String version, String mcVersion, String mapping) {
        this.provider = provider;
        this.channel = channel;
        this.version = version;
        this.mcVersion = mcVersion;
        this.mapping = mapping;
    }

    public String getProvider() {
        return provider;
    }

    public String getChannel() {
        return channel;
    }

    public String getVersion() {
        return version;
    }

    public String getMCVersion() {
        return mcVersion;
    }

    public String getMapping() {
        return mapping;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append("provider", provider)
                .append("channel", channel)
                .append("version", version)
                .append("mcVersion", mcVersion)
                .append("mapping", mapping)
                .toString();
    }

    public static MappingVersion lazy(Supplier<MappingVersion> supplier) {
        return new MappingVersion(null, null, null, null, null) {
            private MappingVersion parent;

            @Override
            public String getProvider() {
                if(parent == null) parent = supplier.get();
                return parent.getProvider();
            }

            @Override
            public String getChannel() {
                if(parent == null) parent = supplier.get();
                return parent.getChannel();
            }

            @Override
            public String getVersion() {
                if(parent == null) parent = supplier.get();
                return parent.getVersion();
            }

            @Override
            public String getMCVersion() {
                if(parent == null) parent = supplier.get();
                return parent.getMCVersion();
            }

            @Override
            public String getMapping() {
                if(parent == null) parent = supplier.get();
                return parent.getMapping();
            }

            @Override
            public String toString() {
                if(parent == null) parent = supplier.get();
                return parent.toString();
            }
        };
    }

}
