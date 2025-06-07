/*
 * Copyright (c) 2023, 2025, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.builtins.foreign;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.StopIterationException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;
import com.oracle.truffle.js.builtins.JSBuiltinsContainer;
import com.oracle.truffle.js.builtins.foreign.ForeignIteratorPrototypeBuiltinsFactory.NextNodeGen;
import com.oracle.truffle.js.nodes.access.CreateIterResultObjectNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.nodes.interop.ImportValueNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * Prototype of foreign iterator objects.
 */
public final class ForeignIteratorPrototypeBuiltins extends JSBuiltinsContainer.SwitchEnum<ForeignIteratorPrototypeBuiltins.ForeignIteratorPrototype> {
    public static final JSBuiltinsContainer BUILTINS = new ForeignIteratorPrototypeBuiltins();

    protected ForeignIteratorPrototypeBuiltins() {
        super(null, ForeignIteratorPrototype.class);
    }

    public enum ForeignIteratorPrototype implements BuiltinEnum<ForeignIteratorPrototype> {
        next(0);

        private final int length;

        ForeignIteratorPrototype(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, ForeignIteratorPrototype builtinEnum) {
        switch (builtinEnum) {
            case next:
                return NextNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
        }
        return null;
    }

    @ImportStatic({JSConfig.class})
    public abstract static class NextNode extends JSBuiltinNode {

        public NextNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected JSObject next(Object target,
                        @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary interop,
                        @Cached("create(getContext())") CreateIterResultObjectNode createIterResult,
                        @Cached ImportValueNode importValue,
                        @Cached InlinedBranchProfile errorBranch) {
            if (!interop.isIterator(target)) {
                errorBranch.enter(this);
                throw Errors.createTypeErrorNotIterator(target, this);
            }

            boolean hasNext;
            try {
                hasNext = interop.hasIteratorNextElement(target);
            } catch (UnsupportedMessageException umex) {
                errorBranch.enter(this);
                throw Errors.createTypeErrorInteropException(target, umex, "hasIteratorNextElement", null);
            }

            Object nextValue = Undefined.instance;
            if (hasNext) {
                try {
                    nextValue = importValue.executeWithTarget(interop.getIteratorNextElement(target));
                } catch (UnsupportedMessageException umex) {
                    errorBranch.enter(this);
                    throw Errors.createTypeErrorInteropException(target, umex, "getIteratorNextElement", null);
                } catch (StopIterationException siex) {
                    hasNext = false;
                }
            }

            return createIterResult.execute(nextValue, !hasNext);
        }
    }
}
