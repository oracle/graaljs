/*
 * Copyright (c) 2021, 2021, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.js.builtins.JSBuiltinsContainer;
import com.oracle.truffle.js.builtins.foreign.ForeignIterablePrototypeBuiltinsFactory.IteratorNodeGen;
import com.oracle.truffle.js.nodes.access.PropertySetNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSOrdinary;

/**
 * The prototype of foreign iterable objects, fulfilling the JS Iterable contract. Provides
 * an @@iterator method that returns a JS wrapper around the foreign iterator, so that it conforms
 * to the JS iterator protocol, i.e., has a next method that returns an iterator result object of
 * the form <code>{value: any, done: boolean}</code>.
 */
public final class ForeignIterablePrototypeBuiltins extends JSBuiltinsContainer.SwitchEnum<ForeignIterablePrototypeBuiltins.ForeignIterablePrototype> {
    public static final JSBuiltinsContainer BUILTINS = new ForeignIterablePrototypeBuiltins();

    protected ForeignIterablePrototypeBuiltins() {
        super(null, ForeignIterablePrototype.class);
    }

    public enum ForeignIterablePrototype implements BuiltinEnum<ForeignIterablePrototype> {
        _iterator(0);

        private final int length;

        ForeignIterablePrototype(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }

        @Override
        public Object getKey() {
            return this == _iterator ? Symbol.SYMBOL_ITERATOR : BuiltinEnum.super.getKey();
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, ForeignIterablePrototype builtinEnum) {
        switch (builtinEnum) {
            case _iterator:
                return IteratorNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
        }
        return null;
    }

    @ImportStatic({JSConfig.class})
    public abstract static class IteratorNode extends JSBuiltinNode {
        @Child private PropertySetNode setEnumerateIteratorNode;
        private final BranchProfile errorBranch;

        public IteratorNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            this.errorBranch = BranchProfile.create();
            this.setEnumerateIteratorNode = PropertySetNode.createSetHidden(JSRuntime.ENUMERATE_ITERATOR_ID, context);
        }

        @Specialization
        protected DynamicObject execute(Object target,
                        @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary interop) {
            if (!interop.hasIterator(target)) {
                errorBranch.enter();
                throw Errors.createTypeErrorNotIterable(target, null);
            }
            Object iterator;
            try {
                iterator = interop.getIterator(target);
            } catch (UnsupportedMessageException e) {
                errorBranch.enter();
                throw Errors.createTypeErrorInteropException(target, e, "getIterator", null);
            }
            DynamicObject iteratorObj = JSOrdinary.create(getContext(), getContext().getEnumerateIteratorFactory(), getRealm());
            setEnumerateIteratorNode.setValue(iteratorObj, iterator);
            return iteratorObj;
        }
    }
}
