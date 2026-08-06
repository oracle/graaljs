/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NeverDefault;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;

/**
 * Get hash code for JS collections (i.e. Map or Set). The provided key must be normalized first.
 *
 * @see JSCollectionsNormalizeNode
 */
@ImportStatic(JSConfig.class)
@GenerateUncached
public abstract class JSCollectionsHashCodeNode extends JavaScriptBaseNode {

    public abstract int execute(Object key);

    @NeverDefault
    public static JSCollectionsHashCodeNode create() {
        return JSCollectionsHashCodeNodeGen.create();
    }

    public static JSCollectionsHashCodeNode getUncached() {
        return JSCollectionsHashCodeNodeGen.getUncached();
    }

    @Specialization
    static int doInt(int key) {
        return Integer.hashCode(key);
    }

    @Specialization
    static int doDouble(double key) {
        return Double.hashCode(key);
    }

    @Specialization
    static int doString(TruffleString key,
                    @Cached TruffleString.HashCodeNode hashCodeNode) {
        return hashCodeNode.execute(key, TruffleString.Encoding.UTF_16);
    }

    @Specialization
    static int doJSObject(JSDynamicObject key) {
        return key.identityHashCode();
    }

    @Specialization
    static int doBoolean(boolean key) {
        return Boolean.hashCode(key);
    }

    @Specialization
    static int doSymbol(Symbol key) {
        return key.hashCode();
    }

    @Specialization
    static int doBigInt(BigInt key) {
        return key.hashCode();
    }

    /**
     * @see JSCollectionsNormalizeNode#doForeignObject
     */
    @Specialization(guards = {"isForeignObject(key)", "interop.hasIdentity(key)"}, limit = "InteropLibraryLimit")
    static int doForeignObject(Object key,
                    @CachedLibrary("key") InteropLibrary interop) {
        // key must already be normalized
        try {
            return interop.identityHashCode(key);
        } catch (UnsupportedMessageException e) {
            throw Errors.shouldNotReachHere(e);
        }
    }

    @TruffleBoundary
    @Fallback
    static int doOther(Object key) {
        return key.hashCode();
    }
}
