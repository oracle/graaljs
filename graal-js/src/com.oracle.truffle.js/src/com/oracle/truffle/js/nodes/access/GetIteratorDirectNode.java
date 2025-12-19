/*
 * Copyright (c) 2022, 2025, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateInline;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NeverDefault;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.objects.IteratorRecord;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;

@ImportStatic({JSConfig.class})
@GenerateInline
@GenerateUncached
public abstract class GetIteratorDirectNode extends JavaScriptBaseNode {

    public final IteratorRecord execute(Object iterator) {
        return execute(null, iterator);
    }

    public abstract IteratorRecord execute(Node node, Object iterator);

    @Specialization
    protected IteratorRecord get(JSObject obj,
                    @Cached(value = "createGetNextNode()", uncached = "getNullNode()", inline = false) @Shared PropertyGetNode getNextMethodNode) {
        return getImpl(obj, getNextMethodNode);
    }

    @Specialization(guards = "isForeignObject(obj)")
    protected IteratorRecord get(Object obj,
                    @Cached(value = "createGetNextNode()", uncached = "getNullNode()", inline = false) @Shared PropertyGetNode getNextMethodNode,
                    @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary interop) {
        JSRealm realm = JSRealm.get(this);
        // java.util.Iterator.next() does not have the needed semantics
        // => use next() method from the prototype
        if (interop.isHostObject(obj) && interop.isIterator(obj)) {
            JSDynamicObject prototype = realm.getForeignIteratorPrototype();
            Object nextMethod = (getNextMethodNode == null) ? JSObject.get(prototype, Strings.NEXT) : getNextMethodNode.getValue(prototype);
            return IteratorRecord.create(obj, nextMethod, false);
        }
        return getImpl(obj, getNextMethodNode);
    }

    private static IteratorRecord getImpl(Object obj, PropertyGetNode getNextMethodNode) {
        Object nextMethod = (getNextMethodNode == null) ? JSRuntime.get(obj, Strings.NEXT) : getNextMethodNode.getValue(obj);
        return IteratorRecord.create(obj, nextMethod, false);
    }

    @Fallback
    public IteratorRecord unsupported(Object obj) {
        throw Errors.createTypeErrorNotAnObject(obj, this);
    }

    @NeverDefault
    PropertyGetNode createGetNextNode() {
        return PropertyGetNode.create(Strings.NEXT, getJSContext());
    }

    @NeverDefault
    public static GetIteratorDirectNode create() {
        return GetIteratorDirectNodeGen.create();
    }

}
