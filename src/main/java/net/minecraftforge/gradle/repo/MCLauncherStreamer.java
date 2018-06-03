package net.minecraftforge.gradle.repo;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import net.minecraftforge.gradle.util.IOFunction;
import net.minecraftforge.gradle.util.StreamedResource;
import net.minecraftforge.gradle.util.Util;
import org.gradle.internal.Pair;
import org.gradle.internal.impldep.com.google.gson.JsonElement;
import org.gradle.internal.impldep.com.google.gson.JsonObject;
import org.gradle.internal.impldep.org.apache.commons.codec.digest.DigestUtils;
import org.gradle.internal.impldep.org.apache.commons.io.IOUtils;
import org.gradle.internal.resource.ResourceExceptions;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;

/**
 * A resource streamer for Minecraft client and server jars.
 */
public class MCLauncherStreamer implements IOFunction<URL, StreamedResource> {

    // A pattern that matches both .pom files and the client/server jars
    private static final Pattern PATTERN = Pattern.compile(
            "/net/minecraft/minecraft/(\\d\\.\\d+(?:\\.\\d))/minecraft-\\1(?:-(client|server-pure))?\\.(jar|pom)(?:\\.(md5|sha1))?");

    private Cache<String, Pair<String, String>> hashes = CacheBuilder.newBuilder().expireAfterAccess(500, TimeUnit.SECONDS).build();

    @Override
    public StreamedResource apply(URL url) throws IOException {
        Matcher matcher = PATTERN.matcher(url.getPath());
        if (!matcher.matches()) return null;

        String version = matcher.group(1);
        String classifier = matcher.group(2);
        String extension = matcher.group(3);
        String hash = matcher.group(4);

        if (hash != null) {
            String resourceID = version + ":" + classifier + ":" + extension;
            Pair<String, String> hashPair = hashes.getIfPresent(resourceID);
            if (hashPair == null) {
                hashes.put(resourceID, hashPair = genHashes(url, version, classifier, extension));
            }
            String hashString = hash.equals("md5") ? hashPair.getLeft() : hashPair.getRight();
            return new StreamedResource.ByteArrayStreamedResource(url, hashString.getBytes());
        }

        return getResource(url, version, classifier, extension);
    }

    private Pair<String, String> genHashes(URL url, String version, String classifier, String extension) throws IOException {
        StreamedResource res = getResource(url, version, classifier, extension);
        InputStream stream = res.getStream();

        byte[] buffer = new byte[stream.available()];
        IOUtils.readFully(stream, buffer);

        return Pair.of(DigestUtils.md5Hex(buffer), DigestUtils.sha1Hex(buffer));
    }

