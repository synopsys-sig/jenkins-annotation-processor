/*
 * jenkins-annotation-processor
 *
 * Copyright (c) 2022 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.jenkins.annotations;

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

import org.apache.commons.lang3.exception.ExceptionUtils;

@SupportedSourceVersion(SourceVersion.RELEASE_11)
public class HelpMarkdownProcessor extends AbstractProcessor {
    private Filer filer;
    private Messager messager;
    private Elements elementUtils;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        filer = processingEnv.getFiler();
        messager = processingEnv.getMessager();
        elementUtils = processingEnv.getElementUtils();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        try {
            if (roundEnv.processingOver()) {
                return true;
            }

            HelpHtmlGenerator helpHtmlGenerator = new HelpHtmlGenerator(filer);

            Set<TypeElement> typeElements = getAnnotatedClassesAsTypeElements(roundEnv.getRootElements(), annotations);
            for (TypeElement typeElement : typeElements) {
                String packageString = elementUtils.getPackageOf(typeElement).getQualifiedName().toString();
                String typeName = typeElement.getSimpleName().toString();
                String resourcePackage = packageString + "." + typeName;

                for (VariableElement variableElement : ElementFilter.fieldsIn(typeElement.getEnclosedElements())) {
                    helpHtmlGenerator.generateHelpHtmlFromAnnotation(resourcePackage, variableElement);
                }
            }
        } catch (Exception e) {
            messager.printMessage(Diagnostic.Kind.ERROR, ExceptionUtils.getStackTrace(e));
        }
        return true;
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Stream.of(HelpMarkdown.class.getCanonicalName()).collect(Collectors.toSet());
    }

    private Set<TypeElement> getAnnotatedClassesAsTypeElements(Set<? extends Element> elements, Set<? extends Element> supportedAnnotations) {
        return elements.stream()
                   .filter(TypeElement.class::isInstance)
                   .map(TypeElement.class::cast)
                   .filter(element -> elementContainsSupportedAnnotation(element, supportedAnnotations))
                   .collect(Collectors.toSet());
    }

    private boolean elementContainsSupportedAnnotation(Element element, Set<? extends Element> supportedAnnotations) {
        return element.getEnclosedElements().stream()
                   .map(Element::getAnnotationMirrors)
                   .flatMap(List::stream)
                   .map(AnnotationMirror::getAnnotationType)
                   .map(DeclaredType::asElement)
                   .anyMatch(supportedAnnotations::contains);
    }
}
