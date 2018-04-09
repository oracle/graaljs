/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The Universal Permissive License (UPL), Version 1.0
 *
 * Subject to the condition set forth below, permission is hereby granted to any
 * person obtaining a copy of this software, associated documentation and/or
 * data (collectively the "Software"), free of charge and under any and all
 * copyright rights in the Software, and any and all patent rights owned or
 * freely licensable by each licensor hereunder covering either (i) the
 * unmodified Software as contributed to or provided by such licensor, or (ii)
 * the Larger Works (as defined below), to deal in both
 *
 * (a) the Software, and
 *
 * (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 * one is included with the Software each a "Larger Work" to which the Software
 * is contributed by such licensors),
 *
 * without restriction, including without limitation the rights to copy, create
 * derivative works of, display, perform, and distribute the Software and make,
 * use, sell, offer for sale, import, export, have made, and have sold the
 * Software and the Larger Work(s), and to sublicense the foregoing rights on
 * either these or other terms.
 *
 * This license is subject to the following condition:
 *
 * The above copyright notice and either this complete permission notice or at a
 * minimum a reference to the UPL must be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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
