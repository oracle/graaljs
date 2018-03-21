/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.parser;

import java.util.Arrays;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Executed;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.object.Property;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.js.builtins.ArrayPrototypeBuiltins.ArrayPrototype;
import com.oracle.truffle.js.builtins.ObjectFunctionBuiltinsFactory.ObjectDefinePropertyNodeGen;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.NodeFactory;
import com.oracle.truffle.js.nodes.ScriptNode;
import com.oracle.truffle.js.nodes.access.GetIteratorNode;
import com.oracle.truffle.js.nodes.access.GetViewValueNode;
import com.oracle.truffle.js.nodes.access.GlobalConstantNode;
import com.oracle.truffle.js.nodes.access.GlobalObjectNode;
import com.oracle.truffle.js.nodes.access.HasHiddenKeyCacheNode;
import com.oracle.truffle.js.nodes.access.IsArrayNode.IsArrayWrappedNode;
import com.oracle.truffle.js.nodes.access.IsJSClassNode;
import com.oracle.truffle.js.nodes.access.IsObjectNode.IsObjectWrappedNode;
import com.oracle.truffle.js.nodes.access.JSConstantNode;
import com.oracle.truffle.js.nodes.access.JSTargetableNode;
import com.oracle.truffle.js.nodes.access.OrdinaryCreateFromConstructorNode;
import com.oracle.truffle.js.nodes.access.RequireObjectNode;
import com.oracle.truffle.js.nodes.access.SetViewValueNode;
import com.oracle.truffle.js.nodes.cast.JSEnqueueJobNode;
import com.oracle.truffle.js.nodes.cast.JSIsConstructorFunctionNode;
import com.oracle.truffle.js.nodes.cast.JSToBooleanNode;
import com.oracle.truffle.js.nodes.cast.JSToConstructorFunctionNode;
import com.oracle.truffle.js.nodes.cast.JSToInt32Node;
import com.oracle.truffle.js.nodes.cast.JSToNumberNode;
import com.oracle.truffle.js.nodes.cast.JSToNumberNode.JSToNumberWrapperNode;
import com.oracle.truffle.js.nodes.cast.JSToObjectNode.JSToObjectWrapperNode;
import com.oracle.truffle.js.nodes.cast.JSToPropertyKeyNode.JSToPropertyKeyWrapperNode;
import com.oracle.truffle.js.nodes.cast.JSToStringNode;
import com.oracle.truffle.js.nodes.cast.JSToStringNode.JSToStringWrapperNode;
import com.oracle.truffle.js.nodes.cast.JSToUInt32Node.JSToUInt32WrapperNode;
import com.oracle.truffle.js.nodes.control.StatementNode;
import com.oracle.truffle.js.nodes.function.AbstractFunctionArgumentsNode;
import com.oracle.truffle.js.nodes.function.CreateMethodPropertyNode;
import com.oracle.truffle.js.nodes.function.JSFunctionArgumentsNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.nodes.unary.IsCallableNode;
import com.oracle.truffle.js.parser.InternalTranslatorFactory.InternalStringReplaceNodeGen;
import com.oracle.truffle.js.parser.env.Environment;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSErrorType;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSArrayBuffer;
import com.oracle.truffle.js.runtime.builtins.JSArrayBufferView;
import com.oracle.truffle.js.runtime.builtins.JSBoolean;
import com.oracle.truffle.js.runtime.builtins.JSClass;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunctionData;
import com.oracle.truffle.js.runtime.builtins.JSMap;
import com.oracle.truffle.js.runtime.builtins.JSNumber;
import com.oracle.truffle.js.runtime.builtins.JSPromise;
import com.oracle.truffle.js.runtime.builtins.JSProxy;
import com.oracle.truffle.js.runtime.builtins.JSRegExp;
import com.oracle.truffle.js.runtime.builtins.JSSet;
import com.oracle.truffle.js.runtime.builtins.JSString;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSProperty;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.JSHashMap;

