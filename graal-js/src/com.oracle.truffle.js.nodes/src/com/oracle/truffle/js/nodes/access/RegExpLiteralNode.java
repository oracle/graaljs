/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.access;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.RegexCompiler;
import com.oracle.truffle.js.runtime.builtins.JSRegExp;

public class RegExpLiteralNode extends JavaScriptNode {
    private final JSContext context;
    private final String pattern;
    private final String flags;

    @CompilationFinal private TruffleObject regex;

    RegExpLiteralNode(JSContext context, String pattern, String flags) {
        this.context = context;
        this.pattern = pattern;
        this.flags = flags;
    }

    public static RegExpLiteralNode create(JSContext context, String pattern, String flags) {
        return new RegExpLiteralNode(context, pattern, flags);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        if (regex == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            regex = RegexCompiler.compile(pattern, flags, context);
        }
        return JSRegExp.create(context, regex);
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return create(context, pattern, flags);
    }
}
