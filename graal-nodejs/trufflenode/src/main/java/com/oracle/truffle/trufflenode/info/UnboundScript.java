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
package com.oracle.truffle.trufflenode.info;

import java.io.File;
import java.nio.ByteBuffer;

import com.oracle.js.parser.ir.FunctionNode;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.js.runtime.AbstractJavaScriptLanguage;

/**
 *
 * @author Jan Stola
 */
public final class UnboundScript {
    private static final boolean IS_WINDOWS = System.getProperty("os.name").contains("Windows");
    private static int lastId;
    private final int id;
    private final Source source;
    private final Object parseResult;

    private UnboundScript(Source source, Object parseResult, int id) {
        this.source = source;
        this.parseResult = parseResult;
        this.id = id;
        assert parseResult instanceof FunctionNode || parseResult instanceof ByteBuffer;
    }

    public UnboundScript(Source source, Object parseResult) {
        this(source, parseResult, ++lastId);
    }

    public UnboundScript(Script script) {
        this(script.getScriptNode().getRootNode().getSourceSection().getSource(), script.getParseResult(), script.getId());
    }

    public static Source createSource(String code, String fileName) {
        String name = sourcefileName(fileName);
        Source source;
        if (isCoreModule(name)) {
            // Core modules are kept in memory (i.e. there is no file that contains the source)
            source = Source.newBuilder(code).name(name).mimeType(AbstractJavaScriptLanguage.APPLICATION_MIME_TYPE).build();
        } else {
            // We have the content already, but we need to associate the Source
            // with the corresponding file so that debugger knows where to add
            // the breakpoints.
            source = Source.newBuilder(new File(name)).content(code).name(name).mimeType(AbstractJavaScriptLanguage.APPLICATION_MIME_TYPE).build();
        }
        return source;
    }

    public static boolean isCoreModule(String name) {
        // All but core modules are represented by an absolute path
        // NB: Returns true for repl, [eval], [eval]-wrapper and other
        // names of scripts which are not in files.
        if (IS_WINDOWS) {
            return !name.contains(":");
        } else {
            return !name.startsWith("/");
        }
    }

    private static String sourcefileName(String fileName) {
        return (fileName == null) ? "unknown source" : fileName;
    }

    public int getId() {
        return id;
    }

    public Source getSource() {
        return source;
    }

    public Object getParseResult() {
        return parseResult;
    }
}
