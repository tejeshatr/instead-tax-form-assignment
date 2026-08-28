package com.taxform.formspec.demo;

import com.taxform.formspec.fill.FillPlan;
import com.taxform.formspec.fill.FormFiller;
import com.taxform.formspec.model.annotation.AnnotationDocument;
import com.taxform.formspec.model.binding.Binding;
import com.taxform.formspec.model.form.FormTemplate;
import com.taxform.formspec.parse.DocumentParser;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Demo entry point: parses the shipped 1040 example template, binding,
 * annotation document, and sample data, then prints the dry-run fill plan.
 * Lives outside the library sources (src/demo/java) to show how a consumer
 * application uses the spec.
 */
public final class DryRunDemo {

    private static final String TEMPLATE = "examples/form-templates/f1040-2025.template.json";
    private static final String BINDING = "examples/bindings/f1040-2025.binding.json";
    private static final String ANNOTATION = "examples/annotations/f1040-2025.annotation.json";
    private static final String DATA = "examples/sample-data/taxpayer-1.json";

    private DryRunDemo() {
    }

    public static FillPlan run() {
        Path base = Paths.get(System.getProperty("user.dir", "."));
        FormTemplate template = DocumentParser.parseTemplate(base.resolve(TEMPLATE));
        Binding binding = DocumentParser.parseBinding(base.resolve(BINDING), template);
        AnnotationDocument annotation = DocumentParser.parseAnnotationDocument(base.resolve(ANNOTATION), template);
        String data = DocumentParser.readJson(base.resolve(DATA));
        return FormFiller.fill(template, binding, annotation, data);
    }

    public static void main(String[] args) {
        System.out.println(run().render());
    }
}
