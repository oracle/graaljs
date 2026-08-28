/*
 * Copyright (c) 2020, 2026, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.builtins.wasm;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NeverDefault;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.exception.AbstractTruffleException;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;
import com.oracle.truffle.js.builtins.JSBuiltinsContainer;
import com.oracle.truffle.js.builtins.wasm.WebAssemblyTablePrototypeBuiltinsFactory.WebAssemblyTableGetLengthNodeGen;
import com.oracle.truffle.js.builtins.wasm.WebAssemblyTablePrototypeBuiltinsFactory.WebAssemblyTableGetNodeGen;
import com.oracle.truffle.js.builtins.wasm.WebAssemblyTablePrototypeBuiltinsFactory.WebAssemblyTableGrowNodeGen;
import com.oracle.truffle.js.builtins.wasm.WebAssemblyTablePrototypeBuiltinsFactory.WebAssemblyTableSetNodeGen;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.nodes.wasm.ToJSValueNode;
import com.oracle.truffle.js.nodes.wasm.AddressValueToU64Node;
import com.oracle.truffle.js.nodes.wasm.ToWebAssemblyValueNode;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.wasm.JSWebAssemblyTable;
import com.oracle.truffle.js.runtime.builtins.wasm.JSWebAssemblyTableObject;
import com.oracle.truffle.js.runtime.builtins.wasm.WebAssemblyType;
import com.oracle.truffle.js.runtime.objects.Undefined;

public class WebAssemblyTablePrototypeBuiltins extends JSBuiltinsContainer.SwitchEnum<WebAssemblyTablePrototypeBuiltins.WebAssemblyTablePrototype> {

    public static final JSBuiltinsContainer BUILTINS = new WebAssemblyTablePrototypeBuiltins();

    protected WebAssemblyTablePrototypeBuiltins() {
        super(JSWebAssemblyTable.PROTOTYPE_NAME, WebAssemblyTablePrototype.class);
    }

    public enum WebAssemblyTablePrototype implements BuiltinEnum<WebAssemblyTablePrototype> {
        grow(1),
        get(1),
        set(1),
        length(0);

        private final int functionLength;

        WebAssemblyTablePrototype(int length) {
            this.functionLength = length;
        }

        @Override
        public int getLength() {
            return functionLength;
        }

        @Override
        public boolean isEnumerable() {
            return true;
        }

        @Override
        public boolean isGetter() {
            return this == length;
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, WebAssemblyTablePrototype builtinEnum) {
        switch (builtinEnum) {
            case grow:
                return WebAssemblyTableGrowNodeGen.create(context, builtin, args().withThis().fixedArgs(1).varArgs().createArgumentNodes(context));
            case get:
                return WebAssemblyTableGetNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case set:
                return WebAssemblyTableSetNodeGen.create(context, builtin, args().withThis().fixedArgs(1).varArgs().createArgumentNodes(context));
            case length:
                return WebAssemblyTableGetLengthNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
        }
        return null;
    }

    @ImportStatic(JSConfig.class)
    public abstract static class WebAssemblyTableGrowNode extends JSBuiltinNode {

        public WebAssemblyTableGrowNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object grow(Object thiz, Object delta, Object[] args,
                        @Cached("createToDeltaNode()") AddressValueToU64Node toDeltaNode,
                        @Cached ToWebAssemblyValueNode toWebAssemblyValueNode,
                        @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary tableGrowLib,
                        @Cached InlinedBranchProfile errorBranch) {
            if (!JSWebAssemblyTable.isJSWebAssemblyTable(thiz)) {
                errorBranch.enter(this);
                throw Errors.createTypeError("WebAssembly.Table.grow(): Receiver is not a WebAssembly.Table");
            }
            JSWebAssemblyTableObject table = (JSWebAssemblyTableObject) thiz;
            long deltaSize = toDeltaNode.execute(delta, table.hasIndexType64());
            Object wasmTable = table.getWASMTable();
            WebAssemblyType elementKind = table.getElementKind();

            final JSRealm realm = getRealm();
            final Object wasmValue;
            if (args.length == 0) {
                wasmValue = elementKind.getDefaultValue(realm);
            } else {
                wasmValue = toWebAssemblyValueNode.execute(args[0], elementKind);
            }
            try {
                Object growFn = realm.getWASMTableGrow();
                Object previousSize = tableGrowLib.execute(growFn, wasmTable, deltaSize, wasmValue);
                return table.hasIndexType64() ? BigInt.valueOfUnsigned(JSRuntime.longValue((Number) previousSize)) : previousSize;
            } catch (InteropException ex) {
                throw Errors.shouldNotReachHere(ex);
            } catch (AbstractTruffleException ex) {
                errorBranch.enter(this);
                throw Errors.createRangeError(ex, this);
            }
        }

        @NeverDefault
        protected static AddressValueToU64Node createToDeltaNode() {
            return AddressValueToU64Node.create("WebAssembly.Table.grow(): Argument 0");
        }

    }

    @ImportStatic(JSConfig.class)
    public abstract static class WebAssemblyTableGetNode extends JSBuiltinNode {

        public WebAssemblyTableGetNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object get(Object thiz, Object index,
                        @Cached("createToIndexNode()") AddressValueToU64Node toIndexNode,
                        @Cached ToJSValueNode toJSValueNode,
                        @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary tableGetLib,
                        @Cached InlinedBranchProfile errorBranch) {
            if (!JSWebAssemblyTable.isJSWebAssemblyTable(thiz)) {
                errorBranch.enter(this);
                throw Errors.createTypeError("WebAssembly.Table.get(): Receiver is not a WebAssembly.Table");
            }
            JSRealm realm = getRealm();
            JSWebAssemblyTableObject table = (JSWebAssemblyTableObject) thiz;
            long tableIndex = toIndexNode.execute(index, table.hasIndexType64());
            Object wasmTable = table.getWASMTable();
            try {
                Object getFn = realm.getWASMTableRead();
                Object fn = tableGetLib.execute(getFn, wasmTable, tableIndex);
                return toJSValueNode.execute(fn);
            } catch (InteropException ex) {
                throw Errors.shouldNotReachHere(ex);
            } catch (AbstractTruffleException ex) {
                errorBranch.enter(this);
                throw Errors.createRangeError(ex, this);
            }
        }

        @NeverDefault
        protected static AddressValueToU64Node createToIndexNode() {
            return AddressValueToU64Node.create("WebAssembly.Table.get(): Argument 0");
        }
    }

    @ImportStatic(JSConfig.class)
    public abstract static class WebAssemblyTableSetNode extends JSBuiltinNode {

        public WebAssemblyTableSetNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object set(Object thiz, Object index, Object[] args,
                        @Cached("createToIndexNode()") AddressValueToU64Node toIndexNode,
                        @Cached ToWebAssemblyValueNode toWebAssemblyValueNode,
                        @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary tableSetLib,
                        @Cached InlinedBranchProfile errorBranch) {
            if (!JSWebAssemblyTable.isJSWebAssemblyTable(thiz)) {
                errorBranch.enter(this);
                throw Errors.createTypeError("WebAssembly.Table.set(): Receiver is not a WebAssembly.Table");
            }
            JSWebAssemblyTableObject table = (JSWebAssemblyTableObject) thiz;
            long tableIndex = toIndexNode.execute(index, table.hasIndexType64());
            Object wasmTable = table.getWASMTable();
            WebAssemblyType elementKind = table.getElementKind();
            final JSRealm realm = getRealm();

            final Object wasmValue;
            if (args.length == 0) {
                wasmValue = elementKind.getDefaultValue(realm);
            } else {
                wasmValue = toWebAssemblyValueNode.execute(args[0], elementKind);
            }
            try {
                Object setFn = realm.getWASMTableWrite();
                tableSetLib.execute(setFn, wasmTable, tableIndex, wasmValue);
            } catch (InteropException ex) {
                throw Errors.shouldNotReachHere(ex);
            } catch (AbstractTruffleException ex) {
                errorBranch.enter(this);
                throw Errors.createRangeError(ex, this);
            }
            return Undefined.instance;
        }

        @NeverDefault
        protected static AddressValueToU64Node createToIndexNode() {
            return AddressValueToU64Node.create("WebAssembly.Table.set(): Argument 0");
        }

    }

    @ImportStatic(JSConfig.class)
    public abstract static class WebAssemblyTableGetLengthNode extends JSBuiltinNode {
        public WebAssemblyTableGetLengthNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object getLength(JSWebAssemblyTableObject tableObj,
                        @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary tableLengthLib) {
            Object wasmTable = tableObj.getWASMTable();
            try {
                Object lengthFn = getRealm().getWASMTableLength();
                Object length = tableLengthLib.execute(lengthFn, wasmTable);
                return tableObj.hasIndexType64() ? BigInt.valueOfUnsigned(JSRuntime.longValue((Number) length)) : length;
            } catch (InteropException ex) {
                throw Errors.shouldNotReachHere(ex);
            }
        }

        @TruffleBoundary
        @Fallback
        protected Object doIncompatibleReceiver(@SuppressWarnings("unused") Object thisObj) {
            throw Errors.createTypeError("WebAssembly.Table.length(): Receiver is not a WebAssembly.Table", this);
        }
    }
}
