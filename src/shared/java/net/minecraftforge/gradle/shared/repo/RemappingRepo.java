package net.minecraftforge.gradle.shared.repo;

import net.minecraftforge.gradle.api.mapping.MappingManager;
import net.minecraftforge.gradle.api.mapping.MappingVersion;
import net.minecraftforge.gradle.shared.mappings.Remapper;
import net.minecraftforge.gradle.shared.util.StreamedResource;
import net.minecraftforge.gradle.shared.util.Util;
import org.gradle.api.Project;
import org.gradle.api.artifacts.ArtifactIdentifier;
import org.gradle.api.artifacts.dsl.RepositoryHandler;
import org.gradle.internal.impldep.com.beust.jcommander.internal.Maps;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RemappingRepo {

    private static final Pattern PATTERN_MAPPING = Pattern.compile(
            "^remapped\\.(?<provider>[^.]+)\\.(?<channel>[^.]+)\\.(?<version>[^.]+)\\.(?<mapping>[^.]+)\\.(?<group>.*)$");

    public static CustomRepository add(Project project, AtomicInteger counter, String mcVersion, String name, Object url, MappingManager manager) {
        RepositoryHandler handler = project.getRepositories();
        return CustomRepository.add(handler, name, url, new ArtifactProvider(project, counter, manager, mcVersion), null);
    }

    private static class ArtifactProvider implements CustomRepository.ArtifactProvider {

        private final Project project;
        private final AtomicInteger counter;
        private final MappingManager manager;
        private String mcVersion;

        public ArtifactProvider(Project project, AtomicInteger counter, MappingManager manager, String mcVersion) {
            this.project = project;
            this.counter = counter;
            this.manager = manager;
            this.mcVersion = mcVersion;
        }

        @Override
        public StreamedResource getArtifact(ArtifactIdentifier identifier) throws IOException {
            String group = identifier.getModuleVersionIdentifier().getGroup();
            Matcher matcher = PATTERN_MAPPING.matcher(group);
            if (!matcher.matches()) return null;

            String provider = matcher.group("provider");
            String channel = matcher.group("channel");
            String version = matcher.group("version");
            String mappingName = matcher.group("mapping");
            String newGroup = matcher.group("group");

            Set<File> files = Util.resolveDependency(project, counter, Maps.newHashMap(
                    "group", newGroup,
                    "name", identifier.getModuleVersionIdentifier().getName(),
                    "version", identifier.getModuleVersionIdentifier().getVersion(),
                    "classifier", identifier.getClassifier(),
                    "ext", identifier.getExtension(),
                    "transitive", false
            ));
            if (files.isEmpty()) return null;

            if (identifier.getExtension().equals("pom")) {
                try {
                    return fixPOM(files.iterator().next(), group);
                } catch (SAXException | ParserConfigurationException | TransformerException ex) {
                    return null;
                }
            }

            MappingVersion mapping = new MappingVersion(provider, channel, version, mcVersion, mappingName);
            Set<File> remapped = Remapper.remap(manager, mapping, files);
            return new StreamedResource.URLStreamedResource(remapped.iterator().next().toURL());
        }

        private StreamedResource fixPOM(File pomFile, String group) throws IOException, SAXException, ParserConfigurationException, TransformerException {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document doc = docBuilder.parse(pomFile);

            Element docElement = doc.getDocumentElement();
            docElement.normalize();

            NodeList groupIdNodes = docElement.getElementsByTagName("groupId");
            for (int i = 0; i < groupIdNodes.getLength(); i++) {
                Node node = groupIdNodes.item(0);
                node.removeChild(node.getFirstChild());
                node.appendChild(doc.createTextNode(group));
            }

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            StreamResult result = new StreamResult(baos);
            transformer.transform(source, result);

            return new StreamedResource.ByteArrayStreamedResource(baos.toByteArray());
        }

    }

}
