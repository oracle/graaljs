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
package com.oracle.truffle.js.runtime.builtins;

import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.nodes.function.BuiltinArgumentBuilder;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Strings;

public interface BuiltinEnum<E extends Enum<? extends BuiltinEnum<E>>> {
    @SuppressWarnings("unchecked")
    default E asEnum() {
        return (E) this;
    }

    default TruffleString getName() {
        return prependAccessorPrefix(JSRuntime.propertyKeyToFunctionNameString(getKey()));
    }

    default Object getKey() {
        return stripName(Strings.fromJavaString(asEnum().name()));
    }

    default boolean isConstructor() {
        return false;
    }

    default boolean isNewTargetConstructor() {
        return false;
    }

    int getLength();

    default boolean isEnabled() {
        return true;
    }

    default boolean isAOTSupported() {
        return true;
    }

    default int getECMAScriptVersion() {
        return 5;
    }

    default boolean isAnnexB() {
        return false;
    }

    default boolean isWritable() {
        return true;
    }

    default boolean isConfigurable() {
        return true;
    }

    default boolean isEnumerable() {
        return false;
    }

    default boolean isGetter() {
        return false;
    }

    default boolean isSetter() {
        return false;
    }

    default boolean isOptional() {
        return isConstructor();
    }

    @SuppressWarnings("unused")
    default Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget) {
        throw new UnsupportedOperationException();
    }

    default BuiltinArgumentBuilder args() {
        return BuiltinArgumentBuilder.builder();
    }

    static TruffleString stripName(TruffleString name) {
        if (Strings.endsWith(name, Strings.UNDERSCORE) && !Strings.endsWith(name, Strings.UNDERSCORE_2)) {
            // just one char is leaked, lazy substring is OK here
            return Strings.lazySubstring(name, 0, Strings.length(name) - 1);
        } else {
            return name;
        }
    }

    default TruffleString prependAccessorPrefix(TruffleString name) {
        if (isGetter()) {
            return Strings.concat(Strings.GET_SPC, name);
        } else if (isSetter()) {
            return Strings.concat(Strings.SET_SPC, name);
        }
        return name;
    }
}