final class InternalTranslator extends GraalJSTranslator {
    static final String INTERNAL_NAME = "Internal";
    static final NodeFactory INTERNAL_NODE_FACTORY = new InternalNodeFactory();

    private static final class InternalNodeFactory extends NodeFactory {
        @Override
        public JSTargetableNode createReadGlobalProperty(JSContext context, String name) {
            if (name.equals(INTERNAL_NAME)) {
                return new InternalNode();
            }
            Object value = getGlobalPropertyValue(context.getRealm(), name);
            if (value != null) {
                return GlobalConstantNode.createGlobalConstant(context, name, value);
            } else {
                return GlobalConstantNode.createGlobalConstant(context, name, Undefined.instance);
            }
        }

        private static Object getGlobalPropertyValue(JSRealm realm, String name) {
            switch (name) {
                case "Object":
                    return realm.getObjectConstructor();
                case "Array":
                    return realm.getArrayConstructor().getFunctionObject();
                case "String":
                    return realm.getStringConstructor().getFunctionObject();
                case "Date":
                    return realm.getDateConstructor().getFunctionObject();
                case "DataView":
                    return realm.getDataViewConstructor().getFunctionObject();
                case "Symbol":
                    return realm.getSymbolConstructor() == null ? null : realm.getSymbolConstructor().getFunctionObject();
                case "Map":
                    return realm.getMapConstructor() == null ? null : realm.getMapConstructor().getFunctionObject();
                case "Set":
                    return realm.getSetConstructor() == null ? null : realm.getSetConstructor().getFunctionObject();
                case "Proxy":
                    return realm.getProxyConstructor().getFunctionObject();
                case "TypeError":
                    return realm.getErrorConstructor(JSErrorType.TypeError).getFunctionObject();
                default:
                    throw new AssertionError("Reference to unexpected global: " + name);
            }
        }

        @Override
        public JavaScriptNode createReadProperty(JSContext context, JavaScriptNode base, String propertyName) {
            if (base instanceof InternalNode) {
                switch (propertyName) {
                    case "AnnexB":
                        return JSConstantNode.createBoolean(context.isOptionAnnexB());
                    case "V8CompatibilityMode":
                        return JSConstantNode.createBoolean(context.isOptionV8CompatibilityMode());
                    case "TypedArray":
                        return JSConstantNode.create(context.getRealm().getTypedArrayConstructor());
                }
                return new InternalPropertyNode(propertyName);
            } else if (base instanceof GlobalConstantNode) {
                Object constant = ((GlobalConstantNode) base).getValue();
                if (JSObject.isDynamicObject(constant)) {
                    DynamicObject object = (DynamicObject) (constant);
                    Object value = getDataPropertyValue(object, propertyName);
                    if (value != null) {
                        return JSConstantNode.create(value);
                    }
                }
            }
            return super.createReadProperty(context, base, propertyName);
        }

