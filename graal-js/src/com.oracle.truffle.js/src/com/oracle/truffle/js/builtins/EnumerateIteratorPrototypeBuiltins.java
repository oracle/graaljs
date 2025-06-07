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
package com.oracle.truffle.js.builtins;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.StopIterationException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;
import com.oracle.truffle.js.builtins.EnumerateIteratorPrototypeBuiltinsFactory.EnumerateNextNodeGen;
import com.oracle.truffle.js.nodes.access.CreateIterResultObjectNode;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.nodes.interop.ImportValueNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * Prototype of [[Enumerate]]().
 */
public final class EnumerateIteratorPrototypeBuiltins extends JSBuiltinsContainer.SwitchEnum<EnumerateIteratorPrototypeBuiltins.EnumerateIteratorPrototype> {
    public static final JSBuiltinsContainer BUILTINS = new EnumerateIteratorPrototypeBuiltins();

    protected EnumerateIteratorPrototypeBuiltins() {
        super(JSFunction.ENUMERATE_ITERATOR_PROTOTYPE_NAME, EnumerateIteratorPrototype.class);
    }

    public enum EnumerateIteratorPrototype implements BuiltinEnum<EnumerateIteratorPrototype> {
        next(0);

        private final int length;

        EnumerateIteratorPrototype(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, EnumerateIteratorPrototype builtinEnum) {
        switch (builtinEnum) {
            case next:
                return EnumerateNextNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
        }
        return null;
    }

    @ImportStatic({JSConfig.class})
    public abstract static class EnumerateNextNode extends JSBuiltinNode {
        @Child private PropertyGetNode getIteratorNode;

        public EnumerateNextNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            this.getIteratorNode = PropertyGetNode.createGetHidden(JSRuntime.ENUMERATE_ITERATOR_ID, context);
        }

        @Specialization
        public JSDynamicObject next(Object target,
                        @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary interop,
                        @Cached("create(getContext())") CreateIterResultObjectNode createIterResultObjectNode,
                        @Cached ImportValueNode importValueNode,
                        @Cached InlinedBranchProfile errorBranch) {
            Object iterator = getIteratorNode.getValue(target);
            if (iterator == Undefined.instance) {
                errorBranch.enter(this);
                throw Errors.createTypeError("Enumerate iterator required");
            }
            try {
                if (interop.hasIteratorNextElement(iterator)) {
                    try {
                        Object nextValue = interop.getIteratorNextElement(iterator);
                        Object importedValue = importValueNode.executeWithTarget(nextValue);
                        return createIterResultObjectNode.execute(importedValue, false);
                    } catch (StopIterationException e) {
                        // fall through
                    }
                }
                return createIterResultObjectNode.execute(Undefined.instance, true);
            } catch (UnsupportedMessageException e) {
                errorBranch.enter(this);
                throw Errors.createTypeErrorInteropException(iterator, e, "next", null);
            }
        }

    }
}
