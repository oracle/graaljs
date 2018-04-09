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
package com.oracle.truffle.js.nodes.function;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.PropertyDescriptor;

public class SetFunctionNameNode extends JavaScriptBaseNode {
    private final ConditionProfile isSymbolProfile;

    protected SetFunctionNameNode() {
        this.isSymbolProfile = ConditionProfile.createBinaryProfile();
    }

    public static SetFunctionNameNode create() {
        return new SetFunctionNameNode();
    }

    public Object execute(Object functionValue, Object propertyKey) {
        return execute(functionValue, propertyKey, null);
    }

    public Object execute(Object functionValue, Object propertyKey, String prefix) {
        assert JSFunction.isJSFunction(functionValue);
        assert JSRuntime.isPropertyKey(propertyKey);
        String name = isSymbolProfile.profile(propertyKey instanceof Symbol) ? ((Symbol) propertyKey).toFunctionNameString() : (String) propertyKey;
        if (prefix != null && !prefix.isEmpty()) {
            name = concatenate(prefix, name);
        }

        return setFunctionName((DynamicObject) functionValue, name);
    }

    @TruffleBoundary
    private static String concatenate(String prefix, String name) {
        return new StringBuilder(prefix.length() + 1 + name.length()).append(prefix).append(' ').append(name).toString();
    }

    private static Object setFunctionName(DynamicObject functionValue, String name) {
        PropertyDescriptor propDesc = PropertyDescriptor.createData(name, false, false, true);
        JSRuntime.definePropertyOrThrow(functionValue, JSFunction.NAME, propDesc, JSObject.getJSContext(functionValue));
        return functionValue;
    }
}
