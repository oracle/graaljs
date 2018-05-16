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
package com.oracle.truffle.trufflenode.node;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.GetPrototypeNode;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.access.PropertySetNode;
import com.oracle.truffle.js.runtime.AbstractJavaScriptLanguage;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.JavaScriptRootNode;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.trufflenode.GraalJSAccess;
import com.oracle.truffle.trufflenode.NativeAccess;
import com.oracle.truffle.trufflenode.info.FunctionTemplate;
import com.oracle.truffle.trufflenode.info.ObjectTemplate;

public class ExecuteNativeFunctionNode extends JavaScriptNode {

    private final GraalJSAccess graalAccess;
    private final JSContext context;
    private final FunctionTemplate functionTemplate;
    private final FunctionTemplate signature;
    private final ObjectTemplate instanceTemplate;
    private final boolean hasPropertyHandler;
    private final boolean isNew;
    private final boolean isNewTarget;
    private final BranchProfile errorBranch = BranchProfile.create();
    private final ConditionProfile isTemplate = ConditionProfile.createBinaryProfile();
    private final ConditionProfile eightOrLessArgs = ConditionProfile.createBinaryProfile();
    private final ConditionProfile argumentLengthTwo = ConditionProfile.createBinaryProfile();
    private final int templateID;
    private final long functionPointer;

    private static final int IMPLICIT_ARG_COUNT = 2;
    private static final int EXPLICIT_ARG_COUNT = 6;
    @Children private final ValueTypeNode[] valueTypeNodes;
    @Children private final FlattenNode[] flattenNodes;
    @Child private GetPrototypeNode getPrototypeNode;
    @Child private PropertyGetNode prototypePropertyGetNode;

    private static final boolean USE_TEMPLATE_NODES = true;
    @Child private ObjectTemplateNode instanceTemplateNode;
    @Child private PropertySetNode setConstructorTemplateNode;
    @Child private PropertyGetNode getConstructorTemplateNode;

    ExecuteNativeFunctionNode(GraalJSAccess graalAccess, JSContext context, FunctionTemplate template, boolean isNew, boolean isNewTarget) {
        super(createSourceSection());
        this.graalAccess = graalAccess;
        this.context = context;
        this.functionTemplate = template;
        this.signature = template.getSignature();
        this.instanceTemplate = template.getInstanceTemplate();
        this.hasPropertyHandler = instanceTemplate.hasPropertyHandler();
        this.isNew = isNew;
        this.isNewTarget = isNewTarget;
        this.templateID = template.getID();
        this.functionPointer = template.getFunctionPointer();
        this.getPrototypeNode = GetPrototypeNode.create();
        this.prototypePropertyGetNode = PropertyGetNode.create(JSObject.PROTOTYPE, false, context);

        this.valueTypeNodes = new ValueTypeNode[IMPLICIT_ARG_COUNT + EXPLICIT_ARG_COUNT];
        this.flattenNodes = new FlattenNode[EXPLICIT_ARG_COUNT];
    }

    // Truffle profiler merges identical source sections
    // => including a counter to differentiate them
    private static int sourceSectionCounter;

    private static SourceSection createSourceSection() {
        return Source.newBuilder("").name("<native$" + ++sourceSectionCounter + ">").language(AbstractJavaScriptLanguage.ID).build().createUnavailableSection();
    }

