package net.minecraftforge.gradle.shared.repo;

import net.minecraftforge.gradle.shared.util.POMBuilder;
import net.minecraftforge.gradle.shared.util.StreamedResource;
import net.minecraftforge.gradle.shared.util.Util;
import org.gradle.api.artifacts.ArtifactIdentifier;
import org.gradle.internal.hash.HashValue;
import org.gradle.internal.impldep.com.google.gson.JsonElement;
import org.gradle.internal.impldep.com.google.gson.JsonObject;
import org.gradle.internal.impldep.org.apache.commons.io.IOUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Date;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

/**
 * An artifact provider for Minecraft client and server jars.
 */
public class MCLauncherArtifactProvider implements CustomRepository.ArtifactProvider {

    @Override
    public StreamedResource getArtifact(ArtifactIdentifier identifier) throws IOException {
        if (!identifier.getModuleVersionIdentifier().getGroup().equals("net.minecraft")
                || !identifier.getModuleVersionIdentifier().getName().equals("minecraft")) {
            return null; // We only handle MC
        }

        if (identifier.getExtension().equals("pom")) {
            return getPOMArtifact(identifier); // Get the POM for the specified MC version
        }

        String classifier = identifier.getClassifier();
        if (classifier.equals("client")) {
            return getClientArtifact(identifier); // Get the client jar
        } else if (classifier.equals("server-pure")) {
            return getPureServerArtifact(identifier); // Get the pure server jar
        }

        return null;
    }

    private StreamedResource getClientArtifact(ArtifactIdentifier identifier) throws IOException {
        String version = identifier.getModuleVersionIdentifier().getVersion();
        JsonObject manifest = getManifest(version);
        if (manifest == null) return null;

        // Get the artifact metadata
        JsonObject artifact = manifest.getAsJsonObject("downloads").getAsJsonObject("client");
        String url = artifact.get("url").getAsString();
        long size = artifact.get("size").getAsLong();
        String hash = artifact.get("sha1").getAsString();
        Date date = new Date(manifest.get("time").getAsString());

        // Create a streamed resource with that metadata
        return new StreamedResource.URLStreamedResource(new URL(url), size, date, HashValue.parse(hash));
    }

    private StreamedResource getPureServerArtifact(ArtifactIdentifier identifier) throws IOException {
        String version = identifier.getModuleVersionIdentifier().getVersion();
        JsonObject manifest = getManifest(version);
        if (manifest == null) return null;

        // Get the artifact URL
        JsonObject artifact = manifest.getAsJsonObject("downloads").getAsJsonObject("server");
        String url = artifact.get("url").getAsString();

        try {
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

            // Create a streamed resource from the resulting bytes
            return new StreamedResource.ByteArrayStreamedResource(baos.toByteArray());
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }
    }

    private StreamedResource getPOMArtifact(ArtifactIdentifier identifier) {
        String version = identifier.getModuleVersionIdentifier().getVersion();
        JsonObject manifest = getManifest(version);
        if (manifest == null) return null;

        // Start building a POM
        POMBuilder builder = new POMBuilder("net.minecraft", "minecraft", version);
        builder.description("minecraft");

        // Add dependencies
        for (JsonElement libElement : manifest.getAsJsonArray("libraries")) {
            JsonObject lib = libElement.getAsJsonObject();

            // Ignore this dependency if anything says we shouldn't be using it
            if (!isLibraryAllowed(lib)) continue;

            String name = lib.get("name").getAsString();
            JsonObject downloads = lib.getAsJsonObject("downloads");

            // Add main download if we need it
            if (downloads.has("artifact")) {
                builder.dependencies().add(name, "compile");
            }

            if (downloads.has("classifiers")) {
                JsonObject classifiers = downloads.getAsJsonObject("classifiers");
                // Add test dependencies if needed
                if (classifiers.has("test")) {
                    builder.dependencies().add(name, "test").withClassifier("test");
                }
                // Locate natives if required
                if (lib.has("natives")) {
                    JsonElement classifier = lib.getAsJsonObject("natives").get(Util.getOS());
                    if (classifier != null) {
                        builder.dependencies().add(name, "runtime").withClassifier(classifier.getAsString());
                    }
                }
            }
        }

        // Return the resulting POM file
        String pom = builder.tryBuild();
        if (pom == null) return null;
        return new StreamedResource.ByteArrayStreamedResource(pom.getBytes());
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
        JsonObject manifest = Util.readJSON("https://launchermeta.mojang.com/mc/game/version_manifest.json");
        for (JsonElement e : manifest.getAsJsonArray("versions")) {
            String v = e.getAsJsonObject().get("id").getAsString();
            if (v.equals(version)) {
                return Util.readJSON(e.getAsJsonObject().get("url").getAsString());
            }
        }
        return null;
    }

}