    private StreamedResource getResource(URL url, String version, String classifier, String extension) throws IOException {
        JsonObject manifest = getManifest(url, version);

        if (extension.equals("pom")) {
            try {
                return streamPOM(url, manifest);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        } else {
            String[] artifactInfo = classifier.split("-");
            JsonObject artifact = manifest.getAsJsonObject("downloads").getAsJsonObject(artifactInfo[0]);
            String jar = artifact.get("url").getAsString();
            if (classifier.equals("client")) {
                return new StreamedResource.URLStreamedResource(new URL(jar));
            } else {
                try {
                    JarInputStream is = new JarInputStream(new URL(jar).openStream());
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    JarOutputStream os = new JarOutputStream(baos);

                    JarEntry entry;
                    while ((entry = is.getNextJarEntry()) != null) {
                        if (!isServerEntryValid(entry)) continue;
                        os.putNextEntry(entry);
                        IOUtils.copyLarge(is, os, 0, entry.getSize());
                        os.closeEntry();
                    }

                    os.close();
                    is.close();

                    return new StreamedResource.ByteArrayStreamedResource(url, baos.toByteArray());
                } catch (Exception ex) {
                    ex.printStackTrace();
                    throw new RuntimeException(ex);
                }
            }
        }
    }

    private boolean isServerEntryValid(ZipEntry entry) {
        if(entry.isDirectory()) return false;
        String name = entry.getName();
        return !name.startsWith("org/bouncycastle/") && !name.startsWith("org/bouncycastle/") && !name.startsWith("org/apache/")
                && !name.startsWith("com/google/") && !name.startsWith("com/mojang/authlib/") && !name.startsWith("com/mojang/util/")
                && !name.startsWith("gnu/trove/") && !name.startsWith("io/netty/") && !name.startsWith("javax/annotation/")
                && !name.startsWith("argo/") && !name.startsWith("it/unimi/dsi/fastutil/");
    }

    private StreamedResource streamPOM(URL url, JsonObject manifest) throws Exception {
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
        Document doc = docBuilder.newDocument();

        Element project = doc.createElement("project");
        project.setAttribute("xsi:schemaLocation", "http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd");
        project.setAttribute("xmlns", "http://maven.apache.org/POM/4.0.0");
        project.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
        doc.appendChild(project);

        set(doc, project, "modelVersion", "4.0.0");
        set(doc, project, "groupId", "net.minecraft");
        set(doc, project, "artifactId", "minecraft");
        set(doc, project, "version", manifest.get("id").getAsString());
        set(doc, project, "name", "minecraft");
        set(doc, project, "description", "minecraft");

        Element repositories = doc.createElement("repositories");
        Element libsRepo = doc.createElement("repository");
        set(doc, libsRepo, "id", "mclibraries");
        set(doc, libsRepo, "url", "https://libraries.minecraft.net");
        repositories.appendChild(libsRepo);
        project.appendChild(repositories);

        Element dependencies = doc.createElement("dependencies");
        for (JsonElement libElement : manifest.getAsJsonArray("libraries")) {
            JsonObject lib = libElement.getAsJsonObject();

            // Ignore this dependency if anything says we shouldn't be using it
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
                if (shouldDownload != null && !shouldDownload) continue;
            }

            String name = lib.get("name").getAsString();
            JsonObject downloads = lib.getAsJsonObject("downloads");

            // Add main download if we need it
            if (downloads.has("artifact")) {
                addDependency(doc, dependencies, name, null, "compile");
            }

            if (downloads.has("classifiers")) {
                JsonObject classifiers = downloads.getAsJsonObject("classifiers");
                // Add test dependencies if needed
                if (classifiers.has("test")) {
                    addDependency(doc, dependencies, name, "test", "test");
                }
                // Locate natives if required
                if (lib.has("natives")) {
                    JsonElement classifier = lib.getAsJsonObject("natives").get(Util.getOS());
                    if (classifier != null) {
                        addDependency(doc, dependencies, name, classifier.getAsString(), "runtime");
                    }
                }
            }
        }
        project.appendChild(dependencies);

        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        DOMSource source = new DOMSource(doc);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        StreamResult result = new StreamResult(baos);
        transformer.transform(source, result);

        return new StreamedResource.ByteArrayStreamedResource(url, baos.toByteArray());
    }

    private void addDependency(Document doc, Element dependencies, String name, String classifier, String scope) {
        String[] nameElements = name.split(":");
        Element dep = doc.createElement("dependency");
        set(doc, dep, "groupId", nameElements[0]);
        set(doc, dep, "artifactId", nameElements[1]);
        set(doc, dep, "version", nameElements[2]);
        if (classifier != null) {
            set(doc, dep, "classifier", classifier);
        } else if (nameElements.length == 4) {
            set(doc, dep, "classifier", nameElements[3]);
        }
        set(doc, dep, "scope", scope);
        dependencies.appendChild(dep);
    }

    private void set(Document doc, Element parent, String name, String value) {
        Element description = doc.createElement(name);
        description.appendChild(doc.createTextNode(value));
        parent.appendChild(description);
    }

    private JsonObject getManifest(URL url, String version) {
        JsonObject manifest = Util.readJSON("https://launchermeta.mojang.com/mc/game/version_manifest.json");
        for (JsonElement e : manifest.getAsJsonArray("versions")) {
            String v = e.getAsJsonObject().get("id").getAsString();
            if (v.equals(version)) {
                return Util.readJSON(e.getAsJsonObject().get("url").getAsString());
            }
        }
        throw ResourceExceptions.failure(Util.getURI(url), "Could not find Minecraft version '" + version + "'.", null);
    }

}