    @Override
    public boolean hasTag(Class<? extends Tag> tag) {
        return (tag == StandardTags.RootTag.class);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        Object[] arguments = frame.getArguments();
        DynamicObject thisObject = (DynamicObject) arguments[0];
        JSRealm realm = context.getRealm();
        if (isNew) {
            objectTemplateInstantiate(frame, thisObject, realm);
            if (hasPropertyHandler) {
                thisObject = graalAccess.propertyHandlerInstantiate(context, realm, instanceTemplate, thisObject, false);
            }
            setConstructorTemplate(thisObject);
        } else if (signature != null) {
            checkConstructorTemplate(thisObject, realm);
        }
        Object result;
        int offset = isNewTarget ? 1 : 0;
        Object newTarget = isNew ? (isNewTarget ? arguments[2] : arguments[1]) : null;
        if (isTemplate.profile(functionPointer == 0)) {
            result = thisObject;
        } else if (eightOrLessArgs.profile(arguments.length <= IMPLICIT_ARG_COUNT + EXPLICIT_ARG_COUNT + offset)) {
            int thisType = getValueType(0, thisObject);
            Object calleeObject = arguments[1];
            if (argumentLengthTwo.profile(arguments.length == 2 + offset)) {
                result = executeFunction0(thisObject, thisType, calleeObject, newTarget, realm);
            } else {
                graalAccess.resetSharedBuffer();
                Object argument1 = flatten(0, arguments[2 + offset]);
                int argument1Type = getValueType(2, argument1);
                if (arguments.length == 3 + offset) {
                    result = executeFunction1(thisObject, thisType, calleeObject, newTarget, argument1, argument1Type, realm);
                } else {
                    Object argument2 = flatten(1, arguments[3 + offset]);
                    int argument2Type = getValueType(3, argument2);
                    if (arguments.length == 4 + offset) {
                        result = executeFunction2(thisObject, thisType, calleeObject, newTarget, argument1, argument1Type, argument2, argument2Type, realm);
                    } else {
                        Object argument3 = flatten(2, arguments[4 + offset]);
                        int argument3Type = getValueType(4, argument3);
                        if (arguments.length == 5 + offset) {
                            result = executeFunction3(thisObject, thisType, calleeObject, newTarget, argument1, argument1Type, argument2, argument2Type, argument3, argument3Type, realm);
                        } else {
                            Object argument4 = flatten(3, arguments[5 + offset]);
                            int argument4Type = getValueType(5, argument4);
                            if (arguments.length == 6 + offset) {
                                result = executeFunction4(thisObject, thisType, calleeObject, newTarget, argument1, argument1Type, argument2, argument2Type, argument3, argument3Type, argument4,
                                                argument4Type, realm);
                            } else {
                                Object argument5 = flatten(4, arguments[6 + offset]);
                                int argument5Type = getValueType(6, argument5);
                                if (arguments.length == 7 + offset) {
                                    result = executeFunction5(thisObject, thisType, calleeObject, newTarget, argument1, argument1Type, argument2, argument2Type, argument3, argument3Type, argument4,
                                                    argument4Type, argument5, argument5Type, realm);
                                } else {
                                    assert arguments.length == IMPLICIT_ARG_COUNT + EXPLICIT_ARG_COUNT + offset;
                                    Object argument6 = flatten(5, arguments[7 + offset]);
                                    int argument6Type = getValueType(7, argument6);
                                    result = executeFunction6(thisObject, thisType, calleeObject, newTarget, argument1, argument1Type, argument2, argument2Type, argument3, argument3Type, argument4,
                                                    argument4Type, argument5, argument5Type, argument6, argument6Type, realm);
                                }
                            }
                        }
                    }
                }
            }
        } else {
            result = executeFunction(arguments, realm);
        }
        return graalAccess.correctReturnValue(result);
    }

    private void objectTemplateInstantiate(VirtualFrame frame, DynamicObject thisObject, JSRealm realm) {
        if (USE_TEMPLATE_NODES) {
            if (instanceTemplateNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                instanceTemplateNode = insert(ObjectTemplateNode.fromObjectTemplate(instanceTemplate, context, graalAccess));
            }
            instanceTemplateNode.executeWithObject(frame, thisObject);
        } else {
            graalAccess.objectTemplateInstantiate(realm, instanceTemplate, thisObject);
        }
    }

    private void setConstructorTemplate(DynamicObject thisObject) {
        if (USE_TEMPLATE_NODES) {
            if (setConstructorTemplateNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                setConstructorTemplateNode = insert(PropertySetNode.createSetHidden(FunctionTemplate.CONSTRUCTOR, context));
            }
            setConstructorTemplateNode.setValue(thisObject, functionTemplate);
        } else {
            thisObject.define(FunctionTemplate.CONSTRUCTOR, functionTemplate);
        }
    }

    private Object getConstructorTemplate(DynamicObject thisObject) {
        if (USE_TEMPLATE_NODES) {
            if (getConstructorTemplateNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getConstructorTemplateNode = insert(PropertyGetNode.createGetHidden(FunctionTemplate.CONSTRUCTOR, context));
            }
            Object result = getConstructorTemplateNode.getValue(thisObject);
            return result == Undefined.instance ? null : result;
        } else {
            return thisObject.get(FunctionTemplate.CONSTRUCTOR);
        }
    }

    private void checkConstructorTemplate(DynamicObject thisObject, JSRealm realm) {
        Object constructorTemplate = getConstructorTemplate(thisObject);
        if (constructorTemplate == null) {
            errorBranch.enter();
            illegalInvocation();
        }
        if (constructorTemplate != signature) { // checking the most common case
            DynamicObject signatureFunction = (DynamicObject) graalAccess.functionTemplateGetFunction(realm, signature);
            DynamicObject signaturePrototype = (DynamicObject) prototypePropertyGetNode.getValue(signatureFunction);
            if (!JSRuntime.isPrototypeOf(thisObject, signaturePrototype)) {
                errorBranch.enter();
                illegalInvocation();
            }
        }
    }

    private static void illegalInvocation() {
        throw Errors.createTypeError("Illegal invocation");
    }

    private int getValueType(int index, Object argument) {
        if (valueTypeNodes[index] == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            valueTypeNodes[index] = ValueTypeNodeGen.create(graalAccess, context, index >= IMPLICIT_ARG_COUNT);
        }
        int type = valueTypeNodes[index].executeInt(argument);
        assert type == graalAccess.valueType(argument, false);
        return type;
    }

