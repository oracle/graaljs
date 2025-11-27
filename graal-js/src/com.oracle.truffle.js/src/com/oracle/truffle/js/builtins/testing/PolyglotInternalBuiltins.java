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
package com.oracle.truffle.js.builtins.testing;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.HostCompilerDirectives.InliningCutoff;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Exclusive;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.InvalidArrayIndexException;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.builtins.JSBuiltinsContainer;
import com.oracle.truffle.js.builtins.testing.PolyglotInternalBuiltinsFactory.PolyglotConstructNodeGen;
import com.oracle.truffle.js.builtins.testing.PolyglotInternalBuiltinsFactory.PolyglotExecuteNodeGen;
import com.oracle.truffle.js.builtins.testing.PolyglotInternalBuiltinsFactory.PolyglotGetSizeNodeGen;
import com.oracle.truffle.js.builtins.testing.PolyglotInternalBuiltinsFactory.PolyglotHasKeysNodeGen;
import com.oracle.truffle.js.builtins.testing.PolyglotInternalBuiltinsFactory.PolyglotHasSizeNodeGen;
import com.oracle.truffle.js.builtins.testing.PolyglotInternalBuiltinsFactory.PolyglotIsBoxedPrimitiveNodeGen;
import com.oracle.truffle.js.builtins.testing.PolyglotInternalBuiltinsFactory.PolyglotIsExecutableNodeGen;
import com.oracle.truffle.js.builtins.testing.PolyglotInternalBuiltinsFactory.PolyglotIsInstantiableNodeGen;
import com.oracle.truffle.js.builtins.testing.PolyglotInternalBuiltinsFactory.PolyglotIsNullNodeGen;
import com.oracle.truffle.js.builtins.testing.PolyglotInternalBuiltinsFactory.PolyglotKeysNodeGen;
import com.oracle.truffle.js.builtins.testing.PolyglotInternalBuiltinsFactory.PolyglotReadNodeGen;
import com.oracle.truffle.js.builtins.testing.PolyglotInternalBuiltinsFactory.PolyglotRemoveNodeGen;
import com.oracle.truffle.js.builtins.testing.PolyglotInternalBuiltinsFactory.PolyglotToJSValueNodeGen;
import com.oracle.truffle.js.builtins.testing.PolyglotInternalBuiltinsFactory.PolyglotToPolyglotValueNodeGen;
import com.oracle.truffle.js.builtins.testing.PolyglotInternalBuiltinsFactory.PolyglotUnboxValueNodeGen;
import com.oracle.truffle.js.builtins.testing.PolyglotInternalBuiltinsFactory.PolyglotWriteNodeGen;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.nodes.interop.ExportValueNode;
import com.oracle.truffle.js.nodes.interop.ImportValueNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.interop.JSInteropUtil;
import com.oracle.truffle.js.runtime.objects.Null;

/**
 * Extra builtins injected into {@code Polyglot} object for Interop testing purposes.
 */
public final class PolyglotInternalBuiltins extends JSBuiltinsContainer.SwitchEnum<PolyglotInternalBuiltins.PolyglotInternal> {

    public static final JSBuiltinsContainer BUILTINS = new PolyglotInternalBuiltins();

    protected PolyglotInternalBuiltins() {
        super(JSRealm.POLYGLOT_CLASS_NAME, PolyglotInternalBuiltins.PolyglotInternal.class);
    }

    public enum PolyglotInternal implements BuiltinEnum<PolyglotInternalBuiltins.PolyglotInternal> {
        isExecutable(1),
        isBoxed(1),
        isNull(1),
        hasSize(1),
        read(2),
        write(3),
        unbox(1),
        construct(1),
        execute(1),
        getSize(1),
        remove(2),
        toJSValue(1),
        toPolyglotValue(1),
        keys(1),
        hasKeys(1),
        isInstantiable(1);

        private final int length;