        @Override
        public JavaScriptNode createFunctionCall(JSContext context, JavaScriptNode function, JavaScriptNode[] arguments) {
            if (function instanceof InternalPropertyNode) {
                JSRealm realm = context.getRealm();
                String name = ((InternalPropertyNode) function).name;
                switch (name) {
                    case "ToObject":
                    case "RequireObjectCoercible":
                        return JSToObjectWrapperNode.createToObject(context, arguments[0]);
                    case "ToNumber":
                        return JSToNumberWrapperNode.create(arguments[0]);
                    case "ToString":
                        return JSToStringWrapperNode.create(arguments[0]);
                    case "ToInt32":
                        return JSToInt32Node.create(arguments[0]);
                    case "ToUint32":
                        return JSToUInt32WrapperNode.create(arguments[0]);
                    case "ToBoolean":
                        return JSToBooleanNode.create(arguments[0]);
                    case "CallFunction":
                        return JSFunctionCallNode.createInternalCall(arguments);
                    case "GetViewValue":
                    case "SetViewValue":
                        return makeDataViewValueNode(context, name, arguments);
                    case "GetGlobalObject":
                        return GlobalObjectNode.create(context);
                    case "NextTick":
                        return JSEnqueueJobNode.create(context, arguments[0]);
                    case "MakeConstructor":
                        return JSToConstructorFunctionNode.create(context, arguments[0]);
                    case "IsConstructor":
                        return JSIsConstructorFunctionNode.create(arguments[0]);
                    case "ToLength":
                        return com.oracle.truffle.js.builtins.DebugBuiltinsFactory.DebugToLengthNodeGen.create(context, null, arguments);
                    case "IsFunction":
                    case "IsRegExp":
                    case "IsBooleanWrapper":
                    case "IsStringWrapper":
                    case "IsNumberWrapper":
                    case "IsArrayBufferView":
                    case "IsMap":
                    case "IsPromise":
                    case "IsProxy":
                    case "IsSet":
                        return makeInstanceOfJSClassNode(name, arguments);
                    case "IsArray":
                        return IsArrayWrappedNode.createIsArray(arguments[0]);
                    case "IsObject":
                        return IsObjectWrappedNode.create(arguments[0]);
                    case "IsValidTypedArray":
                        return new InternalIsValidTypedArrayNode(arguments[0]);
                    case "HasDetachedBuffer":
                        return new InternalHasDetachedBufferNode(arguments[0]);
                    case "RequireObject":
                        return RequireObjectNode.create(arguments[0]);
                    case "HiddenKey":
                        return JSConstantNode.create(getPrivateHiddenKey((String) ((JSConstantNode) arguments[0]).getValue()));
                    case "GetHiddenKey":
                        return JSConstantNode.create(getSharedHiddenKey((String) ((JSConstantNode) arguments[0]).getValue()));
                    case "HasHiddenKey":
                        return new InternalHasHiddenKeyNode(arguments[0], arguments[1]);
                    case "CreateMethodProperty":
                        return CreateMethodPropertyNode.create(context, ((JSConstantNode) arguments[1]).getValue(), arguments[0], arguments[2]);
                    case "GetIteratorPrototype":
                        return JSConstantNode.create(realm.getIteratorPrototype());
                    case "GetKeyFromMapCursor":
                        return new InternalGetKeyFromMapCursorNode(arguments[0]);
                    case "GetValueFromMapCursor":
                        return new InternalGetValueFromMapCursorNode(arguments[0]);
                    case "AdvanceMapCursor":
                        return new InternalAdvanceMapCursorNode(arguments[0]);
                    case "GetMapCursor":
                        return new InternalGetMapCursorNode(arguments[0]);
                    case "RevokeProxy":
                        return new RevokeProxyNode(arguments[0]);
                    case "SetFunctionName":
                        return new InternalSetFunctionNameNode(arguments[0], arguments[1]);
                    case "StringReplace":
                        return InternalStringReplaceNode.create(arguments[0], arguments[1], arguments[2]);
                    case "ObjectDefineProperty":
                        return ObjectDefinePropertyNodeGen.create(context, null, arguments);
                    case "ToPropertyKey":
                        return JSToPropertyKeyWrapperNode.create(arguments[0]);
                    case "CreatePromiseFromConstructor":
                        return OrdinaryCreateFromConstructorNode.create(context, arguments[0], JSRealm::getPromisePrototype, JSPromise.INSTANCE);
                    case "IsCallable":
                        return IsCallableNode.create(arguments[0]);
                    case "Assert":
                        return createAssert(arguments[0]);
                    case "ArrayPush":
                        DynamicObject arrayPushFunction = context.getRealm().lookupFunction(JSArray.PROTOTYPE_NAME, ArrayPrototype.push.getName());
                        JavaScriptNode functionNode = JSConstantNode.create(arrayPushFunction);
                        AbstractFunctionArgumentsNode args = JSFunctionArgumentsNode.create(Arrays.copyOfRange(arguments, 1, arguments.length));
                        return JSFunctionCallNode.create(functionNode, arguments[0], args, false, false);
                    case "GetIterator":
                        return GetIteratorNode.create(context, arguments[0]);
                    case "RegisterAsyncFunctionBuiltins":
                        return InternalRegisterAsyncFunctionBuiltins.create(context, arguments[0], arguments[1]);
                    case "PromiseRejectionTracker":
                        return PromiseRejectionTrackerNode.create(context, arguments[0], arguments[1]);
                    case "PromiseHook":
                        return PromiseHookNode.create(context, arguments[0], arguments[1]);
                }
                return new InternalFunctionCallNode(name);
            }
            return super.createFunctionCall(context, function, arguments);
        }

