package net.minecraftforge.gradle.shared.impl;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import net.minecraftforge.gradle.shared.Constants;
import net.minecraftforge.gradle.shared.repo.CustomRepository;
import net.minecraftforge.gradle.shared.repo.StreamedResource;
import net.minecraftforge.gradle.shared.util.IOSupplier;
import net.minecraftforge.gradle.shared.util.POMBuilder;
import net.minecraftforge.gradle.shared.util.Util;
import org.gradle.api.artifacts.ArtifactIdentifier;
import org.gradle.internal.hash.HashUtil;
import org.gradle.internal.hash.HashValue;
import org.gradle.internal.impldep.com.google.gson.JsonElement;
import org.gradle.internal.impldep.com.google.gson.JsonObject;
import org.gradle.internal.impldep.org.apache.commons.io.IOUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

/**
 * An artifact provider for Minecraft client and server jars.
 */
public class MCLauncherArtifactProvider extends CustomRepository.ArtifactProvider.Simple {

    public static final DateFormat DATE_FORMAT = new SimpleDateFormat("YYYY-mm-dd HH:MM:SS");
    private final Cache<String, JsonObject> manifests = CacheBuilder.newBuilder().expireAfterWrite(30, TimeUnit.MINUTES).build();
    private final Map<String, HashValue> hashes = new HashMap<>();

    public MCLauncherArtifactProvider() {
        addExtensionProvider("jar", this::getJar);
        addExtensionProvider("jar.sha1", this::getJarHash);
        addExtensionProvider("pom", this::getPOM);
    }

    @Override
    protected boolean validate(ArtifactIdentifier identifier) {
        String group = identifier.getModuleVersionIdentifier().getGroup();
        String name = identifier.getModuleVersionIdentifier().getName();
        return group.equals("net.minecraft") && (name.equals("client") || name.equals("server-pure"));
    }

    private IOSupplier<StreamedResource> getJar(ArtifactIdentifier identifier) throws IOException {
        String name = identifier.getModuleVersionIdentifier().getName();
        if (name.equals("client")) {
            return getClientArtifact(identifier); // Get the client jar
        } else if (name.equals("server-pure")) {
            return getPureServerArtifact(identifier); // Get the pure server jar
        }
        return null;
    }

    private IOSupplier<StreamedResource> getJarHash(ArtifactIdentifier identifier) throws IOException {
        String hashID = getHashID(identifier);
        HashValue hash = hashes.get(hashID);
        if (hash == null) {
            IOSupplier<StreamedResource> supplier = getJar(identifier);
            if (supplier == null) return null;
            StreamedResource res = supplier.get();
            hash = res.getMetadata(null).getSha1();
            hashes.put(hashID, hash);
        }
        byte[] hashBytes = hash.asHexString().getBytes();
        return () -> new StreamedResource.ByteArrayStreamedResource(hashBytes);
    }

    private IOSupplier<StreamedResource> getClientArtifact(ArtifactIdentifier identifier) throws IOException {
        String version = identifier.getModuleVersionIdentifier().getVersion();
        JsonObject manifest = getManifest(version);
        if (manifest == null) return null;

        // Get the artifact metadata
        JsonObject artifact = manifest.getAsJsonObject("downloads").getAsJsonObject("client");
        String urlString = artifact.get("url").getAsString();
        long size = artifact.get("size").getAsLong();
        HashValue hash = HashValue.parse(artifact.get("sha1").getAsString());
        try {
            String dateString = manifest.get("time").getAsString().replace('T', ' ');
            dateString = dateString.substring(0, dateString.indexOf('+'));
            Date date = DATE_FORMAT.parse(dateString);

            hashes.put(getHashID(identifier), hash);

            // Create a streamed resource with that metadata
            URL url = new URL(urlString);
            return () -> new StreamedResource.URLStreamedResource(url, size, date, hash);
        } catch (ParseException e) {
            return null;
        }
    }

    private IOSupplier<StreamedResource> getPureServerArtifact(ArtifactIdentifier identifier) throws IOException {
        String version = identifier.getModuleVersionIdentifier().getVersion();
        JsonObject manifest = getManifest(version);
        if (manifest == null) return null;

        // Get the artifact URL
        JsonObject artifact = manifest.getAsJsonObject("downloads").getAsJsonObject("server");
        String url = artifact.get("url").getAsString();

        JarInputStream is = new JarInputStream(new URL(url).openStream());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        JarOutputStream os = new JarOutputStream(baos);

        // Ignore any entry that's not allowed
        JarEntry entry;
        while ((entry = is.getNextJarEntry()) != null) {
            if (!isServerEntryValid(entry)) continue;
            os.putNextEntry(entry);
            IOUtils.copyLarge(is, os, 0, entry.getSize());
            os.closeEntry();
        }

        os.close();
        is.close();

        hashes.put(getHashID(identifier), HashUtil.sha1(baos.toByteArray()));

        // Create a streamed resource from the resulting bytes
        return () -> new StreamedResource.ByteArrayStreamedResource(baos.toByteArray());
    }

