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
package com.oracle.truffle.js.scriptengine;

import java.util.AbstractMap;
import java.util.Map;
import java.util.Set;

import javax.script.Bindings;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.TypeLiteral;
import org.graalvm.polyglot.Value;

final class GraalJSBindings extends AbstractMap<String, Object> implements Bindings {

    private static final TypeLiteral<Map<String, Object>> STRING_MAP = new TypeLiteral<Map<String, Object>>() {
    };

    private final Context context;
    private final Map<String, Object> global;
    private Value deleteProperty;
    private Value clear;

    GraalJSBindings(Context context) {
        this.context = context;
        this.global = GraalJSScriptEngine.evalInternal(context, "this").as(STRING_MAP);
    }

    private Value deletePropertyFunction() {
        if (this.deleteProperty == null) {
            this.deleteProperty = GraalJSScriptEngine.evalInternal(context, "(function(obj, prop) {delete obj[prop]})");
        }
        return this.deleteProperty;
    }

    private Value clearFunction() {
        if (this.clear == null) {
            this.clear = GraalJSScriptEngine.evalInternal(context, "(function(obj) {for (var prop in obj) {delete obj[prop]}})");
        }
        return this.clear;
    }

    @Override
    public Object put(String name, Object v) {
        return global.put(name, v);
    }

    @Override
    public void clear() {
        clearFunction().execute(global);
    }

    @Override
    public Object get(Object key) {
        return global.get(key);
    }

    @Override
    public Object remove(Object key) {
        Object prev = get(key);
        deletePropertyFunction().execute(global, key);
        return prev;
    }

    public Context getContext() {
        return context;
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
        return global.entrySet();
    }

}