        private JavaScriptNode createAssert(JavaScriptNode assertion) {
            boolean ea = false;
            assert (ea = true) == true;
            if (ea) {
                class AssertNode extends StatementNode {
                    @Child private JavaScriptNode assertionNode;

                    AssertNode(JavaScriptNode assertionNode) {
                        this.assertionNode = JSToBooleanNode.create(assertionNode);
                    }

                    @Override
                    public Object execute(VirtualFrame frame) {
                        if (!executeConditionAsBoolean(frame, assertionNode)) {
                            throwAssertionError();
                        }
                        return EMPTY;
                    }

                    @TruffleBoundary
                    private void throwAssertionError() {
                        SourceSection sourceSection = getSourceSection();
                        throw new AssertionError(sourceSection == null ? "Internal.Assert" : sourceSection.getCharacters());
                    }

                    @Override
                    protected JavaScriptNode copyUninitialized() {
                        return new AssertNode(cloneUninitialized(assertionNode));
                    }
                }
                return new AssertNode(assertion);
            }
            return createEmpty();
        }

        private static HiddenKey getPrivateHiddenKey(String keyName) {
            return new HiddenKey(keyName);
        }

        private static HiddenKey getSharedHiddenKey(String keyName) {
            switch (keyName) {
                case "IteratedObject":
                    return JSRuntime.ITERATED_OBJECT_ID;
                case "IteratorNextIndex":
                    return JSRuntime.ITERATOR_NEXT_INDEX;
                case "PromiseState":
                    return JSPromise.PROMISE_STATE;
                case "PromiseResult":
                    return JSPromise.PROMISE_RESULT;
                case "OnFinally":
                    return JSPromise.PROMISE_ON_FINALLY;
                case "PromiseFinallyConstructor":
                    return JSPromise.PROMISE_FINALLY_CONSTRUCTOR;
                case "PromiseIsHandled":
                    return JSPromise.PROMISE_IS_HANDLED;
            }
            throw new IllegalArgumentException(keyName);
        }

        private static JavaScriptNode makeDataViewValueNode(JSContext context, String name, JavaScriptNode[] arguments) {
            String type = (String) ((JSConstantNode) arguments[3]).getValue();
            switch (name) {
                case "GetViewValue":
                    return GetViewValueNode.create(context, type, arguments[0], arguments[1], arguments[2]);
                case "SetViewValue":
                    return SetViewValueNode.create(context, type, arguments[0], arguments[1], arguments[2], arguments[4]);
            }
            throw Errors.shouldNotReachHere();
        }