    private Object flatten(int index, Object argument) {
        if (flattenNodes[index] == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            flattenNodes[index] = FlattenNodeGen.create();
        }
        return flattenNodes[index].execute(argument);
    }

    @CompilerDirectives.TruffleBoundary
    private Object executeFunction(Object[] arguments, JSRealm realm) {
        return NativeAccess.executeFunction(templateID, arguments, isNew, isNewTarget, realm);
    }

    @CompilerDirectives.TruffleBoundary
    private Object executeFunction0(Object thisObject, int thisType, @SuppressWarnings("unused") Object calleeObject, Object newTarget, JSRealm realm) {
        return NativeAccess.executeFunction0(templateID, thisObject, thisType, newTarget, realm);
    }

    @CompilerDirectives.TruffleBoundary
    private Object executeFunction1(Object thisObject, int thisType, @SuppressWarnings("unused") Object calleeObject, Object newTarget, Object argument, int argumentType, JSRealm realm) {
        return NativeAccess.executeFunction1(templateID, thisObject, thisType, newTarget, argument, argumentType, realm);
    }

    @CompilerDirectives.TruffleBoundary
    private Object executeFunction2(Object thisObject, int thisType, @SuppressWarnings("unused") Object calleeObject, Object newTarget, Object argument1, int argument1Type, Object argument2,
                    int argument2Type, JSRealm realm) {
        return NativeAccess.executeFunction2(templateID, thisObject, thisType, newTarget, argument1, argument1Type, argument2, argument2Type, realm);
    }

    @CompilerDirectives.TruffleBoundary
    private Object executeFunction3(Object thisObject, int thisType, @SuppressWarnings("unused") Object calleeObject, Object newTarget, Object argument1, int argument1Type, Object argument2,
                    int argument2Type, Object argument3, int argument3Type, JSRealm realm) {
        return NativeAccess.executeFunction3(templateID, thisObject, thisType, newTarget, argument1, argument1Type, argument2, argument2Type, argument3, argument3Type, realm);
    }

    @CompilerDirectives.TruffleBoundary
    private Object executeFunction4(Object thisObject, int thisType, @SuppressWarnings("unused") Object calleeObject, Object newTarget, Object argument1, int argument1Type, Object argument2,
                    int argument2Type, Object argument3, int argument3Type, Object argument4, int argument4Type, JSRealm realm) {
        return NativeAccess.executeFunction4(templateID, thisObject, thisType, newTarget, argument1, argument1Type, argument2, argument2Type, argument3, argument3Type, argument4, argument4Type,
                        realm);
    }

    @CompilerDirectives.TruffleBoundary
    private Object executeFunction5(Object thisObject, int thisType, @SuppressWarnings("unused") Object calleeObject, Object newTarget, Object argument1, int argument1Type, Object argument2,
                    int argument2Type, Object argument3, int argument3Type, Object argument4, int argument4Type, Object argument5, int argument5Type, JSRealm realm) {
        return NativeAccess.executeFunction5(templateID, thisObject, thisType, newTarget, argument1, argument1Type, argument2, argument2Type, argument3, argument3Type, argument4, argument4Type,
                        argument5, argument5Type, realm);
    }

    @CompilerDirectives.TruffleBoundary
    private Object executeFunction6(Object thisObject, int thisType, @SuppressWarnings("unused") Object calleeObject, Object newTarget, Object argument1, int argument1Type, Object argument2,
                    int argument2Type, Object argument3, int argument3Type, Object argument4, int argument4Type, Object argument5, int argument5Type, Object argument6, int argument6Type,
                    JSRealm realm) {
        return NativeAccess.executeFunction6(templateID, thisObject, thisType, newTarget, argument1, argument1Type, argument2, argument2Type, argument3, argument3Type, argument4, argument4Type,
                        argument5, argument5Type, argument6, argument6Type, realm);
    }

    public static class NativeFunctionRootNode extends JavaScriptRootNode {
        @Child private JavaScriptNode node;
        private final GraalJSAccess graalAccess;
        private final JSContext context;
        private final FunctionTemplate template;
        private final boolean isNew;
        private final boolean isNewTarget;

        public NativeFunctionRootNode(GraalJSAccess graalAccess, JSContext context, FunctionTemplate template, boolean isNew, boolean isNewTarget) {
            this.graalAccess = graalAccess;
            this.template = template;
            this.context = context;
            this.isNew = isNew;
            this.isNewTarget = isNewTarget;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            if (node == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                node = insert(new ExecuteNativeFunctionNode(graalAccess, context, template, isNew, isNewTarget));
            }
            return node.execute(frame);
        }

        @Override
        public String getName() {
            return JSFunction.getFunctionData(template.getFunctionObject()).getName();
        }

        @Override
        public boolean isFunction() {
            return true;
        }

    }

}