        PolyglotInternal(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, PolyglotInternalBuiltins.PolyglotInternal builtinEnum) {
        switch (builtinEnum) {
            case isExecutable:
                return PolyglotIsExecutableNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            case isBoxed:
                return PolyglotIsBoxedPrimitiveNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            case isNull:
                return PolyglotIsNullNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            case isInstantiable:
                return PolyglotIsInstantiableNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            case hasSize:
                return PolyglotHasSizeNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            case read:
                return PolyglotReadNodeGen.create(context, builtin, args().fixedArgs(2).createArgumentNodes(context));
            case write:
                return PolyglotWriteNodeGen.create(context, builtin, args().fixedArgs(3).createArgumentNodes(context));
            case remove:
                return PolyglotRemoveNodeGen.create(context, builtin, args().fixedArgs(2).createArgumentNodes(context));
            case unbox:
                return PolyglotUnboxValueNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            case construct:
                return PolyglotConstructNodeGen.create(context, builtin, args().fixedArgs(1).varArgs().createArgumentNodes(context));
            case execute:
                return PolyglotExecuteNodeGen.create(context, builtin, args().fixedArgs(1).varArgs().createArgumentNodes(context));
            case getSize:
                return PolyglotGetSizeNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            case keys:
                return PolyglotKeysNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            case toJSValue:
                return PolyglotToJSValueNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            case toPolyglotValue:
                return PolyglotToPolyglotValueNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            case hasKeys:
                return PolyglotHasKeysNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
        }
        return null;
    }

    @ImportStatic({JSConfig.class})
    abstract static class PolyglotIsExecutableNode extends JSBuiltinNode {

        PolyglotIsExecutableNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected static boolean truffleObject(TruffleObject obj,
                        @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary interop) {
            return interop.isExecutable(obj);
        }

        @Specialization(guards = "isJavaPrimitive(obj)")
        protected static boolean primitive(@SuppressWarnings("unused") Object obj) {
            return false;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"!isTruffleObject(obj)", "!isJavaPrimitive(obj)"})
        protected static boolean unsupported(Object obj) {
            return false;
        }
    }

    @ImportStatic({JSConfig.class})
    abstract static class PolyglotIsBoxedPrimitiveNode extends JSBuiltinNode {

        PolyglotIsBoxedPrimitiveNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(limit = "InteropLibraryLimit")
        protected static boolean truffleObject(TruffleObject obj,
                        @CachedLibrary("obj") InteropLibrary interop) {
            return JSInteropUtil.isBoxedPrimitive(obj, interop);
        }

