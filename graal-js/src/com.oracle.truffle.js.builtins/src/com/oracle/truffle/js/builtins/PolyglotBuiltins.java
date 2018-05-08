/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.AccessDeniedException;
import java.nio.file.NoSuchFileException;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.js.builtins.PolyglotBuiltinsFactory.PolyglotConstructNodeGen;
import com.oracle.truffle.js.builtins.PolyglotBuiltinsFactory.PolyglotCreateForeignDynamicObjectNodeGen;
import com.oracle.truffle.js.builtins.PolyglotBuiltinsFactory.PolyglotCreateForeignObjectNodeGen;
import com.oracle.truffle.js.builtins.PolyglotBuiltinsFactory.PolyglotEvalFileNodeGen;
import com.oracle.truffle.js.builtins.PolyglotBuiltinsFactory.PolyglotEvalNodeGen;
import com.oracle.truffle.js.builtins.PolyglotBuiltinsFactory.PolyglotExecuteNodeGen;
import com.oracle.truffle.js.builtins.PolyglotBuiltinsFactory.PolyglotExportNodeGen;
import com.oracle.truffle.js.builtins.PolyglotBuiltinsFactory.PolyglotGetSizeNodeGen;
import com.oracle.truffle.js.builtins.PolyglotBuiltinsFactory.PolyglotHasKeysNodeGen;
import com.oracle.truffle.js.builtins.PolyglotBuiltinsFactory.PolyglotHasSizeNodeGen;
import com.oracle.truffle.js.builtins.PolyglotBuiltinsFactory.PolyglotImportNodeGen;
import com.oracle.truffle.js.builtins.PolyglotBuiltinsFactory.PolyglotIsBoxedPrimitiveNodeGen;
import com.oracle.truffle.js.builtins.PolyglotBuiltinsFactory.PolyglotIsExecutableNodeGen;
import com.oracle.truffle.js.builtins.PolyglotBuiltinsFactory.PolyglotIsInstantiableNodeGen;
import com.oracle.truffle.js.builtins.PolyglotBuiltinsFactory.PolyglotIsNullNodeGen;
import com.oracle.truffle.js.builtins.PolyglotBuiltinsFactory.PolyglotKeysNodeGen;
import com.oracle.truffle.js.builtins.PolyglotBuiltinsFactory.PolyglotReadNodeGen;
import com.oracle.truffle.js.builtins.PolyglotBuiltinsFactory.PolyglotRemoveNodeGen;
import com.oracle.truffle.js.builtins.PolyglotBuiltinsFactory.PolyglotToJSValueNodeGen;
import com.oracle.truffle.js.builtins.PolyglotBuiltinsFactory.PolyglotToPolyglotValueNodeGen;
import com.oracle.truffle.js.builtins.PolyglotBuiltinsFactory.PolyglotUnboxValueNodeGen;
import com.oracle.truffle.js.builtins.PolyglotBuiltinsFactory.PolyglotWriteNodeGen;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.nodes.interop.ExportValueNode;
import com.oracle.truffle.js.nodes.interop.JSForeignToJSTypeNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.Evaluator;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSTruffleOptions;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.truffleinterop.JSInteropUtil;

public final class PolyglotBuiltins extends JSBuiltinsContainer.SwitchEnum<PolyglotBuiltins.Polyglot> {
    protected PolyglotBuiltins() {
        super(JSRealm.POLYGLOT_CLASS_NAME, Polyglot.class);
    }

    public enum Polyglot implements BuiltinEnum<Polyglot> {
        // external
        export(2),
        import_(1),
        eval(2),
        evalFile(2);

        private final int length;

        Polyglot(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, Polyglot builtinEnum) {
        switch (builtinEnum) {
            case export:
                return PolyglotExportNodeGen.create(context, builtin, args().fixedArgs(2).createArgumentNodes(context));
            case import_:
                return PolyglotImportNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            case eval:
                return PolyglotEvalNodeGen.create(context, builtin, args().fixedArgs(2).createArgumentNodes(context));
            case evalFile:
                return PolyglotEvalFileNodeGen.create(context, builtin, args().fixedArgs(2).createArgumentNodes(context));
        }
        return null;
    }

