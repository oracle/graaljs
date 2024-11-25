/*
 * Copyright (c) 2024, 2024, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.test.interop;

import static com.oracle.truffle.js.lang.JavaScriptLanguage.ID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.io.IOAccess;
import org.junit.Test;

import com.oracle.truffle.js.test.JSTest;

/**
 * Test loading modules from data URI.
 */
public class ESModuleDataURLTest {

    private static void commonCheck(Value v) {
        assertTrue(v.hasArrayElements());
        assertTrue(v.getArrayElement(0).isNumber());
        assertEquals(121, v.getArrayElement(0).asInt());
        assertTrue(v.getArrayElement(1).isNumber());
        assertEquals(5, v.getArrayElement(1).asInt());
        assertTrue(v.getArrayElement(2).isNumber());
        assertEquals(11, v.getArrayElement(2).asInt());
    }

    @Test
    public void testFunctionExport() {
        String functionExportModule = """
                        export const sqrt = Math.sqrt;
                        export function square(x) {
                            return x * x;
                        }
                        export function diag(x, y) {
                            return sqrt(square(x) + square(y));
                        }
                        """;
        String functionExportTest = """
                        import * as module from '%s'; // import everything that the module exports
                        var sq = module.square(11);
                        var dg = module.diag(3, 4);
                        var st = module.sqrt(121);
                        [sq, dg, st];
                        """;

        try (Context context = JSTest.newContextBuilder().allowIO(IOAccess.ALL).build()) {
            List<String> dataURLs = List.of(
                            makeDataURL(functionExportModule, "application/javascript;charset=UTF-8", false),
                            makeDataURL(functionExportModule, "application/javascript;charset=UTF-8", true),
                            makeDataURL(functionExportModule, "application/javascript", false, StandardCharsets.US_ASCII),
                            makeDataURL(functionExportModule, "application/javascript", true, StandardCharsets.US_ASCII),
                            makeDataURL(functionExportModule, "application/javascript;charset=ISO-8859-1", false, Charset.forName("ISO-8859-1")),
                            makeDataURL(functionExportModule, "application/javascript;charset=ISO-8859-1", true, Charset.forName("ISO-8859-1")),
                            makeDataURL(functionExportModule, "application/javascript;charset=ISO-8859-7", false, Charset.forName("ISO-8859-7")),
                            makeDataURL(functionExportModule, "application/javascript;charset=ISO-8859-7", true, Charset.forName("ISO-8859-7")),
                            makeDataURL(functionExportModule, "application/javascript;charset=", false, StandardCharsets.US_ASCII),
                            makeDataURL(functionExportModule, "application/javascript;charset=", true, StandardCharsets.US_ASCII),
                            makeDataURL(functionExportModule, "text/plain", false, StandardCharsets.US_ASCII),
                            makeDataURL(functionExportModule, "text/plain", true, StandardCharsets.US_ASCII),
                            makeDataURL(functionExportModule, "", false, StandardCharsets.US_ASCII),
                            makeDataURL(functionExportModule, "", true, StandardCharsets.US_ASCII));
            for (String dataURL : dataURLs) {
                String mainSourceText = functionExportTest.formatted(dataURL);
                Source mainSource = Source.newBuilder(ID, mainSourceText, "functionexporttest.mjs").buildLiteral();
                commonCheck(context.eval(mainSource));

                String dataURLUpperCase = dataURL.replace(";base64", ";BASE64").replace("charset=", "CHARSET=");
                mainSource = Source.newBuilder(ID, functionExportTest.formatted(dataURLUpperCase), "functionexporttest.mjs").buildLiteral();
                commonCheck(context.eval(mainSource));
            }
        }
    }

    @Test
    public void testCharset() {
        String mainSourceTemplate = """
                        import str from "%s";
                        export default str;
                        str;
                        """;

        try (Context context = JSTest.newContextBuilder().allowIO(IOAccess.ALL).build()) {
            Map<String, String> dataURLs = Map.of(
                            makeDataURL("export default '\u03B1'", "application/javascript;charset=UTF-8", false), "\u03B1",
                            makeDataURL("export default '\u03B1'", "application/javascript;charset=UTF-8", true), "\u03B1",
                            makeDataURL("export default '\u03B1'", "application/javascript;charset=ISO-8859-7", false, Charset.forName("ISO-8859-7")), "\u03B1",
                            makeDataURL("export default '\u03B1'", "application/javascript;charset=ISO-8859-7", true, Charset.forName("ISO-8859-7")), "\u03B1",
                            makeDataURL("export default '\u00F6'", "application/javascript;charset=ISO-8859-1", false, Charset.forName("ISO-8859-1")), "\u00F6",
                            makeDataURL("export default '\u00F6'", "application/javascript;charset=ISO-8859-1", true, Charset.forName("ISO-8859-1")), "\u00F6",
                            makeDataURL("export default '\u00F6'", "application/javascript", false, StandardCharsets.ISO_8859_1), "\uFFFD",
                            makeDataURL("export default '\u00F6'", "application/javascript", true, StandardCharsets.ISO_8859_1), "\uFFFD");
            dataURLs.forEach((dataURL, expected) -> {
                String mainSourceText = mainSourceTemplate.formatted(dataURL);
                Source mainSource = Source.newBuilder(ID, mainSourceText, "main.mjs").buildLiteral();
                assertEquals(expected, context.eval(mainSource).asString());
            });
        }
    }

    private static String makeDataURL(String code, String mimeType, boolean base64) {
        return makeDataURL(code, mimeType, base64, StandardCharsets.UTF_8);
    }

    private static String makeDataURL(String code, String mimeType, boolean base64, Charset base64Charset) {
        return "data:" + mimeType + (base64 ? ";base64" : "") + "," + encodeForDataURI(code, base64, base64Charset);
    }

    private static String encodeForDataURI(String code, boolean base64, Charset charset) {
        assertTrue(charset.toString(), charset.newEncoder().canEncode(code));
        if (base64) {
            return Base64.getEncoder().encodeToString(code.getBytes(charset));
        } else {
            return percentEncode(code, charset);
        }
    }

    private static String percentEncode(String code, Charset charset) {
        if (charset == StandardCharsets.UTF_8 || charset == StandardCharsets.US_ASCII) {
            try {
                return new URI(null, null, code).getRawFragment();
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException(e);
            }
        } else {
            byte[] bytes = code.getBytes(charset);
            return IntStream.range(0, bytes.length).map(i -> Byte.toUnsignedInt(bytes[i])).mapToObj(c -> String.format("%%%02x", c)).collect(Collectors.joining());
        }
    }
}
