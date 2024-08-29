/*
 * Copyright (c) 2018, 2024, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.nodes;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.GenerateInline;
import com.oracle.truffle.api.dsl.Idempotent;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.api.nodes.LoopNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSLanguageOptions;
import com.oracle.truffle.js.runtime.JSRealm;

@TypeSystemReference(JSTypes.class)
@NodeInfo(language = "JavaScript", description = "The abstract base node for all JavaScript nodes")
@GenerateInline(value = false, inherit = true)
@ImportStatic(JSGuards.class)
public abstract class JavaScriptBaseNode extends Node {

    public JavaScriptBaseNode() {
        JSNodeUtil.NODE_CREATE_COUNT.inc();
    }

    @Override
    public JavaScriptBaseNode copy() {
        JSNodeUtil.NODE_CREATE_COUNT.inc();
        return (JavaScriptBaseNode) super.copy();
    }

    @Override
    protected void onReplace(Node newNode, CharSequence reason) {
        super.onReplace(newNode, reason);
        JSNodeUtil.NODE_REPLACE_COUNT.inc();
    }

    protected final JSRealm getRealm() {
        return JSRealm.get(this);
    }

    @Idempotent
    protected final JavaScriptLanguage getLanguage() {
        return JavaScriptLanguage.get(this);
    }

    @Idempotent
    protected final JSContext getJSContext() {
        return getLanguage().getJSContext();
    }

    @Idempotent
    protected final JSLanguageOptions getLanguageOptions() {
        return getJSContext().getLanguageOptions();
    }

    protected final boolean hasOverloadedOperators(Object obj) {
        assert !JSGuards.hasOverloadedOperators(obj) || getLanguageOptions().operatorOverloading();
        return (CompilerDirectives.inInterpreter() || getLanguageOptions().operatorOverloading()) && JSGuards.hasOverloadedOperators(obj);
    }

    public static void reportLoopCount(Node node, int count) {
        if (count > 0) {
            LoopNode.reportLoopCount(node, count);
        }
    }

    public static void reportLoopCount(Node node, long count) {
        if (count > 0) {
            LoopNode.reportLoopCount(node, count > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) count);
        }
    }
}
