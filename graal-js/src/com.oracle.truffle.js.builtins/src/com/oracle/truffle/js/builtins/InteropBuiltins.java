/*
 * Copyright (c) 2012, 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.builtins;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.MessageResolution;
import com.oracle.truffle.api.interop.Resolve;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.interop.java.JavaInterop;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.js.builtins.InteropBuiltinsFactory.InteropConstructNodeGen;
import com.oracle.truffle.js.builtins.InteropBuiltinsFactory.InteropCreateForeignDynamicObjectNodeGen;
import com.oracle.truffle.js.builtins.InteropBuiltinsFactory.InteropCreateForeignObjectNodeGen;
import com.oracle.truffle.js.builtins.InteropBuiltinsFactory.InteropEvalNodeGen;
import com.oracle.truffle.js.builtins.InteropBuiltinsFactory.InteropExecuteNodeGen;
import com.oracle.truffle.js.builtins.InteropBuiltinsFactory.InteropExportNodeGen;
import com.oracle.truffle.js.builtins.InteropBuiltinsFactory.InteropGetSizeNodeGen;
import com.oracle.truffle.js.builtins.InteropBuiltinsFactory.InteropHasKeysNodeGen;
import com.oracle.truffle.js.builtins.InteropBuiltinsFactory.InteropHasSizeNodeGen;
import com.oracle.truffle.js.builtins.InteropBuiltinsFactory.InteropImportNodeGen;
import com.oracle.truffle.js.builtins.InteropBuiltinsFactory.InteropIsBoxedPrimitiveNodeGen;
import com.oracle.truffle.js.builtins.InteropBuiltinsFactory.InteropIsExecutableNodeGen;
import com.oracle.truffle.js.builtins.InteropBuiltinsFactory.InteropIsInstantiableNodeGen;
import com.oracle.truffle.js.builtins.InteropBuiltinsFactory.InteropIsNullNodeGen;
import com.oracle.truffle.js.builtins.InteropBuiltinsFactory.InteropKeysNodeGen;
import com.oracle.truffle.js.builtins.InteropBuiltinsFactory.InteropParseNodeGen;
import com.oracle.truffle.js.builtins.InteropBuiltinsFactory.InteropReadNodeGen;
import com.oracle.truffle.js.builtins.InteropBuiltinsFactory.InteropRemoveNodeGen;
import com.oracle.truffle.js.builtins.InteropBuiltinsFactory.InteropToInteropValueNodeGen;
import com.oracle.truffle.js.builtins.InteropBuiltinsFactory.InteropToJSValueNodeGen;
import com.oracle.truffle.js.builtins.InteropBuiltinsFactory.InteropUnboxValueNodeGen;
import com.oracle.truffle.js.builtins.InteropBuiltinsFactory.InteropWriteNodeGen;
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

public final class InteropBuiltins extends JSBuiltinsContainer.SwitchEnum<InteropBuiltins.Interop> {
    protected InteropBuiltins() {
        super(JSRealm.INTEROP_CLASS_NAME, Interop.class);
    }

    public enum Interop implements BuiltinEnum<Interop> {
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
        toInteropValue(1),
        // import and export
        export(2),
        import_(1),
        eval(2),
        parse(2),
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

        Interop(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, Interop builtinEnum) {
        switch (builtinEnum) {
            case isExecutable:
                return InteropIsExecutableNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            case isBoxed:
                return InteropIsBoxedPrimitiveNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            case isNull:
                return InteropIsNullNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            case isInstantiable:
                return InteropIsInstantiableNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            case hasSize:
                return InteropHasSizeNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            case read:
                return InteropReadNodeGen.create(context, builtin, args().fixedArgs(2).createArgumentNodes(context));
            case write:
                return InteropWriteNodeGen.create(context, builtin, args().fixedArgs(3).createArgumentNodes(context));
            case remove:
                return InteropRemoveNodeGen.create(context, builtin, args().fixedArgs(2).createArgumentNodes(context));
            case unbox:
                return InteropUnboxValueNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            case construct:
                return InteropConstructNodeGen.create(context, builtin, args().fixedArgs(1).varArgs().createArgumentNodes(context));
            case execute:
                return InteropExecuteNodeGen.create(context, builtin, args().fixedArgs(1).varArgs().createArgumentNodes(context));
            case getSize:
                return InteropGetSizeNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            case export:
                return InteropExportNodeGen.create(context, builtin, args().fixedArgs(2).createArgumentNodes(context));
            case import_:
                return InteropImportNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            case eval:
                return InteropEvalNodeGen.create(context, builtin, args().fixedArgs(2).createArgumentNodes(context));
            case parse:
                return InteropParseNodeGen.create(context, builtin, args().fixedArgs(2).createArgumentNodes(context));
            case keys:
                return InteropKeysNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            case toJSValue:
                return InteropToJSValueNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            case toInteropValue:
                return InteropToInteropValueNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            case hasKeys:
                return InteropHasKeysNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            case createForeignObject:
                if (!JSTruffleOptions.SubstrateVM) {
                    return InteropCreateForeignObjectNodeGen.create(context, builtin, args().fixedArgs(0).createArgumentNodes(context));
                }
                break;
            case createForeignDynamicObject:
                if (!JSTruffleOptions.SubstrateVM) {
                    return InteropCreateForeignDynamicObjectNodeGen.create(context, builtin, args().fixedArgs(0).createArgumentNodes(context));
                }
                break;
        }
        return null;
    }

    @ImportStatic({JSInteropUtil.class})
    abstract static class InteropExportNode extends JSBuiltinNode {
        @Child private ExportValueNode export = ExportValueNode.create();
        @Child private Node writeBinding = Message.WRITE.createNode();

        InteropExportNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object doString(String identifier, Object value) {
            TruffleObject polyglotBindings = (TruffleObject) getContext().getEnv().getPolyglotBindings();
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
    abstract static class InteropImportNode extends JSBuiltinNode {
        @Child private Node readBinding = Message.READ.createNode();

        InteropImportNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object doString(String identifier) {
            TruffleObject polyglotBindings = (TruffleObject) getContext().getEnv().getPolyglotBindings();
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
    abstract static class InteropIsExecutableNode extends JSBuiltinNode {

        InteropIsExecutableNode(JSContext context, JSBuiltin builtin) {
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
    abstract static class InteropIsBoxedPrimitiveNode extends JSBuiltinNode {

        InteropIsBoxedPrimitiveNode(JSContext context, JSBuiltin builtin) {
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
    abstract static class InteropIsNullNode extends JSBuiltinNode {

        InteropIsNullNode(JSContext context, JSBuiltin builtin) {
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
    abstract static class InteropHasSizeNode extends JSBuiltinNode {

        InteropHasSizeNode(JSContext context, JSBuiltin builtin) {
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
    abstract static class InteropReadNode extends JSBuiltinNode {

        InteropReadNode(JSContext context, JSBuiltin builtin) {
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
    abstract static class InteropWriteNode extends JSBuiltinNode {

        InteropWriteNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object write(TruffleObject obj, Object name, Object value,
                        @Cached("createWrite()") Node write,
                        @Cached("create()") ExportValueNode exportValue) {
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
    abstract static class InteropRemoveNode extends JSBuiltinNode {

        InteropRemoveNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object remove(TruffleObject obj, Object key,
                        @Cached("createRemove()") Node remove,
                        @Cached("create()") ExportValueNode exportValue) {
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
    abstract static class InteropUnboxValueNode extends JSBuiltinNode {

        InteropUnboxValueNode(JSContext context, JSBuiltin builtin) {
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
    abstract static class InteropExecuteNode extends JSBuiltinNode {

        InteropExecuteNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object execute(TruffleObject obj, Object[] arguments,
                        @Cached("createCall()") Node execute,
                        @Cached("create()") ExportValueNode exportValue) {
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
    abstract static class InteropConstructNode extends JSBuiltinNode {

        InteropConstructNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object doNew(TruffleObject obj, Object[] arguments,
                        @Cached("createNew()") Node newNode,
                        @Cached("create()") ExportValueNode exportValue) {
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
    abstract static class InteropGetSizeNode extends JSBuiltinNode {

        InteropGetSizeNode(JSContext context, JSBuiltin builtin) {
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

    abstract static class InteropEvalNode extends JSBuiltinNode {

        InteropEvalNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = {"isString(mimeType)", "isString(source)"})
        @TruffleBoundary
        protected Object evalString(Object mimeType, Object source) {
            String sourceText = source.toString();
            Source sourceObject = Source.newBuilder(sourceText).name(Evaluator.EVAL_SOURCE_NAME).mimeType(mimeType.toString()).build();

            CallTarget callTarget;

            try {
                callTarget = getContext().getEnv().parse(sourceObject);
            } catch (Exception e) {
                throw Errors.createError(e.getMessage());
            }

            return callTarget.call();
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isString(mimeType) || !isString(source)")
        protected Object eval(Object mimeType, Object source) {
            throw Errors.createTypeError("Expected arguments: (String mimeType, String sourceCode)");
        }
    }

    abstract static class InteropParseNode extends JSBuiltinNode {

        InteropParseNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = {"isString(mimeType)", "isString(fileName)"})
        @TruffleBoundary
        protected Object evalString(Object mimeType, Object fileName) {
            String fileNameStr = fileName.toString();
            Source source;
            try {
                source = Source.newBuilder(new File(fileNameStr)).mimeType(mimeType.toString()).build();
            } catch (IOException e) {
                throw Errors.createError(e.getMessage());
            }

            CallTarget callTarget;
            try {
                callTarget = getContext().getEnv().parse(source);
            } catch (Exception e) {
                throw Errors.createError(e.getMessage());
            }

            return new CallTargetObject(callTarget);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isString(mimeType) || !isString(fileName)")
        protected Object eval(Object mimeType, Object fileName) {
            throw Errors.createTypeError("Expected arguments: (String mimeType, String fileName)");
        }
    }

    @MessageResolution(receiverType = CallTargetObject.class)
    static class CallTargetObject implements TruffleObject {
        final CallTarget callTarget;

        CallTargetObject(CallTarget callTarget) {
            this.callTarget = callTarget;
        }

        @Override
        public ForeignAccess getForeignAccess() {
            return CallTargetObjectForeign.ACCESS;
        }

        static boolean isInstance(TruffleObject obj) {
            return obj instanceof CallTargetObject;
        }

        @Resolve(message = "EXECUTE")
        abstract static class ExecuteNode extends Node {
            @TruffleBoundary
            protected Object access(CallTargetObject obj, Object[] args) {
                return obj.callTarget.call(args);
            }
        }

        @Resolve(message = "IS_EXECUTABLE")
        abstract static class IsExecutableNode extends Node {
            protected boolean access(@SuppressWarnings("unused") CallTargetObject obj) {
                return true;
            }
        }
    }

    @ImportStatic({JSInteropUtil.class})
    abstract static class InteropHasKeysNode extends JSBuiltinNode {

        InteropHasKeysNode(JSContext context, JSBuiltin builtin) {
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
    abstract static class InteropKeysNode extends JSBuiltinNode {

        InteropKeysNode(JSContext context, JSBuiltin builtin) {
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
    abstract static class InteropIsInstantiableNode extends JSBuiltinNode {

        InteropIsInstantiableNode(JSContext context, JSBuiltin builtin) {
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
    abstract static class InteropCreateForeignObjectNode extends JSBuiltinNode {

        private static Class<?> testMapClass;

        InteropCreateForeignObjectNode(JSContext context, JSBuiltin builtin) {
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
                    return JavaInterop.asTruffleObject(testMapClass.newInstance());
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
    abstract static class InteropCreateForeignDynamicObjectNode extends JSBuiltinNode {

        private static Class<?> testMapClass;

        InteropCreateForeignDynamicObjectNode(JSContext context, JSBuiltin builtin) {
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
    abstract static class InteropToJSValueNode extends JSBuiltinNode {
        // this is most likely redundant, we do it to be sure
        @Child private JSForeignToJSTypeNode foreignToJSNode = JSForeignToJSTypeNode.create();
        @Child private Node isNullNode;
        @Child private Node isBoxedNode;
        @Child private Node unboxNode;

        InteropToJSValueNode(JSContext context, JSBuiltin builtin) {
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
    abstract static class InteropToInteropValueNode extends JSBuiltinNode {
        @Child private ExportValueNode exportValueNode = ExportValueNode.create();

        InteropToInteropValueNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object execute(Object value) {
            return exportValueNode.executeWithTarget(value, Undefined.instance);
        }
    }
}
