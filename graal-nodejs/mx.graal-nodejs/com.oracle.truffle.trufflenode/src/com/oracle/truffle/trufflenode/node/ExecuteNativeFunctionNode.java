/*
 * Copyright (c) 2018, 2023, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.trufflenode.node;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.access.PropertySetNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSFrameUtil;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JavaScriptRootNode;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.trufflenode.GraalJSAccess;
import com.oracle.truffle.trufflenode.NativeAccess;
import com.oracle.truffle.trufflenode.info.FunctionTemplate;
import com.oracle.truffle.trufflenode.info.ObjectTemplate;

public class ExecuteNativeFunctionNode extends JavaScriptNode {

    private final JSContext context;

    private final boolean isNew;
    private final boolean isNewTarget;
    private final BranchProfile errorBranch = BranchProfile.create();
    private final ConditionProfile isTemplate = ConditionProfile.create();
    private final ConditionProfile eightOrLessArgs = ConditionProfile.create();
    private final ConditionProfile argumentLengthTwo = ConditionProfile.create();

    private static final int IMPLICIT_ARG_COUNT = 2;
    private static final int EXPLICIT_ARG_COUNT = 6;
    @Children private final ValueTypeNode[] valueTypeNodes;
    @Children private final FlattenNode[] flattenNodes;

    private static final boolean USE_TEMPLATE_NODES = true;
    @Child private PropertyGetNode getFunctionTemplateNode;
    @Child private PropertySetNode setConstructorTemplateNode;
    @Child private PropertyGetNode getConstructorTemplateNode;
    @Child private ObjectTemplateNode instanceTemplateNode;

    ExecuteNativeFunctionNode(JSContext context, boolean isNew, boolean isNewTarget) {
        super(createSourceSection());
        this.context = context;
        this.isNew = isNew;
        this.isNewTarget = isNewTarget;
        this.valueTypeNodes = new ValueTypeNode[IMPLICIT_ARG_COUNT + EXPLICIT_ARG_COUNT];
        this.flattenNodes = new FlattenNode[EXPLICIT_ARG_COUNT];
        this.getFunctionTemplateNode = PropertyGetNode.createGetHidden(GraalJSAccess.FUNCTION_TEMPLATE_KEY, context);
    }

    // Truffle profiler merges identical source sections
    // => including a counter to differentiate them
    private static int sourceSectionCounter;

    private static SourceSection createSourceSection() {
        return Source.newBuilder(JavaScriptLanguage.ID, "", "<native$" + ++sourceSectionCounter + ">").build().createUnavailableSection();
    }

    @Override
    public boolean hasTag(Class<? extends Tag> tag) {
        if (tag == StandardTags.RootTag.class) {
            return true;
        } else if (tag == StandardTags.RootBodyTag.class) {
            return true;
        }
        return false;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        JSDynamicObject functionObject = JSFrameUtil.getFunctionObject(frame);
        FunctionTemplate functionTemplate = (FunctionTemplate) getFunctionTemplateNode.getValue(functionObject);

        JSRealm realm = JSFunction.getRealm(functionObject);
        assert functionTemplate != null;
        GraalJSAccess graalAccess = GraalJSAccess.get(this);

        int templateId = functionTemplate.getId();
        FunctionTemplate signature = functionTemplate.getSignature();
        long functionPointer = functionTemplate.getFunctionPointer();

        Object[] arguments = frame.getArguments();
        Object thisObject = JSArguments.getThisObject(arguments);

        if (isNew) {
            ObjectTemplate instanceTemplate = functionTemplate.getInstanceTemplate();
            boolean hasPropertyHandler = instanceTemplate.hasPropertyHandler();
            JSObject thisJSObject = (JSObject) thisObject;
            objectTemplateInstantiate(frame, thisJSObject, realm, instanceTemplate, graalAccess);
            if (hasPropertyHandler) {
                thisObject = graalAccess.propertyHandlerInstantiate(context, realm, instanceTemplate, thisJSObject, false);
            }
            setConstructorTemplate(thisJSObject, functionTemplate);
        } else if (signature != null) {
            checkConstructorTemplate(thisObject, signature);
        }
        Object result;
        int offset = isNewTarget ? 1 : 0;
        Object newTarget = isNew ? (isNewTarget ? JSArguments.getNewTarget(arguments) : JSArguments.getFunctionObject(arguments)) : null;
        if (isTemplate.profile(functionPointer == 0)) {
            result = thisObject;
        } else if (eightOrLessArgs.profile(arguments.length <= IMPLICIT_ARG_COUNT + EXPLICIT_ARG_COUNT + offset)) {
            int thisType = getValueType(0, thisObject);
            Object calleeObject = arguments[1];
            if (argumentLengthTwo.profile(arguments.length == 2 + offset)) {
                result = executeFunction0(templateId, thisObject, thisType, calleeObject, newTarget, realm);
            } else {
                graalAccess.resetSharedBuffer();
                Object argument1 = flatten(0, arguments[2 + offset]);
                int argument1Type = getValueType(2, argument1);
                if (arguments.length == 3 + offset) {
                    result = executeFunction1(templateId, thisObject, thisType, calleeObject, newTarget,
                                    argument1, argument1Type,
                                    realm);
                } else {
                    Object argument2 = flatten(1, arguments[3 + offset]);
                    int argument2Type = getValueType(3, argument2);
                    if (arguments.length == 4 + offset) {
                        result = executeFunction2(templateId, thisObject, thisType, calleeObject, newTarget,
                                        argument1, argument1Type,
                                        argument2, argument2Type,
                                        realm);
                    } else {
                        Object argument3 = flatten(2, arguments[4 + offset]);
                        int argument3Type = getValueType(4, argument3);
                        if (arguments.length == 5 + offset) {
                            result = executeFunction3(templateId, thisObject, thisType, calleeObject, newTarget,
                                            argument1, argument1Type,
                                            argument2, argument2Type,
                                            argument3, argument3Type,
                                            realm);
                        } else {
                            Object argument4 = flatten(3, arguments[5 + offset]);
                            int argument4Type = getValueType(5, argument4);
                            if (arguments.length == 6 + offset) {
                                result = executeFunction4(templateId, thisObject, thisType, calleeObject, newTarget,
                                                argument1, argument1Type,
                                                argument2, argument2Type,
                                                argument3, argument3Type,
                                                argument4, argument4Type,
                                                realm);
                            } else {
                                Object argument5 = flatten(4, arguments[6 + offset]);
                                int argument5Type = getValueType(6, argument5);
                                if (arguments.length == 7 + offset) {
                                    result = executeFunction5(templateId, thisObject, thisType, calleeObject, newTarget,
                                                    argument1, argument1Type,
                                                    argument2, argument2Type,
                                                    argument3, argument3Type,
                                                    argument4, argument4Type,
                                                    argument5, argument5Type,
                                                    realm);
                                } else {
                                    assert arguments.length == IMPLICIT_ARG_COUNT + EXPLICIT_ARG_COUNT + offset;
                                    Object argument6 = flatten(5, arguments[7 + offset]);
                                    int argument6Type = getValueType(7, argument6);
                                    result = executeFunction6(templateId, thisObject, thisType, calleeObject, newTarget,
                                                    argument1, argument1Type,
                                                    argument2, argument2Type,
                                                    argument3, argument3Type,
                                                    argument4, argument4Type,
                                                    argument5, argument5Type,
                                                    argument6, argument6Type,
                                                    realm);
                                }
                            }
                        }
                    }
                }
            }
        } else {
            result = executeFunction(templateId, arguments, realm);
        }
        return graalAccess.correctReturnValue(result);
    }

    private void objectTemplateInstantiate(VirtualFrame frame, JSObject thisObject, JSRealm realm, ObjectTemplate instanceTemplate, GraalJSAccess graalAccess) {
        if (USE_TEMPLATE_NODES && !context.isMultiContext()) {
            if (instanceTemplateNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                instanceTemplateNode = insert(ObjectTemplateNode.fromObjectTemplate(instanceTemplate, context, graalAccess, realm));
            }
            instanceTemplateNode.executeWithObject(frame, thisObject, realm);
        } else {
            // when aux engine cache is enabled, don't specialize on ObjectTemplate instances.
            graalAccess.objectTemplateInstantiate(realm, instanceTemplate, thisObject);
        }
    }

    private void setConstructorTemplate(JSObject thisObject, FunctionTemplate functionTemplate) {
        if (USE_TEMPLATE_NODES) {
            if (setConstructorTemplateNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                setConstructorTemplateNode = insert(PropertySetNode.createSetHidden(FunctionTemplate.CONSTRUCTOR, context));
            }
            setConstructorTemplateNode.setValue(thisObject, functionTemplate);
        } else {
            JSObjectUtil.putHiddenProperty(thisObject, FunctionTemplate.CONSTRUCTOR, functionTemplate);
        }
    }

    private Object getConstructorTemplate(JSObject thisObject) {
        if (USE_TEMPLATE_NODES) {
            if (getConstructorTemplateNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getConstructorTemplateNode = insert(PropertyGetNode.createGetHidden(FunctionTemplate.CONSTRUCTOR, context));
            }
            Object result = getConstructorTemplateNode.getValue(thisObject);
            return result == Undefined.instance ? null : result;
        } else {
            return JSObjectUtil.getHiddenProperty(thisObject, FunctionTemplate.CONSTRUCTOR);
        }
    }

    private void checkConstructorTemplate(Object thisObject, FunctionTemplate signature) {
        FunctionTemplate constructorTemplate = thisObject instanceof JSObject ? (FunctionTemplate) getConstructorTemplate((JSObject) thisObject) : null;
        while (constructorTemplate != signature && constructorTemplate != null) {
            constructorTemplate = constructorTemplate.getParent();
        }
        if (constructorTemplate == null) {
            errorBranch.enter();
            illegalInvocation();
        }
    }

    private static void illegalInvocation() {
        throw Errors.createTypeError("Illegal invocation");
    }

    private int getValueType(int index, Object argument) {
        if (valueTypeNodes[index] == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            valueTypeNodes[index] = insert(ValueTypeNodeGen.create(context, index >= IMPLICIT_ARG_COUNT));
        }
        int type = valueTypeNodes[index].executeInt(argument);
        assert type == GraalJSAccess.get(this).valueType(argument, false);
        return type;
    }

    private Object flatten(int index, Object argument) {
        if (flattenNodes[index] == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            flattenNodes[index] = insert(FlattenNodeGen.create());
        }
        return flattenNodes[index].execute(argument);
    }

    @CompilerDirectives.TruffleBoundary
    private Object executeFunction(int templateId, Object[] arguments, JSRealm realm) {
        return NativeAccess.executeFunction(templateId, arguments, isNew, isNewTarget, realm);
    }

    @CompilerDirectives.TruffleBoundary
    private static Object executeFunction0(int templateId, Object thisObject, int thisType, @SuppressWarnings("unused") Object calleeObject, Object newTarget, JSRealm realm) {
        return NativeAccess.executeFunction0(templateId, thisObject, thisType, newTarget, realm);
    }

    @CompilerDirectives.TruffleBoundary
    private static Object executeFunction1(int templateId, Object thisObject, int thisType, @SuppressWarnings("unused") Object calleeObject, Object newTarget,
                    Object argument, int argumentType,
                    JSRealm realm) {
        return NativeAccess.executeFunction1(templateId, thisObject, thisType, newTarget, argument, argumentType, realm);
    }

    @CompilerDirectives.TruffleBoundary
    private static Object executeFunction2(int templateId, Object thisObject, int thisType, @SuppressWarnings("unused") Object calleeObject, Object newTarget,
                    Object argument1, int argument1Type,
                    Object argument2, int argument2Type,
                    JSRealm realm) {
        return NativeAccess.executeFunction2(templateId, thisObject, thisType, newTarget, argument1, argument1Type, argument2, argument2Type, realm);
    }

    @CompilerDirectives.TruffleBoundary
    private static Object executeFunction3(int templateId, Object thisObject, int thisType, @SuppressWarnings("unused") Object calleeObject, Object newTarget,
                    Object argument1, int argument1Type,
                    Object argument2, int argument2Type,
                    Object argument3, int argument3Type,
                    JSRealm realm) {
        return NativeAccess.executeFunction3(templateId, thisObject, thisType, newTarget, argument1, argument1Type, argument2, argument2Type, argument3, argument3Type, realm);
    }

    @CompilerDirectives.TruffleBoundary
    private static Object executeFunction4(int templateId, Object thisObject, int thisType, @SuppressWarnings("unused") Object calleeObject, Object newTarget,
                    Object argument1, int argument1Type,
                    Object argument2, int argument2Type,
                    Object argument3, int argument3Type,
                    Object argument4, int argument4Type,
                    JSRealm realm) {
        return NativeAccess.executeFunction4(templateId, thisObject, thisType, newTarget, argument1, argument1Type, argument2, argument2Type, argument3, argument3Type, argument4, argument4Type,
                        realm);
    }

    @CompilerDirectives.TruffleBoundary
    private static Object executeFunction5(int templateId, Object thisObject, int thisType, @SuppressWarnings("unused") Object calleeObject, Object newTarget,
                    Object argument1, int argument1Type,
                    Object argument2, int argument2Type,
                    Object argument3, int argument3Type,
                    Object argument4, int argument4Type,
                    Object argument5, int argument5Type, JSRealm realm) {
        return NativeAccess.executeFunction5(templateId, thisObject, thisType, newTarget, argument1, argument1Type, argument2, argument2Type, argument3, argument3Type, argument4, argument4Type,
                        argument5, argument5Type, realm);
    }

    @CompilerDirectives.TruffleBoundary
    private static Object executeFunction6(int templateId, Object thisObject, int thisType, @SuppressWarnings("unused") Object calleeObject, Object newTarget,
                    Object argument1, int argument1Type,
                    Object argument2, int argument2Type,
                    Object argument3, int argument3Type,
                    Object argument4, int argument4Type,
                    Object argument5, int argument5Type,
                    Object argument6, int argument6Type,
                    JSRealm realm) {
        return NativeAccess.executeFunction6(templateId, thisObject, thisType, newTarget, argument1, argument1Type, argument2, argument2Type, argument3, argument3Type, argument4, argument4Type,
                        argument5, argument5Type, argument6, argument6Type, realm);
    }

    public static class NativeFunctionRootNode extends JavaScriptRootNode {
        @Child private JavaScriptNode node;
        private final JSContext context;
        private final boolean isNew;
        private final boolean isNewTarget;
        private final TruffleString name;

        public NativeFunctionRootNode(JSContext context, FunctionTemplate template, boolean isNew, boolean isNewTarget) {
            super(null, createSourceSection(), null);
            this.context = context;
            this.isNew = isNew;
            this.isNewTarget = isNewTarget;
            this.name = template.getClassName();
        }

        @Override
        public Object execute(VirtualFrame frame) {
            if (node == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                node = insert(new ExecuteNativeFunctionNode(context, isNew, isNewTarget));
            }
            return node.execute(frame);
        }

        @Override
        public String getName() {
            return Strings.toJavaString(name);
        }

        @Override
        public String toString() {
            return "NativeFunction" + (name != null ? "[" + name + "]" : "");
        }

        @Override
        public boolean isFunction() {
            return true;
        }

    }

}