    public static final class PolyglotInternalBuiltins extends JSBuiltinsContainer.SwitchEnum<PolyglotInternalBuiltins.PolyglotInternal> {
        protected PolyglotInternalBuiltins() {
            super(JSRealm.POLYGLOT_INTERNAL_CLASS_NAME, PolyglotInternal.class);
        }

        public enum PolyglotInternal implements BuiltinEnum<PolyglotInternal> {
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
            isInstantiable(1),

            createForeignObject(0) {
                @Override
                public boolean isAOTSupported() {
                    return false;
                }
            },
            createForeignDynamicObject(0) {
                @Override
                public boolean isAOTSupported() {
                    return false;
                }
            };

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
        protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, PolyglotInternal builtinEnum) {
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
                case createForeignObject:
                    if (!JSTruffleOptions.SubstrateVM) {
                        return PolyglotCreateForeignObjectNodeGen.create(context, builtin, args().fixedArgs(0).createArgumentNodes(context));
                    }
                    break;
                case createForeignDynamicObject:
                    if (!JSTruffleOptions.SubstrateVM) {
                        return PolyglotCreateForeignDynamicObjectNodeGen.create(context, builtin, args().fixedArgs(0).createArgumentNodes(context));
                    }
                    break;
            }
            return null;
        }
    }

    @ImportStatic({JSInteropUtil.class})
    abstract static class PolyglotExportNode extends JSBuiltinNode {
        @Child private ExportValueNode export;
        @Child private Node writeBinding = Message.WRITE.createNode();

        PolyglotExportNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            export = ExportValueNode.create(context);
        }

        @Specialization
        protected Object doString(String identifier, Object value) {
            TruffleObject polyglotBindings = (TruffleObject) getContext().getRealm().getEnv().getPolyglotBindings();
            Object exportedValue = export.executeWithTarget(value, Undefined.instance);
            try {
                ForeignAccess.sendWrite(writeBinding, polyglotBindings, identifier, exportedValue);
            } catch (UnknownIdentifierException | UnsupportedTypeException | UnsupportedMessageException e) {
                throw Errors.createTypeErrorInteropException(polyglotBindings, e, Message.WRITE, this);
            }
            return exportedValue;
        }

        @Specialization(guards = {"!isString(identifier)"})
        protected Object doMaybeUnbox(TruffleObject identifier, Object value,
                        @Cached("createIsBoxed()") Node isBoxedNode,
                        @Cached("createUnbox()") Node unboxNode) {
            if (ForeignAccess.sendIsBoxed(isBoxedNode, identifier)) {
                Object unboxed;
                try {
                    unboxed = ForeignAccess.sendUnbox(unboxNode, identifier);
                } catch (UnsupportedMessageException e) {
                    throw Errors.createTypeErrorInteropException(identifier, e, Message.UNBOX, this);
                }
                if (unboxed instanceof String) {
                    return doString((String) unboxed, value);
                }
            }
            return doInvalid(identifier, value);
        }