    private IOSupplier<StreamedResource> getPOM(ArtifactIdentifier identifier) {
        String version = identifier.getModuleVersionIdentifier().getVersion();
        JsonObject manifest = getManifest(version);
        if (manifest == null) return null;

        String group = identifier.getModuleVersionIdentifier().getGroup();
        String name = identifier.getModuleVersionIdentifier().getName();

        // Start building a POM
        POMBuilder builder = new POMBuilder(group, name, version);
        builder.description(name);

        // Add dependencies
        for (JsonElement libElement : manifest.getAsJsonArray("libraries")) {
            JsonObject lib = libElement.getAsJsonObject();

            // Ignore this dependency if anything says we shouldn't be using it
            if (!isLibraryAllowed(lib)) continue;

            String libName = lib.get("name").getAsString();
            JsonObject downloads = lib.getAsJsonObject("downloads");

            // Add main download if we need it
            if (downloads.has("artifact")) {
                builder.dependencies().add(libName, "compile");
            }

            if (downloads.has("classifiers")) {
                JsonObject classifiers = downloads.getAsJsonObject("classifiers");
                // Add test dependencies if needed
                if (classifiers.has("test")) {
                    builder.dependencies().add(libName, "test").withClassifier("test");
                }
                // Locate natives if required
                if (lib.has("natives")) {
                    JsonElement classifier = lib.getAsJsonObject("natives").get(Util.getOS());
                    if (classifier != null) {
                        builder.dependencies().add(libName, "runtime").withClassifier(classifier.getAsString());
                    }
                }
            }
        }

        // Return the resulting POM file
        String pom = builder.tryBuild();
        if (pom == null) return null;
        return () -> new StreamedResource.ByteArrayStreamedResource(pom.getBytes());
    }

    private boolean isLibraryAllowed(JsonObject lib) {
        if (lib.has("rules")) {
            String os = Util.getOS();
            Boolean shouldDownload = null;
            for (JsonElement ruleElement : lib.getAsJsonArray("rules")) {
                JsonObject rule = ruleElement.getAsJsonObject();
                boolean allow = rule.get("action").getAsString().equals("allow");
                if (!rule.has("os") || rule.getAsJsonObject("os").get("name").getAsString().equals(os)) {
                    shouldDownload = allow;
                }
            }
            // If a rule has blocked this download, skip it
            return shouldDownload == null || shouldDownload;
        }
        return true;
    }

    private boolean isServerEntryValid(ZipEntry entry) {
        if (entry.isDirectory()) return false;
        String name = entry.getName();
        return !name.startsWith("org/bouncycastle/") && !name.startsWith("org/bouncycastle/") && !name.startsWith("org/apache/")
                && !name.startsWith("com/google/") && !name.startsWith("com/mojang/authlib/") && !name.startsWith("com/mojang/util/")
                && !name.startsWith("gnu/trove/") && !name.startsWith("io/netty/") && !name.startsWith("javax/annotation/")
                && !name.startsWith("argo/") && !name.startsWith("it/unimi/dsi/fastutil/");
    }

    private JsonObject getManifest(String version) {
        JsonObject versionManifest = manifests.getIfPresent(version);
        if (versionManifest != null) return versionManifest;

        JsonObject manifest = manifests.getIfPresent(Constants.MC_VERSION_MANIFEST);
        if (manifest == null) {
            manifest = Util.readJSON(Constants.MC_VERSION_MANIFEST);
            manifests.put("", manifest);
        }

        for (JsonElement e : manifest.getAsJsonArray("versions")) {
            String v = e.getAsJsonObject().get("id").getAsString();
            if (v.equals(version)) {
                versionManifest = Util.readJSON(e.getAsJsonObject().get("url").getAsString());
                manifests.put(version, versionManifest);
                return versionManifest;
            }
        }
        return null;
    }

    private String getHashID(ArtifactIdentifier identifier) {
        return identifier.getModuleVersionIdentifier().getVersion() + ":" + identifier.getClassifier();
    }

}
