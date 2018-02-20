/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.test.external.suite;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.PrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * Utility methods to work with JSON file format.
 */
public final class JSONUtil {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    static {
        OBJECT_MAPPER.setDefaultPrettyPrinter(new NewlineAddingPrettyPrinter(OBJECT_MAPPER.getSerializationConfig().getDefaultPrettyPrinter()));
        OBJECT_MAPPER.enable(SerializationFeature.INDENT_OUTPUT);
        OBJECT_MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    private JSONUtil() {
        assert false;
    }

    public static void serialize(File result, Object object) throws IOException {
        OBJECT_MAPPER.writeValue(result, object);
    }

    public static <T> T deserialize(File source, Class<T> type) throws IOException {
        return OBJECT_MAPPER.readValue(source, type);
    }

    public static void checkFormatting(File file, Object object) throws IncorrectFormattingException, IOException {
        String fileContent = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        if (!fileContent.equals(OBJECT_MAPPER.writeValueAsString(object))) {
            throw new IncorrectFormattingException("File content of " + file.getName() + " not properly formatted.");
        }
    }

    // ~ Inner classes

    public static final class IncorrectFormattingException extends Exception {

        private static final long serialVersionUID = -1346876843476876454L;

        public IncorrectFormattingException(String message) {
            super(message);
        }

    }

    private static final class NewlineAddingPrettyPrinter implements PrettyPrinter {

        private final PrettyPrinter delegate;
        private int depth = 0;

        NewlineAddingPrettyPrinter(PrettyPrinter delegate) {
            assert delegate != null;
            this.delegate = delegate;
        }

        @Override
        public void writeRootValueSeparator(JsonGenerator jsonGenerator) throws IOException, JsonGenerationException {
            delegate.writeRootValueSeparator(jsonGenerator);
        }

        @Override
        public void writeStartObject(JsonGenerator jsonGenerator) throws IOException, JsonGenerationException {
            delegate.writeStartObject(jsonGenerator);
            ++depth;
        }

        @Override
        public void writeEndObject(JsonGenerator jsonGenerator, int i) throws IOException, JsonGenerationException {
            delegate.writeEndObject(jsonGenerator, i);
            if (--depth == 0) {
                jsonGenerator.writeRaw(System.lineSeparator());
            }
        }

        @Override
        public void writeObjectEntrySeparator(JsonGenerator jsonGenerator) throws IOException, JsonGenerationException {
            delegate.writeObjectEntrySeparator(jsonGenerator);
        }

        @Override
        public void writeObjectFieldValueSeparator(JsonGenerator jsonGenerator) throws IOException, JsonGenerationException {
            delegate.writeObjectFieldValueSeparator(jsonGenerator);
        }

        @Override
        public void writeStartArray(JsonGenerator jsonGenerator) throws IOException, JsonGenerationException {
            delegate.writeStartArray(jsonGenerator);
        }

        @Override
        public void writeEndArray(JsonGenerator jsonGenerator, int i) throws IOException, JsonGenerationException {
            delegate.writeEndArray(jsonGenerator, i);
        }

        @Override
        public void writeArrayValueSeparator(JsonGenerator jsonGenerator) throws IOException, JsonGenerationException {
            delegate.writeArrayValueSeparator(jsonGenerator);
        }

        @Override
        public void beforeArrayValues(JsonGenerator jsonGenerator) throws IOException, JsonGenerationException {
            delegate.beforeArrayValues(jsonGenerator);
        }

        @Override
        public void beforeObjectEntries(JsonGenerator jsonGenerator) throws IOException, JsonGenerationException {
            delegate.beforeObjectEntries(jsonGenerator);
        }

    }

}
