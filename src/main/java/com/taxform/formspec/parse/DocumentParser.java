package com.taxform.formspec.parse;

import com.taxform.formspec.FormSpecException;
import com.taxform.formspec.SpecVersion;
import com.taxform.formspec.model.annotation.AnnotationDocument;
import com.taxform.formspec.model.binding.Binding;
import com.taxform.formspec.model.form.FormTemplate;
import com.taxform.formspec.model.page.FormPage;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

/**
 * Parses form templates and annotations and runs the semantic checks the
 * JSON Schemas cannot express: unique field ids, rects within the page
 * bounds, annotation/template agreement, and fieldRef existence. The only
 * Jackson touch-point in the codebase.
 */
public final class DocumentParser {

    public static final String SUPPORTED_SPEC_VERSION = SpecVersion.CURRENT;

    private static final JsonMapper MAPPER = JsonMapper.builder()
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    private DocumentParser() {
    }

    public static String readJson(Path file) {
        try {
            return Files.readString(file);
        } catch (IOException e) {
            throw new FormSpecException("Cannot read file: " + file, e);
        }
    }

    public static FormTemplate parseTemplate(String json) {
        FormTemplate template = parse(json, FormTemplate.class, "form template");
        validate(template);
        return template;
    }

    public static FormTemplate parseTemplate(Path file) {
        return parseTemplate(readJson(file));
    }

    public static Binding parseBinding(String json) {
        Binding binding = parse(json, Binding.class, "binding");
        checkSpecVersion(binding.specVersion(), "binding");
        if (!"binding".equals(binding.kind())) {
            throw new FormSpecException("kind must be \"binding\", got: " + binding.kind());
        }
        return binding;
    }

    /** Parses a binding and validates it against a template (form identity + fieldRefs). */
    public static Binding parseBinding(String json, FormTemplate template) {
        Binding binding = parseBinding(json);
        if (!binding.form().id().equals(template.form().id())
                || binding.form().taxYear() != template.form().taxYear()
                || !binding.form().version().equals(template.form().version())) {
            throw new FormSpecException("Binding targets " + describe(binding.form())
                    + " but template is " + describe(template.form()));
        }
        Set<String> fieldIds = new HashSet<>();
        for (FormPage page : template.pages()) {
            for (var field : page.fields()) {
                fieldIds.add(field.id());
            }
        }
        for (var entry : binding.fields()) {
            if (!fieldIds.contains(entry.fieldRef())) {
                throw new FormSpecException("fieldRef '" + entry.fieldRef()
                        + "' does not exist in template " + template.form().id()
                        + " " + template.form().version());
            }
        }
        return binding;
    }

    public static Binding parseBinding(Path file, FormTemplate template) {
        return parseBinding(readJson(file), template);
    }

    /** Parses an annotation document and validates it against a template (form identity + field refs). */
    public static AnnotationDocument parseAnnotationDocument(String json, FormTemplate template) {
        AnnotationDocument document = parse(json, AnnotationDocument.class, "annotation document");
        checkSpecVersion(document.specVersion(), "annotation document");
        if (!"annotation".equals(document.kind())) {
            throw new FormSpecException("kind must be \"annotation\", got: " + document.kind());
        }
        if (!document.form().id().equals(template.form().id())
                || document.form().taxYear() != template.form().taxYear()
                || !document.form().version().equals(template.form().version())) {
            throw new FormSpecException("Annotation document targets " + describe(document.form())
                    + " but template is " + describe(template.form()));
        }
        Set<String> fieldIds = new HashSet<>();
        for (FormPage page : template.pages()) {
            for (var field : page.fields()) {
                fieldIds.add(field.id());
            }
        }
        for (var annotation : document.annotations()) {
            if (!fieldIds.contains(annotation.fieldRef())) {
                throw new FormSpecException("field_ref '" + annotation.fieldRef()
                        + "' does not exist in template " + template.form().id()
                        + " " + template.form().version());
            }
        }
        return document;
    }

    public static AnnotationDocument parseAnnotationDocument(Path file, FormTemplate template) {
        return parseAnnotationDocument(readJson(file), template);
    }

    private static <T> T parse(String json, Class<T> type, String what) {
        try {
            return MAPPER.readValue(json, type);
        } catch (JacksonException e) {
            throw new FormSpecException("Invalid " + what + ": " + rootMessage(e), e);
        }
    }

    private static void validate(FormTemplate template) {
        if (!"form-template".equals(template.kind())) {
            throw new FormSpecException("kind must be \"form-template\", got: " + template.kind());
        }
        checkSpecVersion(template.specVersion(), "form template");
        Set<Integer> pageIndexes = new HashSet<>();
        Set<String> fieldIds = new HashSet<>();
        for (FormPage page : template.pages()) {
            if (!pageIndexes.add(page.index())) {
                throw new FormSpecException("Duplicate page index: " + page.index());
            }
            for (var field : page.fields()) {
                if (!fieldIds.add(field.id())) {
                    throw new FormSpecException("Duplicate field id: " + field.id());
                }
                if (!field.rect().within(template.pageSize())) {
                    throw new FormSpecException("Field '" + field.id() + "' rect " + field.rect()
                            + " lies outside page size " + template.pageSize().width()
                            + " x " + template.pageSize().height());
                }
            }
        }
    }

    private static void checkSpecVersion(String version, String what) {
        if (!SUPPORTED_SPEC_VERSION.equals(version)) {
            throw new FormSpecException("Unsupported specVersion '" + version + "' in " + what
                    + " (supported: " + SUPPORTED_SPEC_VERSION + ")");
        }
    }

    private static String describe(com.taxform.formspec.model.form.FormMeta form) {
        return form.id() + " taxYear " + form.taxYear() + " version " + form.version();
    }

    private static String describe(com.taxform.formspec.model.form.FormRef form) {
        return form.id() + " taxYear " + form.taxYear() + " version " + form.version();
    }

    /** Prefer the message of a FormSpecException buried in the cause chain. */
    private static String rootMessage(Throwable t) {
        Throwable current = t;
        Throwable deepest = t;
        while (current.getCause() != null) {
            current = current.getCause();
            if (current instanceof FormSpecException) {
                deepest = current;
            }
        }
        String message = deepest.getMessage();
        return message == null || message.isBlank() ? t.getMessage() : message;
    }
}