        @Specialization(guards = "isJavaPrimitive(obj)")
        protected static boolean primitive(@SuppressWarnings("unused") Object obj) {
            return false;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"!isTruffleObject(obj)", "!isJavaPrimitive(obj)"})
        protected static boolean unsupported(Object obj) {
            return false;
        }
    }

    @ImportStatic({JSConfig.class})
    abstract static class PolyglotIsNullNode extends JSBuiltinNode {

        PolyglotIsNullNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected static boolean truffleObject(TruffleObject obj,
                        @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary interop) {
            return interop.isNull(obj);
        }

        @Specialization(guards = "isJavaPrimitive(obj)")
        protected static boolean primitive(@SuppressWarnings("unused") Object obj) {
            return false;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"!isTruffleObject(obj)", "!isJavaPrimitive(obj)"})
        protected static boolean unsupported(Object obj) {
            return false;
        }
    }

    @ImportStatic({JSConfig.class})
    abstract static class PolyglotHasSizeNode extends JSBuiltinNode {

        PolyglotHasSizeNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected boolean truffleObject(TruffleObject obj,
                        @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary interop) {
            return interop.hasArrayElements(obj);
        }

        @Specialization(guards = "isJavaPrimitive(obj)")
        protected boolean primitive(@SuppressWarnings("unused") Object obj) {
            return false;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"!isTruffleObject(obj)", "!isJavaPrimitive(obj)"})
        protected boolean unsupported(Object obj) {
            return false;
        }
    }

    @ImportStatic({JSConfig.class})
    abstract static class PolyglotReadNode extends JSBuiltinNode {

        PolyglotReadNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object member(TruffleObject obj, TruffleString name,
                        @Shared @Cached ImportValueNode foreignConvert,
                        @Shared @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary interop,
                        @Shared @Cached TruffleString.ToJavaStringNode toJavaString) {
            return JSInteropUtil.readMemberOrDefault(obj, name, Null.instance, interop, foreignConvert, toJavaString);
        }

        @Specialization
        protected Object arrayElementInt(TruffleObject obj, int index,
                        @Shared @Cached ImportValueNode foreignConvert,
                        @Shared @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary interop) {
            return JSInteropUtil.readArrayElementOrDefault(obj, index, Null.instance, interop, foreignConvert);
        }

        @Specialization(guards = "isNumber(index)", replaces = "arrayElementInt")
        protected Object arrayElement(TruffleObject obj, Number index,
                        @Shared @Cached ImportValueNode foreignConvert,
                        @Shared @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary interop) {
            return JSInteropUtil.readArrayElementOrDefault(obj, JSRuntime.longValue(index), Null.instance, interop, foreignConvert);
        }

        @SuppressWarnings("unused")
        @InliningCutoff
        @Specialization(guards = {"!isString(key)", "!isNumber(key)"})
        protected Object unsupportedKey(TruffleObject obj, Object key,
                        @Shared @Cached ImportValueNode foreignConvert,
                        @Shared @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary interop,
                        @Exclusive @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary keyInterop,
                        @Cached TruffleString.SwitchEncodingNode switchEncoding,
                        @Shared @Cached TruffleString.ToJavaStringNode toJavaString) {
            try {
                if (keyInterop.isString(key)) {
                    return member(obj, Strings.interopAsTruffleString(key, keyInterop, switchEncoding), foreignConvert, interop, toJavaString);
                } else if (keyInterop.fitsInInt(key)) {
                    return arrayElement(obj, keyInterop.asInt(key), foreignConvert, interop);
                }
            } catch (UnsupportedMessageException e) {
                throw Errors.createTypeErrorUnboxException(obj, e, this);
            }
            return Null.instance;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isTruffleObject(obj)")
        protected boolean unsupported(Object obj, Object name) {
            throw Errors.createTypeErrorNotATruffleObject("read");
        }
    }

    @ImportStatic({JSConfig.class})
    abstract static class PolyglotWriteNode extends JSBuiltinNode {

        PolyglotWriteNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object member(TruffleObject obj, TruffleString name, Object value,
                        @Shared @Cached ExportValueNode exportValue,
                        @Shared @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary interop,
                        @Shared @Cached TruffleString.ToJavaStringNode toJavaStringNode) {
            Object convertedValue = exportValue.execute(value);
            try {
                interop.writeMember(obj, Strings.toJavaString(toJavaStringNode, name), convertedValue);
                return convertedValue;
            } catch (UnknownIdentifierException e) {
                return Null.instance;
            } catch (UnsupportedMessageException | UnsupportedTypeException e) {
                throw Errors.createTypeErrorInteropException(obj, e, "writeMember", name, this);
            }
        }

        @Specialization
        protected Object arrayElementInt(TruffleObject obj, int index, Object value,
                        @Shared @Cached ExportValueNode exportValue,
                        @Shared @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary interop) {
            Object convertedValue = exportValue.execute(value);
            try {
                interop.writeArrayElement(obj, index, convertedValue);
                return convertedValue;
            } catch (InvalidArrayIndexException e) {
                return Null.instance;
            } catch (UnsupportedMessageException | UnsupportedTypeException e) {
                throw Errors.createTypeErrorInteropException(obj, e, "writeArrayElement", index, this);
            }
        }

        @Specialization(guards = "isNumber(index)", replaces = "arrayElementInt")
        protected Object arrayElement(TruffleObject obj, Number index, Object value,
                        @Shared @Cached ExportValueNode exportValue,
                        @Shared @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary interop) {
            Object convertedValue = exportValue.execute(value);
            try {
                interop.writeArrayElement(obj, JSRuntime.longValue(index), convertedValue);
                return convertedValue;
            } catch (InvalidArrayIndexException e) {
                return Null.instance;
            } catch (UnsupportedMessageException | UnsupportedTypeException e) {
                throw Errors.createTypeErrorInteropException(obj, e, "writeArrayElement", index, this);
            }
        }

        @SuppressWarnings("unused")
        @InliningCutoff
        @Specialization(guards = {"!isString(key)", "!isNumber(key)"})
        protected Object unsupportedKey(TruffleObject obj, Object key, Object value,
                        @Shared @Cached ExportValueNode exportValue,
                        @Shared @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary interop,
                        @Exclusive @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary keyInterop,
                        @Shared @Cached TruffleString.ToJavaStringNode toJavaStringNode,
                        @Cached TruffleString.SwitchEncodingNode switchEncoding) {
            try {
                if (keyInterop.isString(key)) {
                    return member(obj, Strings.interopAsTruffleString(key, keyInterop, switchEncoding), value, exportValue, interop, toJavaStringNode);
                } else if (keyInterop.fitsInInt(key)) {
                    return arrayElement(obj, keyInterop.asInt(key), value, exportValue, interop);
                }
            } catch (UnsupportedMessageException e) {
                throw Errors.createTypeErrorUnboxException(obj, e, this);
            }
            return Null.instance;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isTruffleObject(obj)")
        protected boolean unsupported(Object obj, Object name, Object value) {
            throw Errors.createTypeErrorNotATruffleObject("write");
        }
    }

    @ImportStatic({JSConfig.class})
    abstract static class PolyglotRemoveNode extends JSBuiltinNode {

        PolyglotRemoveNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected boolean member(TruffleObject obj, TruffleString name,
                        @Shared @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary interop,
                        @Shared @Cached TruffleString.ToJavaStringNode toJavaStringNode) {
            try {
                interop.removeMember(obj, Strings.toJavaString(toJavaStringNode, name));
                return true;
            } catch (UnknownIdentifierException e) {
                return false;
            } catch (UnsupportedMessageException e) {
                throw Errors.createTypeErrorInteropException(obj, e, "removeMember", name, this);
            }
        }

        @Specialization
        protected boolean arrayElementInt(TruffleObject obj, int index,
                        @Shared @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary interop) {
            try {
                interop.removeArrayElement(obj, index);
                return true;
            } catch (InvalidArrayIndexException e) {
                return false;
            } catch (UnsupportedMessageException e) {
                throw Errors.createTypeErrorInteropException(obj, e, "removeArrayElement", index, this);
            }
        }

        @Specialization(guards = "isNumber(index)", replaces = "arrayElementInt")
        protected boolean arrayElement(TruffleObject obj, Number index,
                        @Shared @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary interop) {
            try {
                interop.removeArrayElement(obj, JSRuntime.longValue(index));
                return true;
            } catch (InvalidArrayIndexException e) {
                return false;
            } catch (UnsupportedMessageException e) {
                throw Errors.createTypeErrorInteropException(obj, e, "removeArrayElement", index, this);
            }
        }

        @SuppressWarnings("unused")
        @InliningCutoff
        @Specialization(guards = {"!isString(key)", "!isNumber(key)"})
        protected Object unsupportedKey(TruffleObject obj, Object key,
                        @Shared @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary interop,
                        @Exclusive @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary keyInterop,
                        @Shared @Cached TruffleString.ToJavaStringNode toJavaStringNode,
                        @Cached TruffleString.SwitchEncodingNode switchEncoding) {
            try {
                if (keyInterop.isString(key)) {
                    return member(obj, Strings.interopAsTruffleString(key, keyInterop, switchEncoding), interop, toJavaStringNode);
                } else if (keyInterop.fitsInInt(key)) {
                    return arrayElementInt(obj, keyInterop.asInt(key), interop);
                }
            } catch (UnsupportedMessageException e) {
                throw Errors.createTypeErrorUnboxException(obj, e, this);
            }
            return Null.instance;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isTruffleObject(obj)")
        protected boolean unsupported(Object obj, Object key) {
            throw Errors.createTypeErrorNotATruffleObject("remove");
        }
    }

    @ImportStatic({JSConfig.class})
    abstract static class PolyglotUnboxValueNode extends JSBuiltinNode {

        PolyglotUnboxValueNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object truffleObject(TruffleObject obj,
                        @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary interop) {
            Object unboxed = JSInteropUtil.toPrimitiveOrDefaultLossy(obj, obj, interop, this);
            if (unboxed == obj) {
                throw Errors.createTypeErrorNotATruffleObject("unbox");
            }
            return unboxed;
        }

        @Specialization(guards = "isJavaPrimitive(obj)")
        protected Object primitive(Object obj) {
            // identity function
            return obj;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"!isTruffleObject(obj)", "!isJavaPrimitive(obj)"})
        protected boolean unsupported(Object obj) {
            throw Errors.createTypeErrorNotATruffleObject("unbox");
        }
    }

    @ImportStatic({JSConfig.class})
    abstract static class PolyglotExecuteNode extends JSBuiltinNode {

        PolyglotExecuteNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object doExecute(TruffleObject obj, Object[] arguments,
                        @Cached ExportValueNode exportValue,
                        @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary interop) {
            Object target = exportValue.execute(obj);
            Object[] convertedArgs = new Object[arguments.length];
            for (int i = 0; i < arguments.length; i++) {
                convertedArgs[i] = exportValue.execute(arguments[i]);
            }
            try {
                return interop.execute(target, convertedArgs);
            } catch (UnsupportedTypeException | ArityException | UnsupportedMessageException e) {
                throw Errors.createTypeErrorInteropException(obj, e, "execute", this);
            }
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isTruffleObject(obj)")
        protected boolean unsupported(Object obj, Object[] arguments) {
            throw Errors.createTypeErrorNotATruffleObject("execute");
        }
    }

    @ImportStatic({JSConfig.class})
    abstract static class PolyglotConstructNode extends JSBuiltinNode {

        PolyglotConstructNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object doNew(TruffleObject obj, Object[] arguments,
                        @Cached ExportValueNode exportValue,
                        @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary interop) {
            Object target = exportValue.execute(obj);
            Object[] convertedArgs = new Object[arguments.length];
            for (int i = 0; i < arguments.length; i++) {
                convertedArgs[i] = exportValue.execute(arguments[i]);
            }
            try {
                return interop.instantiate(target, convertedArgs);
            } catch (UnsupportedTypeException | ArityException | UnsupportedMessageException e) {
                throw Errors.createTypeErrorInteropException(obj, e, "instantiate", this);
            }
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isTruffleObject(obj)")
        protected boolean unsupported(Object obj, Object[] arguments) {
            throw Errors.createTypeErrorNotATruffleObject("construct");
        }
    }

    @ImportStatic({JSConfig.class})
    abstract static class PolyglotGetSizeNode extends JSBuiltinNode {

        PolyglotGetSizeNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object getSize(TruffleObject obj,
                        @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary interop) {
            try {
                return interop.getArraySize(obj);
            } catch (UnsupportedMessageException e) {
                return Null.instance;
            }
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isTruffleObject(obj)")
        protected boolean unsupported(Object obj) {
            throw Errors.createTypeErrorNotATruffleObject("getSize");
        }
    }

    @ImportStatic({JSConfig.class})
    abstract static class PolyglotHasKeysNode extends JSBuiltinNode {

        PolyglotHasKeysNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected boolean hasKeys(TruffleObject obj,
                        @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary interop) {
            return interop.hasMembers(obj);
        }

        @Specialization(guards = "!isTruffleObject(obj)")
        protected boolean unsupported(@SuppressWarnings("unused") Object obj) {
            return false;
        }
    }

    abstract static class PolyglotKeysNode extends JSBuiltinNode {

        PolyglotKeysNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @TruffleBoundary
        @Specialization
        protected Object keys(TruffleObject obj) {
            return JSArray.createConstantObjectArray(getContext(), getRealm(), JSInteropUtil.keys(obj).toArray());
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isTruffleObject(obj)")
        protected boolean unsupported(Object obj) {
            throw Errors.createTypeErrorNotATruffleObject("keys");
        }
    }

    @ImportStatic({JSConfig.class})
    abstract static class PolyglotIsInstantiableNode extends JSBuiltinNode {

        PolyglotIsInstantiableNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected static boolean isInstantiable(TruffleObject obj,
                        @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary interop) {
            return interop.isInstantiable(obj);
        }

        @Specialization(guards = "!isTruffleObject(obj)")
        protected static boolean unsupported(@SuppressWarnings("unused") Object obj) {
            return false;
        }
    }

    /**
     * Forces the conversion of an (potential) interop value to a JavaScript compliant value. In
     * addition to the conversions forced at the language boundary anyway (e.g., Java primitive
     * types like short or float that are not supported by JavaScript), this operation also converts
     * Nullish interop values to the JavaScript null value, and unboxes boxed TruffleObjects.
     *
     */
    @ImportStatic({JSConfig.class})
    abstract static class PolyglotToJSValueNode extends JSBuiltinNode {
        PolyglotToJSValueNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected final Object toJSValue(TruffleObject obj,
                        @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary interop) {
            return JSInteropUtil.toPrimitiveOrDefaultLossy(obj, obj, interop, this);
        }

        @Specialization(guards = "!isTruffleObject(obj)")
        protected static Object toJSValue(Object obj) {
            return obj;
        }
    }

    /**
     * Forces the conversion of a JavaScript value to a value compliant with Interop semantics. This
     * is done automatically at the language boundary and should rarely be necessary to be triggered
     * by user code.
     */
    abstract static class PolyglotToPolyglotValueNode extends JSBuiltinNode {
        @Child private ExportValueNode exportValueNode;

        PolyglotToPolyglotValueNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            this.exportValueNode = ExportValueNode.create();
        }

        @Specialization
        protected Object toPolyglotValue(Object value) {
            return exportValueNode.execute(value);
        }
    }
}
