package com.synopsys.integration.jenkins;

import java.io.IOException;
import java.util.Collections;

import javax.annotation.processing.Filer;
import javax.lang.model.element.TypeElement;

import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.data.MutableDataSet;

public class JellyGenerator {
    private final Filer filer;
    private final Parser parser;
    private final HtmlRenderer renderer;

    public JellyGenerator(final Filer filer) {
        this.filer = filer;
        final MutableDataSet options = new MutableDataSet();
        options.set(Parser.EXTENSIONS, Collections.singleton(TablesExtension.create()));
        options.set(Parser.CODE_SOFT_LINE_BREAKS, true);

        this.parser = Parser.builder(options).build();
        this.renderer = HtmlRenderer.builder(options).build();
    }

    public void generateJellyFromAnnotations(final String resourcePackage, final TypeElement typeElement) throws IOException {
        final GenerateJelly generateJelly = typeElement.getAnnotation(GenerateJelly.class);
        if (generateJelly != null && generateJelly.value().length != 0) {
            // Generate Jelly
        }
    }

}