        private static JavaScriptNode makeInstanceOfJSClassNode(String name, JavaScriptNode[] arguments) {
            JSClass jsclass;
            switch (name) {
                case "IsFunction":
                    jsclass = JSFunction.INSTANCE;
                    break;
                case "IsRegExp":
                    jsclass = JSRegExp.INSTANCE;
                    break;
                case "IsBooleanWrapper":
                    jsclass = JSBoolean.INSTANCE;
                    break;
                case "IsStringWrapper":
                    jsclass = JSString.INSTANCE;
                    break;
                case "IsNumberWrapper":
                    jsclass = JSNumber.INSTANCE;
                    break;
                case "IsArrayBufferView":
                    jsclass = JSArrayBufferView.INSTANCE;
                    break;
                case "IsMap":
                    jsclass = JSMap.INSTANCE;
                    break;
                case "IsSet":
                    jsclass = JSSet.INSTANCE;
                    break;
                case "IsProxy":
                    jsclass = JSProxy.INSTANCE;
                    break;
                case "IsPromise":
                    jsclass = JSPromise.INSTANCE;
                    break;
                default:
                    throw Errors.shouldNotReachHere();
            }
            return IsJSClassNode.create(jsclass, arguments[0]);
        }
    }

    protected static final class InternalNode extends JSTargetableNode {
        @Override
        public Object execute(VirtualFrame frame) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object executeWithTarget(VirtualFrame frame, Object target) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object evaluateTarget(VirtualFrame frame) {
            throw new UnsupportedOperationException();
        }

        @Override
        public JavaScriptNode getTarget() {
            throw new UnsupportedOperationException();
        }
    }

    protected static final class InternalPropertyNode extends JavaScriptNode {
        protected final String name;

        public InternalPropertyNode(String propertyName) {
            this.name = propertyName;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            throw new UnsupportedOperationException(name);
        }
    }

    protected static final class InternalFunctionCallNode extends JavaScriptNode {
        protected final String name;

        public InternalFunctionCallNode(String name) {
            this.name = name;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            throw new UnsupportedOperationException(getErrorMessage());
        }

        @TruffleBoundary
        private String getErrorMessage() {
            return name + "()";
        }
    }

    protected static Object getDataPropertyValue(DynamicObject object, Object key) {
        Property property = object.getShape().getProperty(key);
        if (property != null && JSProperty.isData(property)) {
            return property.get(object, object.getShape());
        }
        return null;
    }

    private InternalTranslator(NodeFactory factory, JSContext context, Source source, Environment env) {
        super(factory, context, source, env, false);
    }

    private static ScriptNode translateFunction(NodeFactory nodeFactory, JSContext context, Source source, com.oracle.js.parser.ir.FunctionNode rootNode) {
        return new InternalTranslator(nodeFactory, context, source, null).translateScript(rootNode);
    }

    static ScriptNode translateSource(NodeFactory nodeFactory, JSContext context, Source source) {
        return translateFunction(nodeFactory, context, source, GraalJSParserHelper.parseScript(source, internalParserOptions()));
    }

    private static GraalJSParserOptions internalParserOptions() {
        return new GraalJSParserOptions().putEcmaScriptVersion(6);
    }

    static ScriptNode translateSource(JSContext context, Source source) {
        return translateSource(INTERNAL_NODE_FACTORY, context, source);
    }

    @Override
    protected GraalJSTranslator newTranslator(Environment env) {
        return new InternalTranslator(factory, context, source, env);
    }

    public abstract static class InternalMapCursorOperation extends JavaScriptNode {
        @Child protected JavaScriptNode indexNode;
        @Child private JSToNumberNode toNumberNode;

        InternalMapCursorOperation(JavaScriptNode indexNode) {
            this.indexNode = indexNode;
        }

        public static JSHashMap getInternalMap(DynamicObject collection) {
            if (JSSet.isJSSet(collection)) {
                return JSSet.getInternalSet(collection);
            } else if (JSMap.isJSMap(collection)) {
                return JSMap.getInternalMap(collection);
            }
            throw Errors.createTypeError("collection expected");
        }

        protected Object executeIndexNode(VirtualFrame frame) {
            return indexNode.execute(frame);
        }
    }

    public static class InternalGetKeyFromMapCursorNode extends InternalMapCursorOperation {

        InternalGetKeyFromMapCursorNode(JavaScriptNode index) {
            super(index);
        }

