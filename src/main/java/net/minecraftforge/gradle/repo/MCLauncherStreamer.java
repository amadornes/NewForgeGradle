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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A resource streamer for Minecraft client and server jars.
 */
public class MCLauncherStreamer implements IOFunction<URL, StreamedResource> {

    // A pattern that matches both .pom files and the client/server jars
    private static final Pattern PATTERN = Pattern.compile(
            "/net/minecraft/minecraft/(\\d\\.\\d+(?:\\.\\d))/minecraft-\\1(?:-(client|server))?\\.(jar|pom)(?:\\.(md5|sha1))?");

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
            JsonObject artifact = manifest.getAsJsonObject("downloads").getAsJsonObject(classifier);
            String jar = artifact.get("url").getAsString();
            return new StreamedResource.URLStreamedResource(new URL(jar));
        }
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

        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        DOMSource source = new DOMSource(doc);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        StreamResult result = new StreamResult(baos);
        transformer.transform(source, result);

        return new StreamedResource.ByteArrayStreamedResource(url, baos.toByteArray());
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
