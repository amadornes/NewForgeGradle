package net.minecraftforge.gradle.api;

import org.gradle.internal.impldep.org.apache.commons.lang.builder.ToStringBuilder;
import org.gradle.internal.impldep.org.apache.commons.lang.builder.ToStringStyle;

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

}
