package com.sca.integration.jenkins.annotations;

import com.synopsys.integration.jenkins.annotations.HelpMarkdown;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Name;
import javax.lang.model.element.VariableElement;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class HelpHtmlGeneratorTest {
    @Test
    public void testGenerateHelpHtmlFromAnnotation() throws IOException {
        String variableName = "variableName";
        String annotationContent = "* foo\r\n* bar";
        String resourcePackage = "com.synopsys.integration.jenkins.Example";

        String expectedFileName = "help-" + variableName + ".html";
        List<String> expectedFileContents = Arrays.asList("<div>", "<ul>", "<li>foo</li>", "<li>bar</li>", "</ul>", "</div>", "");

        Path testHelpHtmlOutput = Files.createTempFile("test-" + expectedFileName, null);
        testHelpHtmlOutput.toFile().deleteOnExit();

        // Set up mocks
        FileObject mockFileObject = Mockito.mock(FileObject.class);
        Mockito.when(mockFileObject.openWriter())
            .thenReturn(Files.newBufferedWriter(testHelpHtmlOutput));
        Filer mockFiler = Mockito.mock(Filer.class);
        Mockito.when(mockFiler.createResource(StandardLocation.CLASS_OUTPUT, resourcePackage, expectedFileName))
            .thenReturn(mockFileObject);
        Name mockedName = Mockito.mock(Name.class);
        Mockito.when(mockedName.toString())
            .thenReturn(variableName);
        HelpMarkdown mockedHelpMarkdown = Mockito.mock(HelpMarkdown.class);
        Mockito.when(mockedHelpMarkdown.value())
            .thenReturn(annotationContent);
        VariableElement mockedVariableElement = Mockito.mock(VariableElement.class);
        Mockito.when(mockedVariableElement.getAnnotation(HelpMarkdown.class))
            .thenReturn(mockedHelpMarkdown);
        Mockito.when(mockedVariableElement.getSimpleName())
            .thenReturn(mockedName);

        // Test
        HelpHtmlGenerator helpHtmlGenerator = new HelpHtmlGenerator(mockFiler);
        try {
            helpHtmlGenerator.generateHelpHtmlFromAnnotation(resourcePackage, mockedVariableElement);
        } catch (IOException e) {
            fail("Unexpected IOException thrown-- test should be intercepting file creation so this shouldn't be happening, please review the test code.", e);
        }

        List<String> actualFileContents = Files.readAllLines(testHelpHtmlOutput);
        for (int i = 0; i < actualFileContents.size(); i++) {
            assertEquals(expectedFileContents.get(i), actualFileContents.get(i));
        }
    }
}
