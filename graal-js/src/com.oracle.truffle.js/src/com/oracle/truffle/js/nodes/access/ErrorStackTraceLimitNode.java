/*
 * Copyright (c) 2018, 2025, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.cast.IsNumberNode;
import com.oracle.truffle.js.nodes.cast.JSToIntegerAsLongNode;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSErrorType;
import com.oracle.truffle.js.runtime.builtins.JSError;
import com.oracle.truffle.js.runtime.builtins.JSFunctionObject;
import com.oracle.truffle.js.runtime.objects.JSProperty;
import com.oracle.truffle.js.runtime.objects.Undefined;

public abstract class ErrorStackTraceLimitNode extends JavaScriptBaseNode {
    @Child private DynamicObject.GetNode getStackTraceLimit;
    @Child private DynamicObject.GetPropertyFlagsNode getStackTraceLimitFlags;

    protected ErrorStackTraceLimitNode() {
        this.getStackTraceLimit = DynamicObject.GetNode.create();
        this.getStackTraceLimitFlags = DynamicObject.GetPropertyFlagsNode.create();
    }

    public static ErrorStackTraceLimitNode create() {
        return ErrorStackTraceLimitNodeGen.create();
    }

    @Specialization
    protected final int doInt(
                    @Cached IsNumberNode isNumber,
                    @Cached JSToIntegerAsLongNode toInteger) {
        JSContext context = getJSContext();
        if (context.getLanguageOptions().stackTraceAPI()) {
            JSFunctionObject errorConstructor = getRealm().getErrorConstructor(JSErrorType.Error);
            if (JSProperty.isData(getStackTraceLimitFlags.execute(errorConstructor, JSError.STACK_TRACE_LIMIT_PROPERTY_NAME, JSProperty.ACCESSOR))) {
                Object value = getStackTraceLimit.execute(errorConstructor, JSError.STACK_TRACE_LIMIT_PROPERTY_NAME, Undefined.instance);
                if (isNumber.execute(this, value)) {
                    long limit = toInteger.executeLong(value);
                    return (int) Math.max(0, Math.min(limit, Integer.MAX_VALUE));
                }
            }
            return 0;
        } else {
            return context.getLanguageOptions().stackTraceLimit();
        }
    }

    public abstract int executeInt();

}
