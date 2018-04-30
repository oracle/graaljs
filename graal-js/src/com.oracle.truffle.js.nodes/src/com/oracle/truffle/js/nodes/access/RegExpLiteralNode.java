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
package com.oracle.truffle.js.nodes.access;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.instrumentation.JSTags;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.LiteralExpressionTag;
import com.oracle.truffle.js.nodes.intl.CreateRegExpNode;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.RegexCompilerInterface;

public class RegExpLiteralNode extends JavaScriptNode {
    private final JSContext context;
    private final String pattern;
    private final String flags;

    @CompilationFinal private TruffleObject regex;

    @Child private CreateRegExpNode createRegExpNode;

    @Override
    public boolean hasTag(Class<? extends Tag> tag) {
        if (tag == JSTags.LiteralExpressionTag.class) {
            return true;
        } else {
            return super.hasTag(tag);
        }
    }

    @Override
    public Object getNodeObject() {
        return JSTags.createNodeObjectDescriptor("type", LiteralExpressionTag.Type.RegExpLiteral.name());
    }

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
            regex = RegexCompilerInterface.compile(pattern, flags, context);
        }
        return getCreateRegExpNode().execute(regex);
    }

    private CreateRegExpNode getCreateRegExpNode() {
        if (createRegExpNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            createRegExpNode = insert(CreateRegExpNode.create(context));
        }
        return createRegExpNode;
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return create(context, pattern, flags);
    }
}
