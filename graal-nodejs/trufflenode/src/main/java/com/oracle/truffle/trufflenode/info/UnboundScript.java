/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
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
