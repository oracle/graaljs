/*
 * Copyright (c) 2020, 2020, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.runtime.truffleinterop;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.js.lang.JavaScriptLanguage;

/**
 * General meta objects for JS values and foreign objects.
 */
@ExportLibrary(InteropLibrary.class)
public final class JSMetaType implements TruffleObject {

    @FunctionalInterface
    public interface TypeCheck {
        boolean check(InteropLibrary lib, Object value);
    }

    public static final JSMetaType NULL = new JSMetaType("null", InteropLibrary::isNull);
    public static final JSMetaType BOOLEAN = new JSMetaType("boolean", InteropLibrary::isBoolean);
    public static final JSMetaType STRING = new JSMetaType("string", InteropLibrary::isString);
    public static final JSMetaType NUMBER = new JSMetaType("number", InteropLibrary::isNumber);
    public static final JSMetaType FUNCTION = new JSMetaType("function", InteropLibrary::isExecutable);
    public static final JSMetaType ARRAY = new JSMetaType("array", InteropLibrary::hasArrayElements);
    public static final JSMetaType OBJECT = new JSMetaType("object", InteropLibrary::hasMembers);

    @CompilationFinal(dimensions = 1) static final JSMetaType[] KNOWN_TYPES = new JSMetaType[]{NULL, BOOLEAN, STRING, NUMBER, FUNCTION, ARRAY, OBJECT};

    private final String typeName;
    private final TypeCheck isInstance;

    public JSMetaType(String typeName, TypeCheck isInstance) {
        this.typeName = typeName;
        this.isInstance = isInstance;
    }

    /**
     * Checks whether {@code instance} is of this meta type. If used on the fast path the
     * {@link JSMetaType} is required to be a PE-constant.
     */
    public boolean isInstance(Object instance, InteropLibrary interop) {
        CompilerAsserts.partialEvaluationConstant(this);
        return isInstance.check(interop, instance);
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    boolean hasLanguage() {
        return true;
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    Class<? extends TruffleLanguage<?>> getLanguage() {
        return JavaScriptLanguage.class;
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    boolean isMetaObject() {
        return true;
    }

    @ExportMessage(name = "getMetaQualifiedName")
    @ExportMessage(name = "getMetaSimpleName")
    public String getTypeName() {
        return typeName;
    }

    @ExportMessage(name = "toDisplayString")
    Object toDisplayString(@SuppressWarnings("unused") boolean allowSideEffects) {
        return typeName;
    }

    @TruffleBoundary
    @Override
    public String toString() {
        return "JSMetaType[" + typeName + "]";
    }

    @ExportMessage
    static class IsMetaInstance {
        @Specialization(guards = "type == cachedType", limit = "3")
        static boolean doCached(@SuppressWarnings("unused") JSMetaType type, Object value,
                        @Cached("type") JSMetaType cachedType,
                        @CachedLibrary("value") InteropLibrary valueLib) {
            return cachedType.isInstance.check(valueLib, value);
        }

        @TruffleBoundary
        @Specialization(replaces = "doCached")
        static boolean doGeneric(JSMetaType type, Object value) {
            return type.isInstance.check(InteropLibrary.getFactory().getUncached(), value);
        }
    }

}
