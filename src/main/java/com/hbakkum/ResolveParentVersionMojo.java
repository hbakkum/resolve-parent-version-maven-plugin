package com.hbakkum;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * This mojo updates the pom file for a module with the resolved parent version. That is, it ensures that for any poms that have a parent module reference, that
 * when this pom gets installed/deployed it does not include any property placeholders in the parent module version field.
 *
 **/
@Mojo(name = "resolve-parent-version", defaultPhase = LifecyclePhase.GENERATE_RESOURCES, requiresProject = true, requiresDirectInvocation = false, executionStrategy = "once-per-session")
public class ResolveParentVersionMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project.build.directory}", readonly = true, required = true)
    private File outputDirectory;

    @Parameter(property = "outputPomFilename", defaultValue = "resolved-parent-version-pom.xml")
    private String outputPomFilename;

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    public void execute() throws MojoExecutionException {
        MavenProject parentProject = project.getParent();
        if (parentProject == null) {
            getLog().warn("No parent project found - nothing to do");
            return;
        }

        Document doc = parsePomFile();

        Element parentVersionElement = getParentVersionElement(doc);
        if (parentVersionElement == null) {
            getLog().warn("Failed to find parent version element, doing nothing");
            return;
        }
        parentVersionElement.setTextContent(parentProject.getVersion());

        Path outputFile = outputDirectory.toPath().resolve(outputPomFilename);
        writeResolvedParentVersionPom(doc, outputFile);

        project.setPomFile(outputFile.toFile());
    }

    private Document parsePomFile() throws MojoExecutionException {
        File pomFile = project.getFile();

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db;
        try {
            db = dbf.newDocumentBuilder();

        } catch (ParserConfigurationException e) {
            throw new MojoExecutionException("Failed to create dom parser", e);
        }

        try {
            return db.parse(pomFile);

        } catch (SAXException e) {
            throw new MojoExecutionException("Failed to parse pom file", e);

        } catch (IOException e) {
            throw new MojoExecutionException("Failed to parse pom file", e);
        }
    }

    private Element getParentVersionElement(Document doc) throws MojoExecutionException {
        XPath xPath = XPathFactory.newInstance().newXPath();

        try {
            NodeList nodes = ((NodeList) xPath.evaluate("/project/parent/version", doc.getDocumentElement(), XPathConstants.NODESET));
            if (nodes.getLength() == 0) {
                return null;
            }

            return (Element) nodes.item(0);

        } catch (XPathExpressionException e) {
            throw new MojoExecutionException("Failed to evaluate xpath expression", e);
        }
    }

    private void writeResolvedParentVersionPom(Document doc, Path outputFile) throws MojoExecutionException {
        Transformer transformer;
        try {
            transformer = TransformerFactory.newInstance().newTransformer();

        } catch (TransformerConfigurationException e) {
            throw new MojoExecutionException("Failed to create xml transformer", e);
        }

        if (!outputDirectory.exists()) {
            try {
                Files.createDirectory(outputDirectory.toPath());

            } catch (IOException e) {
                throw new MojoExecutionException("Failed to create output directory", e);
            }
        }

        try {
            outputFile = Files.createFile(outputFile);

        } catch (IOException e) {
            throw new MojoExecutionException("Failed to create output file", e);
        }

        Result output = new StreamResult(outputFile.toFile());
        Source input = new DOMSource(doc);
        try {
            transformer.transform(input, output);

        } catch (TransformerException e) {
            throw new MojoExecutionException("Failed to write output file", e);
        }
    }

}