        @Override
        public Object execute(VirtualFrame frame) {
            return ((JSHashMap.Cursor) executeIndexNode(frame)).getKey();
        }

        @Override
        protected JavaScriptNode copyUninitialized() {
            return new InternalGetKeyFromMapCursorNode(cloneUninitialized(indexNode));
        }
    }

    public static class InternalGetValueFromMapCursorNode extends InternalMapCursorOperation {

        InternalGetValueFromMapCursorNode(JavaScriptNode index) {
            super(index);
        }

        @Override
        public Object execute(VirtualFrame frame) {
            return ((JSHashMap.Cursor) executeIndexNode(frame)).getValue();
        }

        @Override
        protected JavaScriptNode copyUninitialized() {
            return new InternalGetValueFromMapCursorNode(cloneUninitialized(indexNode));
        }
    }

    public static class InternalAdvanceMapCursorNode extends InternalMapCursorOperation {

        InternalAdvanceMapCursorNode(JavaScriptNode index) {
            super(index);
        }

        @Override
        public Boolean execute(VirtualFrame frame) {
            return ((JSHashMap.Cursor) executeIndexNode(frame)).advance();
        }

        @Override
        protected JavaScriptNode copyUninitialized() {
            return new InternalAdvanceMapCursorNode(cloneUninitialized(indexNode));
        }

        @Override
        public boolean isResultAlwaysOfType(Class<?> clazz) {
            return clazz == Boolean.class || clazz == boolean.class;
        }
    }

    public static class InternalGetMapCursorNode extends JavaScriptNode {
        @Child private JavaScriptNode collectionNode;

        InternalGetMapCursorNode(JavaScriptNode collection) {
            this.collectionNode = collection;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            DynamicObject collection = (DynamicObject) collectionNode.execute(frame);
            return InternalMapCursorOperation.getInternalMap(collection).getEntries();
        }

        @Override
        protected JavaScriptNode copyUninitialized() {
            return new InternalGetMapCursorNode(cloneUninitialized(collectionNode));
        }
    }

    public static class InternalRegisterAsyncFunctionBuiltins extends JavaScriptNode {

        @Child private JavaScriptNode performPromiseThen;
        @Child private JavaScriptNode newDefaultCapability;

        private final JSContext context;

        InternalRegisterAsyncFunctionBuiltins(JSContext context, JavaScriptNode newDefaultCapability, JavaScriptNode performPromiseThen) {
            this.context = context;
            this.newDefaultCapability = newDefaultCapability;
            this.performPromiseThen = performPromiseThen;
        }

        public static JavaScriptNode create(JSContext context, JavaScriptNode newDefaultCapability, JavaScriptNode performPromiseThen) {
            return new InternalRegisterAsyncFunctionBuiltins(context, newDefaultCapability, performPromiseThen);
        }

        @Override
        public Object execute(VirtualFrame frame) {
            DynamicObject constructor = (DynamicObject) newDefaultCapability.execute(frame);
            assert JSFunction.isJSFunction(constructor);
            context.setAsyncFunctionPromiseCapabilityConstructor(constructor);
            DynamicObject promiseThen = (DynamicObject) performPromiseThen.execute(frame);
            assert JSFunction.isJSFunction(promiseThen);
            context.setPerformPromiseThen(promiseThen);
            return Undefined.instance;
        }

        @Override
        protected JavaScriptNode copyUninitialized() {
            return new InternalRegisterAsyncFunctionBuiltins(context, cloneUninitialized(newDefaultCapability), cloneUninitialized(performPromiseThen));
        }
    }

    public static class InternalSetFunctionNameNode extends JavaScriptNode {
        @Child private JavaScriptNode targetNode;
        @Child private JavaScriptNode nameNode;

