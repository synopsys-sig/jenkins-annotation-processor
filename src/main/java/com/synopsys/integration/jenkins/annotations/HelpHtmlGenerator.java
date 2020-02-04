/**
 * jenkins-annotation-processor
 *
 * Copyright (c) 2020 Synopsys, Inc.
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
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
