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
package com.oracle.truffle.js.nodes.cast;

import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.objects.JSLazyString;

/**
 * @see JSToBooleanUnaryNode
 */
@GenerateUncached
@ImportStatic({JSConfig.class})
public abstract class JSToBooleanNode extends JavaScriptBaseNode {

    protected JSToBooleanNode() {
    }

    public abstract boolean executeBoolean(Object value);

    public static JSToBooleanNode create() {
        return JSToBooleanNodeGen.create();
    }

    @Specialization
    protected static boolean doBoolean(boolean value) {
        return value;
    }

    @Specialization(guards = "isJSNull(value)")
    protected static boolean doNull(@SuppressWarnings("unused") Object value) {
        return false;
    }

    @Specialization(guards = "isUndefined(value)")
    protected static boolean doUndefined(@SuppressWarnings("unused") Object value) {
        return false;
    }

    @Specialization
    protected static boolean doInt(int value) {
        return value != 0;
    }

    @Specialization
    protected static boolean doLong(long value) {
        return value != 0L;
    }

    @Specialization
    protected static boolean doDouble(double value) {
        return value != 0.0 && !Double.isNaN(value);
    }

    @Specialization
    protected static boolean doBigInt(BigInt value) {
        return value.compareTo(BigInt.ZERO) != 0;
    }

    @Specialization
    protected static boolean doLazyString(JSLazyString value) {
        return value.length() != 0;
    }

    @Specialization
    protected static boolean doString(String value) {
        return value.length() != 0;
    }

    @Specialization(guards = "isJSObject(value)")
    protected static boolean doObject(@SuppressWarnings("unused") Object value) {
        return true;
    }

    @Specialization
    protected static boolean doSymbol(@SuppressWarnings("unused") Symbol value) {
        return true;
    }

    @Specialization(guards = "isForeignObject(value)", limit = "InteropLibraryLimit")
    protected final boolean doForeignObject(Object value,
                    @CachedLibrary("value") InteropLibrary interop) {
        if (interop.isNull(value)) {
            return false;
        }
        try {
            if (interop.isBoolean(value)) {
                return interop.asBoolean(value);
            } else if (interop.isString(value)) {
                return !interop.asString(value).isEmpty();
            } else if (interop.isNumber(value)) {
                if (interop.fitsInInt(value)) {
                    return doInt(interop.asInt(value));
                } else if (interop.fitsInLong(value)) {
                    return doLong(interop.asLong(value));
                } else if (interop.fitsInDouble(value)) {
                    return doDouble(interop.asDouble(value));
                } else {
                    return true;
                }
            }
        } catch (UnsupportedMessageException e) {
            throw Errors.createTypeErrorUnboxException(value, e, this);
        }
        return true; // cf. doObject()
    }
}