        InternalSetFunctionNameNode(JavaScriptNode targetNode, JavaScriptNode nameNode) {
            this.targetNode = targetNode;
            this.nameNode = nameNode;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            return setName(targetNode.execute(frame), nameNode.execute(frame));
        }

        @TruffleBoundary
        protected Object setName(Object target, Object name) {
            JSFunctionData fd = JSFunction.getFunctionData(JSObject.castJSObject(target));
            fd.setName(name.toString());
            return Undefined.instance;
        }

        @Override
        protected JavaScriptNode copyUninitialized() {
            return new InternalSetFunctionNameNode(cloneUninitialized(targetNode), cloneUninitialized(nameNode));
        }
    }

    public static class InternalHasHiddenKeyNode extends JavaScriptNode {
        @Child private JavaScriptNode targetNode;
        @Child private JavaScriptNode keyNode;
        @Child private HasHiddenKeyCacheNode hasHiddenPropertyNode;

        InternalHasHiddenKeyNode(JavaScriptNode targetNode, JavaScriptNode keyNode) {
            this.targetNode = targetNode;
            this.keyNode = keyNode;
        }

        @Override
        public Boolean execute(VirtualFrame frame) {
            return executeBoolean(frame);
        }

        @Override
        public boolean executeBoolean(VirtualFrame frame) {
            return hasHiddenKey(targetNode.execute(frame), keyNode.execute(frame));
        }

        private boolean hasHiddenKey(Object target, Object key) {
            HiddenKey hiddenKey = (HiddenKey) key;
            if (hasHiddenPropertyNode == null || !hiddenKey.equals(hasHiddenPropertyNode.getKey())) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                hasHiddenPropertyNode = insert(HasHiddenKeyCacheNode.create(hiddenKey));
            }
            return hasHiddenPropertyNode.executeHasHiddenKey(target);
        }

