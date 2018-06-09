package net.minecraftforge.gradle.shared.mappings;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableSet;
import net.minecraftforge.gradle.api.mapping.MappingEntry;
import net.minecraftforge.gradle.api.mapping.MappingProvider;
import net.minecraftforge.gradle.api.mapping.MappingVersion;
import net.minecraftforge.gradle.shared.Constants;
import net.minecraftforge.gradle.shared.util.Util;
import org.gradle.api.artifacts.dsl.RepositoryHandler;
import org.gradle.internal.impldep.com.beust.jcommander.internal.Maps;
import org.gradle.internal.impldep.org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * {@link MappingProvider} implementation for MCP mappings.<br/>
 * Served from Forge's maven.
 */
public class MCPMappingProvider implements MappingProvider {

    private static final Set<String> SUPPORTED = ImmutableSet.of(
            "notch-srg", "notch-mcp",
            "srg-notch", "srg-mcp",
            "mcp-notch", "mcp-srg"
    );
    private static final Object SRG_HANDLE = new Object();
    private static final Object CSV_HANDLE = new Object();

    private final Map<MappingVersion, MappingData> cachedData = new HashMap<>();

    @Override
    public String getName() {
        return "mcp";
    }

    @Override
    public void addDependencyRepositories(RepositoryHandler handler) {
        handler.maven(repo -> repo.setUrl(Constants.MAVEN_FORGE));
    }

    @Override
    public Set<String> getSupportedMappings() {
        return SUPPORTED;
    }

    @Override
    public Map<MappingEntry, MappingEntry> getMapping(MappingVersion version, Function<Object, File> dependencyResolver) {
        File unzippedSrgs = Util.unzip(dependencyResolver.apply(SRG_HANDLE));
        File unzippedCSVs = Util.unzip(dependencyResolver.apply(CSV_HANDLE));

        MappingData data = cachedData.computeIfAbsent(version, $ -> new MappingData(unzippedSrgs, unzippedCSVs));
        Map<MappingEntry, MappingEntry> mappings = new LinkedHashMap<>();

        switch (version.getMapping()) {
            case "notch-srg":
                mappings.putAll(data.notchSrgPackages);
                mappings.putAll(data.notchSrgClasses);
                mappings.putAll(data.notchSrgFields);
                mappings.putAll(data.notchSrgMethods);
                break;
            case "srg-notch":
                mappings.putAll(data.notchSrgPackages.inverse());
                mappings.putAll(data.notchSrgClasses.inverse());
                mappings.putAll(data.notchSrgFields.inverse());
                mappings.putAll(data.notchSrgMethods.inverse());
                break;
            case "srg-mcp":
                data.notchSrgClasses.values().forEach(e -> mappings.put(e, e));
                mappings.putAll(data.srgMCPFields);
                mappings.putAll(data.srgMCPMethods);
                break;
            case "mcp-srg":
                data.notchSrgClasses.values().forEach(e -> mappings.put(e, e));
                mappings.putAll(data.srgMCPFields.inverse());
                mappings.putAll(data.srgMCPMethods.inverse());
                break;
            case "notch-mcp":
                mappings.putAll(data.notchSrgPackages);
                mappings.putAll(data.notchSrgClasses);
                data.notchSrgFields.forEach((a, b) -> mappings.put(a, data.srgMCPFields.get(b)));
                data.notchSrgMethods.forEach((a, b) -> mappings.put(a, data.srgMCPMethods.get(b)));
                break;
            case "mcp-notch":
                mappings.putAll(data.notchSrgPackages.inverse());
                mappings.putAll(data.notchSrgClasses.inverse());
                data.notchSrgFields.forEach((a, b) -> mappings.put(data.srgMCPFields.get(b), a));
                data.notchSrgMethods.forEach((a, b) -> mappings.put(data.srgMCPMethods.get(b), a));
                break;
        }

        return mappings;
    }

    @Override
    public Map<Object, Object> getDependencies(MappingVersion version) {
        Map<Object, Object> dependencies = new HashMap<>();
        dependencies.put(SRG_HANDLE, Maps.newHashMap(
                "group", "de.oceanlabs.mcp",
                "name", "mcp",
                "version", version.getMCVersion(),
                "classifier", "srg",
                "ext", "zip"
        ));
        dependencies.put(CSV_HANDLE, Maps.newHashMap(
                "group", "de.oceanlabs.mcp",
                "name", "mcp_" + version.getChannel(),
                "version", version.getVersion() + "-+",
                "ext", "zip"
        ));
        return dependencies;
    }

    private class MappingData {

        private final BiMap<MappingEntry, MappingEntry> notchSrgPackages = HashBiMap.create();
        private final BiMap<MappingEntry, MappingEntry> notchSrgClasses = HashBiMap.create();
        private final BiMap<MappingEntry, MappingEntry> notchSrgFields = HashBiMap.create();
        private final BiMap<MappingEntry, MappingEntry> notchSrgMethods = HashBiMap.create();

        private final BiMap<MappingEntry, MappingEntry> srgMCPFields = HashBiMap.create();
        private final BiMap<MappingEntry, MappingEntry> srgMCPMethods = HashBiMap.create();

        private MappingData(File unzippedSrgs, File unzippedCSVs) {
            this(
                    new File(unzippedSrgs, "joined.srg"),
                    new File(unzippedCSVs, "fields.csv"),
                    new File(unzippedCSVs, "methods.csv")
            );
        }

        private MappingData(File joinedSrg, File fieldsCSV, File methodsCSV) {
            List<String> srgLines;
            Map<String, String> fieldMappings, methodMappings;

            try {
                srgLines = FileUtils.readLines(joinedSrg);
                fieldMappings = Util.readCSV(fieldsCSV, "searge", "name");
                methodMappings = Util.readCSV(methodsCSV, "searge", "name");
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }

            for (String line : srgLines) {
                String[] split = line.split(" ");
                switch (split[0]) {
                    case "PK:": {
                        notchSrgPackages.put(MappingEntry.forPackage(split[1]), MappingEntry.forPackage(split[2]));
                        break;
                    }
                    case "CL:": {
                        notchSrgClasses.put(MappingEntry.forClass(split[1]), MappingEntry.forClass(split[2]));
                        break;
                    }
                    case "FD:": {
                        MappingEntry.Field mapping = MappingEntry.forFullyQualifiedField(split[2]);
                        String srgName = fieldMappings.get(mapping.getName());
                        if (srgName == null) srgName = mapping.getName();
                        notchSrgFields.put(MappingEntry.forFullyQualifiedField(split[1]), mapping);
                        srgMCPFields.put(mapping, MappingEntry.forField(mapping.getOwner(), srgName));
                        break;
                    }
                    case "MD:": {
                        MappingEntry.Method mapping = MappingEntry.forFullyQualifiedMethod(split[3], split[4]);
                        String srgName = methodMappings.get(mapping.getName());
                        if (srgName == null) srgName = mapping.getName();
                        notchSrgMethods.put(MappingEntry.forFullyQualifiedMethod(split[1], split[2]), mapping);
                        srgMCPMethods.put(mapping, MappingEntry.forMethod(mapping.getOwner(), srgName, mapping.getDescriptor()));
                        break;
                    }
                }
            }
        }

    }

}
