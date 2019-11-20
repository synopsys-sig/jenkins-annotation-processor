/**
 * jenkins-annotation-processor
 *
 * Copyright (c) 2019 Synopsys, Inc.
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
package com.synopsys.integration.jenkins;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

import org.apache.commons.lang3.StringUtils;

import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;

@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class Processor extends AbstractProcessor {
    private Filer filer;
    private Messager messager;
    private Elements elementUtils;

    @Override
    public synchronized void init(final ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        filer = processingEnv.getFiler();
        messager = processingEnv.getMessager();
        elementUtils = processingEnv.getElementUtils();
    }

    @Override
    public boolean process(final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnv) {
        try {
            if (roundEnv.processingOver()) {
                return true;
            }

            final Set<TypeElement> typeElements = getAnnotatedClassesAsTypeElements(roundEnv.getRootElements(), annotations);
            for (final TypeElement typeElement : typeElements) {
                final String packageString = elementUtils.getPackageOf(typeElement).getQualifiedName().toString();
                final String typeName = typeElement.getSimpleName().toString();
                final String resourcePackage = packageString + "." + typeName;

                for (final VariableElement variableElement : ElementFilter.fieldsIn(typeElement.getEnclosedElements())) {
                    generateHelpHtmlFromAnnotation(resourcePackage, variableElement);
                }
            }
        } catch (final Exception e) {
            messager.printMessage(Diagnostic.Kind.ERROR, e.toString());
            Arrays.stream(e.getStackTrace()).forEach(traceElement -> messager.printMessage(Diagnostic.Kind.ERROR, traceElement.toString()));
        }
        return true;
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Stream.of(HelpMarkdown.class.getCanonicalName()).collect(Collectors.toSet());
    }

    private void generateHelpHtmlFromAnnotation(final String resourcePackage, final VariableElement variableElement) throws IOException {
        final HelpMarkdown helpMarkdown = variableElement.getAnnotation(HelpMarkdown.class);
        if (helpMarkdown != null && StringUtils.isNotBlank(helpMarkdown.value())) {
            final String fileName = "help-" + variableElement.getSimpleName().toString() + ".html";
            final String mdContents = "<div>\r\n\r\n" + helpMarkdown.value() + "\r\n</div>";

            final MutableDataSet options = new MutableDataSet();
            options.set(Parser.EXTENSIONS, Collections.singleton(TablesExtension.create()));
            options.set(Parser.CODE_SOFT_LINE_BREAKS, true);

            final Parser parser = Parser.builder(options).build();
            final Node document = parser.parse(mdContents);
            final HtmlRenderer renderer = HtmlRenderer.builder(options).build();
            final String fileContents = renderer.render(document);

            final FileObject helpHtmlFile = filer.createResource(StandardLocation.CLASS_OUTPUT, resourcePackage, fileName);
            try (Writer writer = helpHtmlFile.openWriter()) {
                writer.write(fileContents);
            }
        }
    }

    private Set<TypeElement> getAnnotatedClassesAsTypeElements(final Set<? extends Element> elements, final Set<? extends Element> supportedAnnotations) {
        return elements.stream()
                   .filter(TypeElement.class::isInstance)
                   .map(TypeElement.class::cast)
                   .filter(element -> elementContainsSupportedAnnotation(element, supportedAnnotations))
                   .collect(Collectors.toSet());
    }

    private boolean elementContainsSupportedAnnotation(final Element element, final Set<? extends Element> supportedAnnotations) {
        return element.getEnclosedElements().stream()
                   .map(Element::getAnnotationMirrors)
                   .flatMap(List::stream)
                   .map(AnnotationMirror::getAnnotationType)
                   .map(DeclaredType::asElement)
                   .anyMatch(supportedAnnotations::contains);
    }
}
