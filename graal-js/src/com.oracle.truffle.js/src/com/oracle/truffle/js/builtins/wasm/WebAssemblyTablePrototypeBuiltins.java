/*
 * Copyright (c) 2020, 2022, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.exception.AbstractTruffleException;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.builtins.JSBuiltinsContainer;
import com.oracle.truffle.js.builtins.wasm.WebAssemblyTablePrototypeBuiltinsFactory.WebAssemblyTableGetNodeGen;
import com.oracle.truffle.js.builtins.wasm.WebAssemblyTablePrototypeBuiltinsFactory.WebAssemblyTableGrowNodeGen;
import com.oracle.truffle.js.builtins.wasm.WebAssemblyTablePrototypeBuiltinsFactory.WebAssemblyTableSetNodeGen;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.nodes.wasm.ToJSValueNode;
import com.oracle.truffle.js.nodes.wasm.ToWebAssemblyIndexOrSizeNode;
import com.oracle.truffle.js.nodes.wasm.ToWebAssemblyValueNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.wasm.JSWebAssemblyTable;
import com.oracle.truffle.js.runtime.builtins.wasm.JSWebAssemblyTableObject;
import com.oracle.truffle.js.runtime.builtins.wasm.JSWebAssemblyValueTypes;
import com.oracle.truffle.js.runtime.objects.Undefined;

public class WebAssemblyTablePrototypeBuiltins extends JSBuiltinsContainer.SwitchEnum<WebAssemblyTablePrototypeBuiltins.WebAssemblyTablePrototype> {

    public static final JSBuiltinsContainer BUILTINS = new WebAssemblyTablePrototypeBuiltins();

    protected WebAssemblyTablePrototypeBuiltins() {
        super(JSWebAssemblyTable.PROTOTYPE_NAME, WebAssemblyTablePrototype.class);
    }

    public enum WebAssemblyTablePrototype implements BuiltinEnum<WebAssemblyTablePrototype> {
        grow(1),
        get(1),
        set(1);

        private final int length;

        WebAssemblyTablePrototype(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }

        @Override
        public boolean isEnumerable() {
            return true;
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
        }
        return null;
    }

    public abstract static class WebAssemblyTableGrowNode extends JSBuiltinNode {
        @Child ToWebAssemblyIndexOrSizeNode toDeltaNode;
        @Child ToWebAssemblyValueNode toWebAssemblyValueNode;
        private final BranchProfile errorBranch = BranchProfile.create();
        @Child InteropLibrary tableGrowLib = InteropLibrary.getFactory().createDispatched(JSConfig.InteropLibraryLimit);
        @Child InteropLibrary tableTypeLib = InteropLibrary.getFactory().createDispatched(JSConfig.InteropLibraryLimit);

        public WebAssemblyTableGrowNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            toDeltaNode = ToWebAssemblyIndexOrSizeNode.create("WebAssembly.Table.grow(): Argument 0");
            toWebAssemblyValueNode = ToWebAssemblyValueNode.create();
        }

        @Specialization
        protected Object grow(Object thiz, Object delta, Object[] args) {
            if (!JSWebAssemblyTable.isJSWebAssemblyTable(thiz)) {
                errorBranch.enter();
                throw Errors.createTypeError("WebAssembly.Table.grow(): Receiver is not a WebAssembly.Table");
            }
            int deltaInt = toDeltaNode.executeInt(delta);
            Object wasmTable = ((JSWebAssemblyTableObject) thiz).getWASMTable();
            final TruffleString tableType;
            try {
                Object tableTypeFn = getRealm().getWASMTableType();
                tableType = asTString(tableTypeLib.execute(tableTypeFn, wasmTable));
            } catch (InteropException ex) {
                throw Errors.shouldNotReachHere(ex);
            }
            final Object wasmValue;
            if (args.length == 0) {
                wasmValue = JSWebAssemblyValueTypes.getDefaultValue(tableType);
            } else {
                wasmValue = toWebAssemblyValueNode.execute(args[0], tableType);
            }
            try {
                Object growFn = getRealm().getWASMTableGrow();
                return tableGrowLib.execute(growFn, wasmTable, deltaInt, wasmValue);
            } catch (InteropException ex) {
                throw Errors.shouldNotReachHere(ex);
            } catch (AbstractTruffleException ex) {
                errorBranch.enter();
                throw Errors.createRangeError(ex, this);
            }
        }

    }

    public abstract static class WebAssemblyTableGetNode extends JSBuiltinNode {
        @Child ToWebAssemblyIndexOrSizeNode toIndexNode;
        @Child ToJSValueNode toJSValueNode;
        private final BranchProfile errorBranch = BranchProfile.create();
        @Child InteropLibrary tableGetLib = InteropLibrary.getFactory().createDispatched(JSConfig.InteropLibraryLimit);

        public WebAssemblyTableGetNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            toIndexNode = ToWebAssemblyIndexOrSizeNode.create("WebAssembly.Table.get(): Argument 0");
            toJSValueNode = ToJSValueNode.create();
        }

        @Specialization
        protected Object get(Object thiz, Object index) {
            if (!JSWebAssemblyTable.isJSWebAssemblyTable(thiz)) {
                errorBranch.enter();
                throw Errors.createTypeError("WebAssembly.Table.get(): Receiver is not a WebAssembly.Table");
            }
            JSRealm realm = getRealm();
            int indexInt = toIndexNode.executeInt(index);
            Object wasmTable = ((JSWebAssemblyTableObject) thiz).getWASMTable();
            try {
                Object getFn = realm.getWASMTableRead();
                Object fn = tableGetLib.execute(getFn, wasmTable, indexInt);
                return toJSValueNode.execute(fn);
            } catch (InteropException ex) {
                throw Errors.shouldNotReachHere(ex);
            } catch (AbstractTruffleException ex) {
                errorBranch.enter();
                throw Errors.createRangeError(ex, this);
            }
        }
    }

    public abstract static class WebAssemblyTableSetNode extends JSBuiltinNode {
        @Child ToWebAssemblyIndexOrSizeNode toIndexNode;
        @Child ToWebAssemblyValueNode toWebAssemblyValueNode;
        private final BranchProfile errorBranch = BranchProfile.create();
        @Child InteropLibrary tableSetLib = InteropLibrary.getFactory().createDispatched(JSConfig.InteropLibraryLimit);
        @Child InteropLibrary tableTypeLib = InteropLibrary.getFactory().createDispatched(JSConfig.InteropLibraryLimit);

        public WebAssemblyTableSetNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            toIndexNode = ToWebAssemblyIndexOrSizeNode.create("WebAssembly.Table.set(): Argument 0");
            toWebAssemblyValueNode = ToWebAssemblyValueNode.create();
        }

        @Specialization
        protected Object set(Object thiz, Object index, Object[] args) {
            if (!JSWebAssemblyTable.isJSWebAssemblyTable(thiz)) {
                errorBranch.enter();
                throw Errors.createTypeError("WebAssembly.Table.set(): Receiver is not a WebAssembly.Table");
            }
            int indexInt = toIndexNode.executeInt(index);
            Object wasmTable = ((JSWebAssemblyTableObject) thiz).getWASMTable();
            final TruffleString tableType;
            try {
                Object tableTypeFn = getRealm().getWASMTableType();
                tableType = asTString(tableTypeLib.execute(tableTypeFn, wasmTable));
            } catch (InteropException ex) {
                throw Errors.shouldNotReachHere(ex);
            }
            final Object wasmValue;
            if (args.length == 0) {
                wasmValue = JSWebAssemblyValueTypes.getDefaultValue(tableType);
            } else {
                wasmValue = toWebAssemblyValueNode.execute(args[0], tableType);
            }
            try {
                Object setFn = getRealm().getWASMTableWrite();
                tableSetLib.execute(setFn, wasmTable, indexInt, wasmValue);
            } catch (InteropException ex) {
                throw Errors.shouldNotReachHere(ex);
            } catch (AbstractTruffleException ex) {
                errorBranch.enter();
                throw Errors.createRangeError(ex, this);
            }
            return Undefined.instance;
        }

    }

    private static TruffleString asTString(Object string) throws UnsupportedMessageException {
        if (string instanceof String) {
            return Strings.fromJavaString((String) string);
        }
        return InteropLibrary.getUncached(string).asTruffleString(string);
    }
}
