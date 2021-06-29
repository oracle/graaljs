/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.builtins.helper;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.Record;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.Tuple;
import com.oracle.truffle.js.runtime.builtins.JSSet;
import com.oracle.truffle.js.runtime.interop.JSInteropUtil;
import com.oracle.truffle.js.runtime.objects.JSLazyString;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * This implements behavior for Collections of ES6. Instead of adhering to the SameValueNull
 * algorithm, we normalize the key (e.g., transform the double value 1.0 to an integer value of 1).
 */
@ImportStatic({JSConfig.class})
@GenerateUncached
public abstract class JSCollectionsNormalizeNode extends JavaScriptBaseNode {

    public abstract Object execute(Object operand);

    public static JSCollectionsNormalizeNode create() {
        return JSCollectionsNormalizeNodeGen.create();
    }

    @Specialization
    public int doInt(int value) {
        return value;
    }

    @Specialization
    public Object doDouble(double value) {
        return JSSet.normalizeDouble(value);
    }

    @Specialization
    public String doJSLazyString(JSLazyString value,
                    @Cached("createBinaryProfile()") ConditionProfile flatten) {
        return value.toString(flatten);
    }

    @Specialization
    public String doString(String value) {
        return value;
    }

    @Specialization
    public boolean doBoolean(boolean value) {
        return value;
    }

    @Specialization(guards = "isJSDynamicObject(object)")
    public Object doDynamicObject(DynamicObject object) {
        return object;
    }

    @Specialization
    public Symbol doSymbol(Symbol value) {
        return value;
    }

    @Specialization
    public BigInt doBigInt(BigInt bigInt) {
        return bigInt;
    }

    @TruffleBoundary
    @Specialization
    public Record doRecord(Record record) {
        Map<String, Object> fields = record.getEntries().stream().collect(Collectors.toMap(
                it -> it.getKey(),
                it -> execute(it.getValue())
        ));
        return Record.create(fields);
    }

    @Specialization
    public Tuple doTuple(Tuple tuple) {
        Object[] elements = tuple.getElements();
        for (int i = 0; i < elements.length; i++) {
            elements[i] = execute(elements[i]);
        }
        return Tuple.create(elements);
    }

    @Specialization(guards = "isForeignObject(object)", limit = "InteropLibraryLimit")
    public Object doForeignObject(Object object,
                    @CachedLibrary("object") InteropLibrary interop,
                    @Cached("createBinaryProfile()") ConditionProfile primitiveProfile,
                    @Cached("create()") JSCollectionsNormalizeNode nestedNormalizeNode) {
        Object primitive = JSInteropUtil.toPrimitiveOrDefault(object, null, interop, this);
        return primitiveProfile.profile(primitive == null) ? object : nestedNormalizeNode.execute(primitive);
    }

}
