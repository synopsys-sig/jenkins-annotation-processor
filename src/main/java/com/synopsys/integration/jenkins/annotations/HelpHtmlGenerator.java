/*
 * jenkins-annotation-processor
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.jenkins.annotations;

import java.io.IOException;
import java.io.Writer;
import java.util.Collections;

import javax.annotation.processing.Filer;
import javax.lang.model.element.VariableElement;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

import org.apache.commons.lang3.StringUtils;

import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;

public class HelpHtmlGenerator {
    private final Filer filer;
    private final Parser parser;
    private final HtmlRenderer renderer;

    public HelpHtmlGenerator(final Filer filer) {
        this.filer = filer;
        final MutableDataSet options = new MutableDataSet();
        options.set(Parser.EXTENSIONS, Collections.singleton(TablesExtension.create()));
        options.set(Parser.CODE_SOFT_LINE_BREAKS, true);

        this.parser = Parser.builder(options).build();
        this.renderer = HtmlRenderer.builder(options).build();
    }

    public void generateHelpHtmlFromAnnotation(final String resourcePackage, final VariableElement variableElement) throws IOException {
        final HelpMarkdown helpMarkdown = variableElement.getAnnotation(HelpMarkdown.class);
        if (helpMarkdown != null && StringUtils.isNotBlank(helpMarkdown.value())) {
            final String fileName = "help-" + variableElement.getSimpleName().toString() + ".html";
            final String mdContents = "<div>\r\n\r\n" + helpMarkdown.value() + "\r\n</div>";

            final Node document = parser.parse(mdContents);
            final String fileContents = renderer.render(document);

            final FileObject helpHtmlFile = filer.createResource(StandardLocation.CLASS_OUTPUT, resourcePackage, fileName);
            try (Writer writer = helpHtmlFile.openWriter()) {
                writer.write(fileContents);
            }
        }
    }

}
