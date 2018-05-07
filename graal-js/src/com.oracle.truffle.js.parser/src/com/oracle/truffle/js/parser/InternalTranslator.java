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
package com.oracle.truffle.js.parser;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Executed;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.object.Property;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.js.builtins.ObjectFunctionBuiltinsFactory.ObjectDefinePropertyNodeGen;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.NodeFactory;
import com.oracle.truffle.js.nodes.ScriptNode;
import com.oracle.truffle.js.nodes.access.GetIteratorNode;
import com.oracle.truffle.js.nodes.access.GlobalConstantNode;
import com.oracle.truffle.js.nodes.access.GlobalObjectNode;
import com.oracle.truffle.js.nodes.access.HasHiddenKeyCacheNode;
import com.oracle.truffle.js.nodes.access.IsArrayNode.IsArrayWrappedNode;
import com.oracle.truffle.js.nodes.access.IsJSClassNode;
import com.oracle.truffle.js.nodes.access.IsObjectNode.IsObjectWrappedNode;
import com.oracle.truffle.js.nodes.access.JSConstantNode;
import com.oracle.truffle.js.nodes.access.JSGetLengthNode;
import com.oracle.truffle.js.nodes.access.JSTargetableNode;
import com.oracle.truffle.js.nodes.access.OrdinaryCreateFromConstructorNode;
import com.oracle.truffle.js.nodes.access.RequireObjectNode;
import com.oracle.truffle.js.nodes.access.WriteElementNode;
import com.oracle.truffle.js.nodes.cast.JSEnqueueJobNode;
import com.oracle.truffle.js.nodes.cast.JSIsConstructorFunctionNode;
import com.oracle.truffle.js.nodes.cast.JSToBooleanNode;
import com.oracle.truffle.js.nodes.cast.JSToInt32Node;
import com.oracle.truffle.js.nodes.cast.JSToNumberNode.JSToNumberWrapperNode;
import com.oracle.truffle.js.nodes.cast.JSToObjectNode.JSToObjectWrapperNode;
import com.oracle.truffle.js.nodes.cast.JSToPropertyKeyNode.JSToPropertyKeyWrapperNode;
import com.oracle.truffle.js.nodes.cast.JSToStringNode.JSToStringWrapperNode;
import com.oracle.truffle.js.nodes.cast.JSToUInt32Node.JSToUInt32WrapperNode;
import com.oracle.truffle.js.nodes.control.StatementNode;
import com.oracle.truffle.js.nodes.function.CreateMethodPropertyNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.nodes.unary.IsCallableNode;
import com.oracle.truffle.js.parser.InternalTranslatorFactory.InternalArrayPushNodeGen;
import com.oracle.truffle.js.parser.env.Environment;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSErrorType;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.builtins.JSArray;
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
                case "String":
                    return realm.getStringConstructor().getFunctionObject();
                case "Symbol":
                    return realm.getSymbolConstructor() == null ? null : realm.getSymbolConstructor().getFunctionObject();
                case "Promise":
                    return realm.getPromiseConstructor();
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
                    case "GetGlobalObject":
                        return GlobalObjectNode.create(context);
                    case "NextTick":
                        return JSEnqueueJobNode.create(context, arguments[0]);
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
                    case "SetFunctionName":
                        return new InternalSetFunctionNameNode(arguments[0], arguments[1]);
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
                        return InternalArrayPushNode.create(context, arguments[0], arguments[1]);
                    case "GetIterator":
                        return GetIteratorNode.create(context, arguments[0]);
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

    abstract static class InternalArrayPushNode extends JavaScriptNode {
        @Child @Executed JavaScriptNode arrayNode;
        @Child @Executed JavaScriptNode valueNode;
        @Child private WriteElementNode writeElementNode;
        @Child private JSGetLengthNode getLengthNode;
        private final JSContext context;

        protected InternalArrayPushNode(JSContext context, JavaScriptNode arrayNode, JavaScriptNode valueNode) {
            this.arrayNode = arrayNode;
            this.valueNode = valueNode;
            this.context = context;
        }

        static InternalArrayPushNode create(JSContext context, JavaScriptNode arrayNode, JavaScriptNode valueNode) {
            return InternalArrayPushNodeGen.create(context, arrayNode, valueNode);
        }

        @Specialization
        protected int doPush(Object array, Object value) {
            assert JSArray.isJSArray(array);
            DynamicObject arrayObject = (DynamicObject) array;
            if (getLengthNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getLengthNode = insert(JSGetLengthNode.create(context));
            }
            if (writeElementNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                writeElementNode = insert(WriteElementNode.create(context, true, true));
            }
            int len = (int) getLengthNode.executeLong(arrayObject);
            writeElementNode.executeWithTargetAndIndexAndValue(arrayObject, len, value);
            return len + 1;
        }

        @Override
        protected JavaScriptNode copyUninitialized() {
            return create(context, cloneUninitialized(arrayNode), cloneUninitialized(valueNode));
        }
    }
}