        @Specialization(guards = "!isString(identifier)")
        @TruffleBoundary
        protected Object doInvalid(Object identifier, @SuppressWarnings("unused") Object value) {
            throw Errors.createTypeErrorInvalidIdentifier(identifier);
        }
    }

    @ImportStatic({JSInteropUtil.class})
    abstract static class PolyglotImportNode extends JSBuiltinNode {
        @Child private Node readBinding = Message.READ.createNode();

        PolyglotImportNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object doString(String identifier) {
            TruffleObject polyglotBindings = (TruffleObject) getContext().getRealm().getEnv().getPolyglotBindings();
            Object value;
            try {
                value = ForeignAccess.sendRead(readBinding, polyglotBindings, identifier);
            } catch (UnknownIdentifierException e) {
                value = Null.instance;
            } catch (UnsupportedMessageException e) {
                throw Errors.createTypeErrorInteropException(polyglotBindings, e, Message.READ, this);
            }
            return value;
        }

        @Specialization(guards = {"!isString(identifier)"})
        protected Object doMaybeUnbox(TruffleObject identifier,
                        @Cached("createIsBoxed()") Node isBoxedNode,
                        @Cached("createUnbox()") Node unboxNode) {
            if (ForeignAccess.sendIsBoxed(isBoxedNode, identifier)) {
                Object unboxed;
                try {
                    unboxed = ForeignAccess.sendUnbox(unboxNode, identifier);
                } catch (UnsupportedMessageException e) {
                    throw Errors.createTypeErrorInteropException(identifier, e, Message.UNBOX, this);
                }
                if (unboxed instanceof String) {
                    return doString((String) unboxed);
                }
            }
            return doInvalid(identifier);
        }

        @Specialization(guards = {"!isString(identifier)", "!isTruffleObject(identifier)"})
        @TruffleBoundary
        protected Object doInvalid(Object identifier) {
            throw Errors.createTypeErrorInvalidIdentifier(identifier);
        }
    }

    @ImportStatic({JSInteropUtil.class})
    abstract static class PolyglotIsExecutableNode extends JSBuiltinNode {

        PolyglotIsExecutableNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected boolean truffleObject(TruffleObject obj,
                        @Cached("createIsExecutable()") Node isExecutable) {
            return ForeignAccess.sendIsExecutable(isExecutable, obj);
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

    @ImportStatic({JSInteropUtil.class})
    abstract static class PolyglotIsBoxedPrimitiveNode extends JSBuiltinNode {

        PolyglotIsBoxedPrimitiveNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected boolean truffleObject(TruffleObject obj,
                        @Cached("createIsBoxed()") Node isBoxed) {
            return ForeignAccess.sendIsBoxed(isBoxed, obj);
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

    @ImportStatic({JSInteropUtil.class})
    abstract static class PolyglotIsNullNode extends JSBuiltinNode {

        PolyglotIsNullNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected boolean truffleObject(TruffleObject obj,
                        @Cached("createIsNull()") Node isNull) {
            return ForeignAccess.sendIsNull(isNull, obj);
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

    @ImportStatic({JSInteropUtil.class})
    abstract static class PolyglotHasSizeNode extends JSBuiltinNode {

        PolyglotHasSizeNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected boolean truffleObject(TruffleObject obj,
                        @Cached("createHasSize()") Node hasSize) {
            return ForeignAccess.sendHasSize(hasSize, obj);
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

    @ImportStatic({JSInteropUtil.class})
    abstract static class PolyglotReadNode extends JSBuiltinNode {

        PolyglotReadNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object read(TruffleObject obj, Object name,
                        @Cached("createRead()") Node read,
                        @Cached("create()") JSForeignToJSTypeNode foreignConvert) {
            try {
                return foreignConvert.executeWithTarget(ForeignAccess.sendRead(read, obj, name));
            } catch (UnknownIdentifierException e) {
                return Null.instance;
            } catch (UnsupportedMessageException e) {
                throw Errors.createTypeErrorInteropException(obj, e, Message.READ, this);
            }
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isTruffleObject(obj)")
        protected boolean unsupported(Object obj, Object name) {
            throw Errors.createTypeErrorNotATruffleObject(Message.READ);
        }
    }

    @ImportStatic({JSInteropUtil.class})
    abstract static class PolyglotWriteNode extends JSBuiltinNode {

        PolyglotWriteNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object write(TruffleObject obj, Object name, Object value,
                        @Cached("createWrite()") Node write,
                        @Cached("create(getContext())") ExportValueNode exportValue) {
            try {
                Object identifier = exportValue.executeWithTarget(name, Undefined.instance);
                Object convertedValue = exportValue.executeWithTarget(value, Undefined.instance);
                return ForeignAccess.sendWrite(write, obj, identifier, convertedValue);
            } catch (UnknownIdentifierException | UnsupportedTypeException | UnsupportedMessageException e) {
                throw Errors.createTypeErrorInteropException(obj, e, Message.WRITE, this);
            }
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isTruffleObject(obj)")
        protected boolean unsupported(Object obj, Object name, Object value) {
            throw Errors.createTypeErrorNotATruffleObject(Message.WRITE);
        }
    }

    @ImportStatic({JSInteropUtil.class})
    abstract static class PolyglotRemoveNode extends JSBuiltinNode {

        PolyglotRemoveNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object remove(TruffleObject obj, Object key,
                        @Cached("createRemove()") Node remove,
                        @Cached("create(getContext())") ExportValueNode exportValue) {
            try {
                Object exportedKey = exportValue.executeWithTarget(key, Undefined.instance);
                return ForeignAccess.sendRemove(remove, obj, exportedKey);
            } catch (UnknownIdentifierException e) {
                return Null.instance;
            } catch (UnsupportedMessageException e) {
                throw Errors.createTypeErrorInteropException(obj, e, Message.REMOVE, this);
            }
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isTruffleObject(obj)")
        protected boolean unsupported(Object obj, Object key) {
            throw Errors.createTypeErrorNotATruffleObject(Message.REMOVE);
        }
    }

    @ImportStatic({JSInteropUtil.class})
    abstract static class PolyglotUnboxValueNode extends JSBuiltinNode {

        PolyglotUnboxValueNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object truffleObject(TruffleObject obj,
                        @Cached("createUnbox()") Node unbox,
                        @Cached("create()") JSForeignToJSTypeNode foreignConvertNode) {
            try {
                return foreignConvertNode.executeWithTarget(ForeignAccess.sendUnbox(unbox, obj));
            } catch (UnsupportedMessageException e) {
                return Null.instance;
            }
        }

        @Specialization(guards = "isJavaPrimitive(obj)")
        protected Object primitive(Object obj) {
            // identity function
            return obj;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"!isTruffleObject(obj)", "!isJavaPrimitive(obj)"})
        protected boolean unsupported(Object obj) {
            throw Errors.createTypeErrorNotATruffleObject(Message.UNBOX);
        }
    }

    @ImportStatic({JSInteropUtil.class})
    abstract static class PolyglotExecuteNode extends JSBuiltinNode {

        PolyglotExecuteNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object execute(TruffleObject obj, Object[] arguments,
                        @Cached("createCall()") Node execute,
                        @Cached("create(getContext())") ExportValueNode exportValue) {
            try {
                TruffleObject target = (TruffleObject) exportValue.executeWithTarget(obj, Undefined.instance);
                Object[] convertedArgs = new Object[arguments.length];
                for (int i = 0; i < arguments.length; i++) {
                    convertedArgs[i] = exportValue.executeWithTarget(arguments[i], Undefined.instance);
                }
                return ForeignAccess.sendExecute(execute, target, convertedArgs);
            } catch (UnsupportedTypeException | ArityException | UnsupportedMessageException e) {
                throw Errors.createTypeErrorInteropException(obj, e, JSInteropUtil.EXECUTE, this);
            }
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isTruffleObject(obj)")
        protected boolean unsupported(Object obj, Object[] arguments) {
            throw Errors.createTypeErrorNotATruffleObject(JSInteropUtil.EXECUTE);
        }
    }

    @ImportStatic({JSInteropUtil.class})
    abstract static class PolyglotConstructNode extends JSBuiltinNode {

        PolyglotConstructNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object doNew(TruffleObject obj, Object[] arguments,
                        @Cached("createNew()") Node newNode,
                        @Cached("create(getContext())") ExportValueNode exportValue) {
            try {
                TruffleObject target = (TruffleObject) exportValue.executeWithTarget(obj, Undefined.instance);
                Object[] convertedArgs = new Object[arguments.length];
                for (int i = 0; i < arguments.length; i++) {
                    convertedArgs[i] = exportValue.executeWithTarget(arguments[i], Undefined.instance);
                }
                return ForeignAccess.sendNew(newNode, target, convertedArgs);
            } catch (UnsupportedTypeException | ArityException | UnsupportedMessageException e) {
                throw Errors.createTypeErrorInteropException(obj, e, JSInteropUtil.NEW, this);
            }
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isTruffleObject(obj)")
        protected boolean unsupported(Object obj, Object[] arguments) {
            throw Errors.createTypeErrorNotATruffleObject(JSInteropUtil.NEW);
        }
    }

    @ImportStatic({JSInteropUtil.class})
    abstract static class PolyglotGetSizeNode extends JSBuiltinNode {

        PolyglotGetSizeNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object getSize(TruffleObject obj,
                        @Cached("createGetSize()") Node getSize) {
            try {
                return ForeignAccess.sendGetSize(getSize, obj);
            } catch (UnsupportedMessageException e) {
                return Null.instance;
            }
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isTruffleObject(obj)")
        protected boolean unsupported(Object obj) {
            throw Errors.createTypeErrorNotATruffleObject(Message.GET_SIZE);
        }
    }

    abstract static class PolyglotEvalNode extends JSBuiltinNode {

        PolyglotEvalNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = {"isString(languageId)", "isString(source)"})
        @TruffleBoundary
        protected Object evalString(Object languageId, Object source) {
            String sourceText = source.toString();
            String languageIdOrMimeType = languageId.toString();
            Source sourceObject = Source.newBuilder(sourceText).name(Evaluator.EVAL_SOURCE_NAME).language(languageIdOrMimeType).mimeType(languageIdOrMimeType).build();

            CallTarget callTarget;

            try {
                callTarget = getContext().getRealm().getEnv().parse(sourceObject);
            } catch (Exception e) {
                throw Errors.createError(e.getMessage());
            }

            return callTarget.call();
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isString(languageId) || !isString(source)")
        protected Object eval(Object languageId, Object source) {
            throw Errors.createTypeError("Expected arguments: (String languageId, String sourceCode)");
        }
    }

    abstract static class PolyglotEvalFileNode extends JSBuiltinNode {

        PolyglotEvalFileNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = {"isString(languageId)", "isString(fileName)"})
        @TruffleBoundary
        protected Object evalString(Object languageId, Object fileName) {
            String fileNameStr = fileName.toString();
            Source source;
            String languageIdOrMimeType = languageId.toString();
            try {
                source = Source.newBuilder(new File(fileNameStr)).language(languageIdOrMimeType).mimeType(languageIdOrMimeType).build();
            } catch (AccessDeniedException e) {
                throw Errors.createError("Cannot evaluate file " + fileNameStr + ": permission denied");
            } catch (NoSuchFileException e) {
                throw Errors.createError("Cannot evaluate file " + fileNameStr + ": no such file");
            } catch (IOException e) {
                throw Errors.createError("Cannot evaluate file: " + e.getMessage());
            }

            CallTarget callTarget;
            try {
                callTarget = getContext().getRealm().getEnv().parse(source);
            } catch (Exception e) {
                throw Errors.createError(e.getMessage());
            }

            return callTarget.call();
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isString(languageId) || !isString(fileName)")
        protected Object eval(Object languageId, Object fileName) {
            throw Errors.createTypeError("Expected arguments: (String languageId, String fileName)");
        }
    }

    @ImportStatic({JSInteropUtil.class})
    abstract static class PolyglotHasKeysNode extends JSBuiltinNode {

        PolyglotHasKeysNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected boolean hasKeys(TruffleObject obj,
                        @Cached("createHasKeys()") Node hasKeys) {
            return ForeignAccess.sendHasKeys(hasKeys, obj);
        }

        @Specialization(guards = "!isTruffleObject(obj)")
        protected boolean unsupported(@SuppressWarnings("unused") Object obj) {
            return false;
        }
    }

    @ImportStatic({JSInteropUtil.class})
    abstract static class PolyglotKeysNode extends JSBuiltinNode {

        PolyglotKeysNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected TruffleObject keys(TruffleObject obj,
                        @Cached("createKeys()") Node keysNode) {
            try {
                return ForeignAccess.sendKeys(keysNode, obj);
            } catch (UnsupportedMessageException e) {
                throw Errors.createTypeErrorInteropException(obj, e, Message.KEYS, this);
            }
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isTruffleObject(obj)")
        protected boolean unsupported(Object obj) {
            throw Errors.createTypeErrorNotATruffleObject(Message.KEYS);
        }
    }

    @ImportStatic({JSInteropUtil.class})
    abstract static class PolyglotIsInstantiableNode extends JSBuiltinNode {

        PolyglotIsInstantiableNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected boolean isInstantiable(TruffleObject obj,
                        @Cached("createIsInstantiable()") Node isInstantiable) {
            return ForeignAccess.sendIsInstantiable(isInstantiable, obj);
        }

        @Specialization(guards = "!isTruffleObject(obj)")
        protected boolean unsupported(@SuppressWarnings("unused") Object obj) {
            return false;
        }
    }

    /**
     * This node exists for debugging purposes. You can call Interop.createForeignObject() from
     * JavaScript code to create a {@link TruffleObject}. It is used to simplify testing interop
     * features in JavaScript code.
     *
     */
    abstract static class PolyglotCreateForeignObjectNode extends JSBuiltinNode {

        private static Class<?> testMapClass;

        PolyglotCreateForeignObjectNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        @TruffleBoundary
        protected Object createForeignObject() {
            if (!JSTruffleOptions.SubstrateVM) {
                try {
                    if (testMapClass == null) {
                        testMapClass = Class.forName("com.oracle.truffle.js.test.interop.object.ForeignTestMap");
                    }
                    return getContext().getRealm().getEnv().asGuestValue(testMapClass.newInstance());
                } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
                    throw Errors.createTypeError("cannot test with ForeignTestMap: " + e.getMessage());
                }
            } else {
                return Undefined.instance;
            }
        }
    }

    /**
     * This node exists for debugging purposes. You can call Interop.createForeignDynamicObject()
     * from JavaScript code to create a {@link DynamicObject}. It is used to simplify testing
     * interop features in JavaScript code.
     *
     */
    abstract static class PolyglotCreateForeignDynamicObjectNode extends JSBuiltinNode {

        private static Class<?> testMapClass;

        PolyglotCreateForeignDynamicObjectNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        @TruffleBoundary
        protected Object createForeignDynamicObject() {
            if (!JSTruffleOptions.SubstrateVM) {
                try {
                    if (testMapClass == null) {
                        testMapClass = Class.forName("com.oracle.truffle.js.test.interop.object.ForeignDynamicObject");
                    }
                    Method createNew = testMapClass.getMethod("createNew");
                    Object result = createNew.invoke(null);
                    return result;
                } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                    throw Errors.createTypeError("cannot test with ForeignDynamicObject: " + e.getMessage());
                }
            } else {
                return Undefined.instance;
            }
        }
    }

    /**
     * Forces the conversion of an (potential) interop value to a JavaScript compliant value. In
     * addition to the conversions forced at the language boundary anyway (e.g., Java primitive
     * types like short or float that are not supported by JavaScript), this operation also converts
     * Nullish interop values to the JavaScript null value, and unboxes boxed TruffleObjects.
     *
     */
    abstract static class PolyglotToJSValueNode extends JSBuiltinNode {
        // this is most likely redundant, we do it to be sure
        @Child private JSForeignToJSTypeNode foreignToJSNode = JSForeignToJSTypeNode.create();
        @Child private Node isNullNode;
        @Child private Node isBoxedNode;
        @Child private Node unboxNode;

        PolyglotToJSValueNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object execute(Object value) {
            Object imported = foreignToJSNode.executeWithTarget(value);

            if (imported instanceof TruffleObject) {
                TruffleObject truffleObj = (TruffleObject) imported;
                if (ForeignAccess.sendIsNull(getIsNull(), truffleObj)) {
                    return Null.instance;
                } else if (ForeignAccess.sendIsBoxed(getIsBoxed(), truffleObj)) {
                    try {
                        return foreignToJSNode.executeWithTarget(ForeignAccess.sendUnbox(getUnbox(), truffleObj));
                    } catch (UnsupportedMessageException e) {
                        return Null.instance;
                    }
                }
            }
            return imported;
        }

        private Node getUnbox() {
            if (unboxNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                unboxNode = insert(JSInteropUtil.createUnbox());
            }
            return unboxNode;
        }

        private Node getIsNull() {
            if (isNullNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                isNullNode = insert(JSInteropUtil.createIsNull());
            }
            return isNullNode;
        }

        public Node getIsBoxed() {
            if (isBoxedNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                isBoxedNode = insert(JSInteropUtil.createIsBoxed());
            }
            return isBoxedNode;
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
            this.exportValueNode = ExportValueNode.create(context);
        }

        @Specialization
        protected Object execute(Object value) {
            return exportValueNode.executeWithTarget(value, Undefined.instance);
        }
    }
}