        @Override
        protected JavaScriptNode copyUninitialized() {
            return new InternalHasHiddenKeyNode(cloneUninitialized(targetNode), cloneUninitialized(keyNode));

        }
    }

    public abstract static class InternalStringReplaceNode extends JavaScriptNode {
        @Child @Executed JavaScriptNode valueNode;
        @Child @Executed JavaScriptNode searchNode;
        @Child @Executed JavaScriptNode replacementNode;

        protected InternalStringReplaceNode(JavaScriptNode valueNode, JavaScriptNode searchNode, JavaScriptNode replacementNode) {
            this.valueNode = valueNode;
            this.searchNode = searchNode;
            this.replacementNode = replacementNode;
        }

        public static InternalStringReplaceNode create(JavaScriptNode valueNode, JavaScriptNode searchNode, JavaScriptNode replacementNode) {
            return InternalStringReplaceNodeGen.create(valueNode, searchNode, replacementNode);
        }

        @Specialization
        protected String stringReplace(Object value, Object search, Object replacement,
                        @Cached("create()") JSToStringNode toString) {
            String valueStr = toString.executeString(value);
            String searchStr = toString.executeString(search);
            String replacementStr = toString.executeString(replacement);
            return doStringReplace(valueStr, searchStr, replacementStr);
        }

        @TruffleBoundary
        private static String doStringReplace(String valueStr, String searchStr, String replacementStr) {
            /* String.replace is complex and not suitable for partial evaluation. */
            return valueStr.replace(searchStr, replacementStr);
        }

        @Override
        protected JavaScriptNode copyUninitialized() {
            return create(cloneUninitialized(valueNode), cloneUninitialized(searchNode), cloneUninitialized(replacementNode));
        }
    }

    /**
     * ES2015, 22.2.3.5.1 ValidateTypedArray.
     */
    public static class InternalIsValidTypedArrayNode extends JavaScriptNode {
        @Child private JavaScriptNode arrayNode;

        InternalIsValidTypedArrayNode(JavaScriptNode node) {
            this.arrayNode = node;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            Object target = arrayNode.execute(frame);
            return JSArrayBufferView.isJSArrayBufferView(target) && !JSArrayBuffer.isDetachedBuffer(JSArrayBufferView.getArrayBuffer((DynamicObject) target));
        }

        @Override
        protected JavaScriptNode copyUninitialized() {
            return new InternalIsValidTypedArrayNode(cloneUninitialized(arrayNode));
        }
    }

    public static class InternalHasDetachedBufferNode extends JavaScriptNode {
        @Child private JavaScriptNode arrayNode;

        InternalHasDetachedBufferNode(JavaScriptNode node) {
            this.arrayNode = node;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            Object target = arrayNode.execute(frame);
            return JSArrayBuffer.isDetachedBuffer(JSArrayBufferView.getArrayBuffer((DynamicObject) target));
        }

        @Override
        protected JavaScriptNode copyUninitialized() {
            return new InternalHasDetachedBufferNode(cloneUninitialized(arrayNode));
        }
    }

    public static class RevokeProxyNode extends JavaScriptNode {
        @Child private JavaScriptNode proxyNode;

        RevokeProxyNode(JavaScriptNode proxyNode) {
            this.proxyNode = proxyNode;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            try {
                JSProxy.revoke(proxyNode.executeDynamicObject(frame));
                return Undefined.instance;
            } catch (UnexpectedResultException e) {
                throw Errors.shouldNotReachHere();
            }
        }

        @Override
        protected JavaScriptNode copyUninitialized() {
            return new RevokeProxyNode(cloneUninitialized(proxyNode));
        }
    }

    public static class PromiseRejectionTrackerNode extends JavaScriptNode {

        @Child private JavaScriptNode promiseNode;
        @Child private JavaScriptNode operationNode;

        private final JSContext context;

        PromiseRejectionTrackerNode(JSContext context, JavaScriptNode promiseNode, JavaScriptNode operationNode) {
            this.context = context;
            this.promiseNode = promiseNode;
            this.operationNode = operationNode;
        }

        public static JavaScriptNode create(JSContext context, JavaScriptNode promiseNode, JavaScriptNode operationNode) {
            return new PromiseRejectionTrackerNode(context, promiseNode, operationNode);
        }

        @Override
        public Object execute(VirtualFrame frame) {
            try {
                DynamicObject promise = promiseNode.executeDynamicObject(frame);
                int operation = operationNode.executeInt(frame);
                context.notifyPromiseRejectionTracker(promise, operation);
            } catch (UnexpectedResultException ex) {
                throw new AssertionError();
            }
            return Undefined.instance;
        }

        @Override
        protected JavaScriptNode copyUninitialized() {
            return new PromiseRejectionTrackerNode(context, cloneUninitialized(promiseNode), cloneUninitialized(operationNode));
        }
    }

    public static class PromiseHookNode extends JavaScriptNode {

        @Child private JavaScriptNode changeTypeNode;
        @Child private JavaScriptNode promiseNode;

        private final JSContext context;

        PromiseHookNode(JSContext context, JavaScriptNode typeNode, JavaScriptNode promiseNode) {
            this.context = context;
            this.changeTypeNode = typeNode;
            this.promiseNode = promiseNode;
        }

        public static JavaScriptNode create(JSContext context, JavaScriptNode typeNode, JavaScriptNode promiseNode) {
            return new PromiseHookNode(context, typeNode, promiseNode);
        }

        @Override
        public Object execute(VirtualFrame frame) {
            try {
                int changeType = changeTypeNode.executeInt(frame);
                DynamicObject promise = promiseNode.executeDynamicObject(frame);
                context.notifyPromiseHook(changeType, promise);
            } catch (UnexpectedResultException ex) {
                throw new AssertionError();
            }
            return Undefined.instance;
        }

        @Override
        protected JavaScriptNode copyUninitialized() {
            return new PromiseHookNode(context, cloneUninitialized(changeTypeNode), cloneUninitialized(promiseNode));
        }
    }
}
