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
package com.oracle.truffle.js.nodes.access;

import java.util.Set;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.HostCompilerDirectives.InliningCutoff;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Executed;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NeverDefault;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.cast.JSToObjectNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.JSAdapter;
import com.oracle.truffle.js.runtime.builtins.JSForInIterator;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunctionObject;
import com.oracle.truffle.js.runtime.builtins.JSOrdinary;
import com.oracle.truffle.js.runtime.builtins.JSString;
import com.oracle.truffle.js.runtime.interop.EmptyIterator;
import com.oracle.truffle.js.runtime.interop.InteropArrayIndexIterator;
import com.oracle.truffle.js.runtime.interop.InteropMemberIterator;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;

/**
 * Returns an Iterator object iterating over the enumerable properties of an object.
 */
@ImportStatic({JSConfig.class})
public abstract class EnumerateNode extends JavaScriptNode {
    /** Enumerate values instead of keys (used by for-each-in loop). */
    private final boolean values;
    /** If true, throw a TypeError for foreign objects that do not have elements or members. */
    private final boolean requireIterable;
    protected final JSContext context;
    @Child @Executed protected JavaScriptNode targetNode;
    @Child private PropertySetNode setEnumerateIteratorNode;

    protected EnumerateNode(JSContext context, boolean values, boolean requireIterable, JavaScriptNode targetNode) {
        this.context = context;
        this.values = values;
        this.requireIterable = requireIterable;
        this.targetNode = targetNode;
    }

    public static EnumerateNode create(JSContext context, JavaScriptNode target, boolean values) {
        return EnumerateNodeGen.create(context, values, false, target);
    }

    public static EnumerateNode create(JSContext context, boolean values, boolean requireIterable) {
        return EnumerateNodeGen.create(context, values, requireIterable, null);
    }

    @NeverDefault
    EnumerateNode copyRecursive() {
        return create(context, values, requireIterable);
    }

    @Override
    public abstract JSObject execute(VirtualFrame frame);

    public abstract JSObject execute(Object iteratedObject);

    @Override
    protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
        return EnumerateNodeGen.create(context, values, requireIterable, cloneUninitialized(targetNode, materializedTags));
    }

    @Specialization(guards = {"!isJSAdapter(iteratedObject)"})
    protected JSObject doEnumerateObject(JSObject iteratedObject) {
        return newForInIterator(iteratedObject);
    }

    @Specialization(guards = {"isNullOrUndefined(iteratedObject)"})
    protected JSObject doEnumerateNullOrUndefined(@SuppressWarnings("unused") Object iteratedObject) {
        return newEmptyIterator();
    }

    @Specialization(guards = "isJSAdapter(iteratedObject)")
    protected JSObject doEnumerateJSAdapter(JSObject iteratedObject,
                    @Cached("createValues()") EnumerateNode enumerateCallbackResultNode) {
        JSDynamicObject adaptee = JSAdapter.getAdaptee(iteratedObject);
        assert JSRuntime.isObject(adaptee);

        Object getIds = JSObject.get(adaptee, values ? JSAdapter.GET_VALUES : JSAdapter.GET_IDS);
        if (JSFunction.isJSFunction(getIds)) {
            Object returnValue = JSFunction.call((JSFunctionObject) getIds, adaptee, JSArguments.EMPTY_ARGUMENTS_ARRAY);
            if (JSRuntime.isObject(returnValue)) {
                return enumerateCallbackResultNode.execute(returnValue);
            }
        }
        return newEmptyIterator();
    }

    @NeverDefault
    EnumerateNode createValues() {
        return create(context, true, false);
    }

    @SuppressWarnings("truffle-static-method")
    @InliningCutoff
    @Specialization(guards = {"isForeignObject(iteratedObject)"}, limit = "InteropLibraryLimit")
    protected JSObject doEnumerateTruffleObject(Object iteratedObject,
                    @Bind Node node,
                    @CachedLibrary("iteratedObject") InteropLibrary interop,
                    @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary keysInterop,
                    @Cached InlinedBranchProfile notIterable,
                    @Cached TruffleString.SwitchEncodingNode switchEncoding) {
        try {
            if (!interop.isNull(iteratedObject)) {
                if (values) {
                    if (interop.hasIterator(iteratedObject)) {
                        Object iterator = interop.getIterator(iteratedObject);
                        return newEnumerateIterator(iterator);
                    }
                } else /* keys */ {
                    if (interop.hasArrayElements(iteratedObject)) {
                        return newEnumerateIterator(InteropArrayIndexIterator.create(iteratedObject));
                    }
                }

                if (interop.isString(iteratedObject)) {
                    return enumerateString(Strings.interopAsTruffleString(iteratedObject, interop, switchEncoding));
                }

                if (interop.hasHashEntries(iteratedObject)) {
                    Object iterator = values ? interop.getHashValuesIterator(iteratedObject) : interop.getHashKeysIterator(iteratedObject);
                    return newEnumerateIterator(iterator);
                }

                if (interop.hasMembers(iteratedObject)) {
                    Object keysObj = interop.getMembers(iteratedObject);
                    assert InteropLibrary.getUncached().hasArrayElements(keysObj);
                    long longSize = keysInterop.getArraySize(keysObj);
                    return newEnumerateIterator(InteropMemberIterator.create(values, iteratedObject, keysObj, longSize));
                }
            }
        } catch (UnsupportedMessageException e) {
            // fall through
        }

        // object is not iterable; throw an error or return an empty iterator.
        notIterable.enter(node);
        if (requireIterable) {
            throw Errors.createTypeErrorNotIterable(iteratedObject, this);
        }
        return newEmptyIterator();
    }

    private JSObject enumerateString(TruffleString string) {
        return newForInIterator(JSString.create(context, getRealm(), string));
    }

    private JSObject newEmptyIterator() {
        return newEnumerateIterator(EmptyIterator.create());
    }

    private JSObject newEnumerateIterator(Object iterator) {
        if (setEnumerateIteratorNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            setEnumerateIteratorNode = insert(PropertySetNode.createSetHidden(JSRuntime.ENUMERATE_ITERATOR_ID, context));
        }
        JSObject obj = JSOrdinary.create(context, context.getEnumerateIteratorFactory(), getRealm());
        setEnumerateIteratorNode.setValue(obj, iterator);
        return obj;
    }

    private JSObject newForInIterator(JSObject obj) {
        return JSForInIterator.create(context, getRealm(), obj, values);
    }

    @Specialization(guards = {"!isJSDynamicObject(iteratedObject)", "!isForeignObject(iteratedObject)"})
    protected JSObject doNonObject(Object iteratedObject,
                    @Cached JSToObjectNode toObjectNode,
                    @Cached("copyRecursive()") EnumerateNode enumerateNode) {
        return enumerateNode.execute(toObjectNode.execute(iteratedObject));
    }
}
