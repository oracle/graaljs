/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.nodes.function;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.CreateObjectNode;
import com.oracle.truffle.js.nodes.access.InitializeInstanceElementsNode;
import com.oracle.truffle.js.nodes.access.JSWriteFrameSlotNode;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.access.PropertySetNode;
import com.oracle.truffle.js.nodes.decorators.ClassElementList;
import com.oracle.truffle.js.nodes.decorators.ClassElementNode;
import com.oracle.truffle.js.nodes.decorators.DecorateConstructorNode;
import com.oracle.truffle.js.nodes.decorators.DecorateElementNode;
import com.oracle.truffle.js.nodes.decorators.ElementDescriptor;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Null;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * ES6 14.5.14 Runtime Semantics: ClassDefinitionEvaluation.
 */
public final class ClassDefinitionNode extends JavaScriptNode implements FunctionNameHolder {

    private final JSContext context;
    @Child private JavaScriptNode constructorFunctionNode;
    @Child private JavaScriptNode classHeritageNode;
    @Children private final ClassElementNode[] memberNodes;

    @Child private JSWriteFrameSlotNode writeClassBindingNode;
    @Child private PropertyGetNode getPrototypeNode;
    @Child private CreateMethodPropertyNode setConstructorNode;
    @Child private CreateObjectNode.CreateObjectWithPrototypeNode createPrototypeNode;
    @Child private DefineMethodNode defineConstructorMethodNode;
    @Child private PropertySetNode setFieldsNode;
    @Child private InitializeInstanceElementsNode staticFieldsNode;
    @Child private PropertySetNode setPrivateBrandNode;
    @Child private SetFunctionNameNode setFunctionName;
    @Children private final JavaScriptNode[] decorators;
    @Child private DecorateElementNode decorateElementNode;
    @Child private DecorateConstructorNode decorateConstructorNode;

    private final boolean hasName;
    //private final int instanceFieldCount;
    //private final int staticFieldCount;

    protected ClassDefinitionNode(JSContext context, JSFunctionExpressionNode constructorFunctionNode, JavaScriptNode classHeritageNode, ClassElementNode[] memberNodes,
                    JSWriteFrameSlotNode writeClassBindingNode, boolean hasName, boolean hasPrivateInstanceMethods, JavaScriptNode[] decorators) {
        this.context = context;
        this.constructorFunctionNode = constructorFunctionNode;
        this.classHeritageNode = classHeritageNode;
        this.memberNodes = memberNodes;
        this.hasName = hasName;
        //this.instanceFieldCount = instanceFieldCount;
        //this.staticFieldCount = staticFieldCount;

        this.writeClassBindingNode = writeClassBindingNode;
        this.getPrototypeNode = PropertyGetNode.create(JSObject.PROTOTYPE, false, context);
        this.setConstructorNode = CreateMethodPropertyNode.create(context, JSObject.CONSTRUCTOR);
        this.createPrototypeNode = CreateObjectNode.createOrdinaryWithPrototype(context);
        this.defineConstructorMethodNode = DefineMethodNode.create(context, constructorFunctionNode);
        this.setFieldsNode = PropertySetNode.createSetHidden(JSFunction.CLASS_FIELDS_ID, context);//instanceFieldCount != 0 ? PropertySetNode.createSetHidden(JSFunction.CLASS_FIELDS_ID, context) : null;
        this.setPrivateBrandNode = PropertySetNode.createSetHidden(JSFunction.PRIVATE_BRAND_ID, context);//readd condition
        this.setFunctionName = hasName ? null : SetFunctionNameNode.create();
        this.decorators = decorators;
        this.decorateElementNode = DecorateElementNode.create(context);
        this.decorateConstructorNode = DecorateConstructorNode.create(context);
    }

    public static ClassDefinitionNode create(JSContext context, JSFunctionExpressionNode constructorFunction, JavaScriptNode classHeritage, ClassElementNode[] members,
                                             JSWriteFrameSlotNode writeClassBinding, boolean hasName, boolean hasPrivateInstanceMethods, JavaScriptNode[] decorators) {
        return new ClassDefinitionNode(context, constructorFunction, classHeritage, members, writeClassBinding, hasName, hasPrivateInstanceMethods, decorators);
    }

    @Override
    public DynamicObject execute(VirtualFrame frame) {
        return executeWithClassName(frame, null);
    }

    public DynamicObject executeWithClassName(VirtualFrame frame, Object className) {
        JSRealm realm = context.getRealm();
        Object protoParent = realm.getObjectPrototype();
        Object constructorParent = realm.getFunctionPrototype();
        if (classHeritageNode != null) {
            Object superclass = classHeritageNode.execute(frame);
            if (superclass == Null.instance) {
                protoParent = Null.instance;
            } else if (!JSRuntime.isConstructor(superclass)) {
                // 6.f. if IsConstructor(superclass) is false, throw a TypeError.
                throw Errors.createTypeError("not a constructor", this);
            } else if (JSRuntime.isGenerator(superclass)) {
                // 6.g.i. if superclass.[[FunctionKind]] is "generator", throw a TypeError
                throw Errors.createTypeError("class cannot extend a generator function", this);
            } else {
                protoParent = getPrototypeNode.getValue(superclass);
                if (protoParent != Null.instance && !JSRuntime.isObject(protoParent)) {
                    throw Errors.createTypeError("protoParent is neither Object nor Null", this);
                }
                constructorParent = superclass;
            }
        }

        /* Let proto be ObjectCreate(protoParent). */
        assert protoParent == Null.instance || JSRuntime.isObject(protoParent);
        DynamicObject proto = createPrototypeNode.execute(frame, ((DynamicObject) protoParent));

        /*
         * Let constructorInfo be the result of performing DefineMethod for constructor with
         * arguments proto and constructorParent as the optional functionPrototype argument.
         */
        DynamicObject constructor = defineConstructorMethodNode.execute(frame, proto, (DynamicObject) constructorParent);

        // Perform MakeConstructor(F, writablePrototype=false, proto).
        JSFunction.setClassPrototype(constructor, proto);

        // If className is not undefined, perform SetFunctionName(F, className).
        if (setFunctionName != null && className != null) {
            setFunctionName.execute(constructor, className);
        }

        // Perform CreateMethodProperty(proto, "constructor", F).
        setConstructorNode.executeVoid(proto, constructor);

        ClassElementList elements = new ClassElementList();

        // Perform ClassElementEvaluation and CoalesceClassElements
        classElementEvaluation(frame, proto, constructor, elements);

        // Perform DecorateClass
        decorateClass(frame, elements);

        //Object[][] instanceFields = 1 == 0 ? null : new Object[1][];
        Object[][] instanceFields = new Object[elements.getInstanceFieldCount()][];
        Object[][] staticFields = new Object[elements.getStaticFieldCount()][];

        boolean hasPrivateKey = initializeClassElements(proto, constructor, elements, instanceFields, staticFields);

        //TODO: AssignPrivateNames
        if (writeClassBindingNode != null) {
            writeClassBindingNode.executeWrite(frame, constructor);
        }

        //TODO: InitializeInstanceElements
        if (instanceFields.length > 0) {
            setFieldsNode.setValue(constructor, instanceFields);
        }

        // If the class contains a private instance method or accessor, set F.[[PrivateBrand]].
        if (hasPrivateKey) {
            HiddenKey privateBrand = new HiddenKey("Brand");
            setPrivateBrandNode.setValue(constructor, privateBrand);
        }

        if (staticFields.length > 0) {
            InitializeInstanceElementsNode defineStaticFields = this.staticFieldsNode;
            if (defineStaticFields == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                this.staticFieldsNode = defineStaticFields = insert(InitializeInstanceElementsNode.create(context));
            }
            defineStaticFields.executeStaticFields(constructor, staticFields);
        }

        return constructor;
    }

    private void classElementEvaluation(VirtualFrame frame, DynamicObject proto, DynamicObject constructor, ClassElementList elements) {
        HashMap<Object, ElementDescriptor> elementMap = new HashMap<>();
        for(ClassElementNode member: memberNodes) {
            DynamicObject homeObject = member.isStatic() ? constructor: proto;
            ElementDescriptor element = member.execute(frame, homeObject, context);
            //CoalesceClassElements
            if(!elementMap.containsKey(element.getKey())) {
                elementMap.put(element.getKey(), element);
            } else {
                if(element.isMethod() ||element.isAccessor()) {
                    ElementDescriptor other = elementMap.get(element.getKey());
                    if(other.isMethod() || element.isAccessor() && element.getPlacement() == other.getPlacement()) {
                        if(element.getDescriptor().isDataDescriptor() || other.getDescriptor().isDataDescriptor()) {
                            assert !element.hasPrivateKey();
                            assert element.getDescriptor().getConfigurable() && other.getDescriptor().getConfigurable();
                            if(element.hasDecorators() || other.hasDecorators()) {
                                throw Errors.createTypeError("Overwritten and overwriting methods can not be decorated.", this);
                            }
                            other.setDescriptor(element.getDescriptor());
                        } else {
                            if(element.hasDecorators()) {
                                if(other.hasDecorators()) {
                                    throw Errors.createTypeError("Either getter or setter can be decorated, not both.", this);
                                }
                                other.setDecorators(element.getDecorators());
                            }
                            //CoalesceGetterSetter
                            assert other.getDescriptor().isAccessorDescriptor() && element.getDescriptor().isAccessorDescriptor();
                            if(element.getDescriptor().hasGet()) {
                                other.getDescriptor().setGet((DynamicObject) element.getDescriptor().getGet());
                            } else {
                                assert element.getDescriptor().hasSet();
                                other.getDescriptor().setSet((DynamicObject) element.getDescriptor().getSet());
                            }
                        }
                        continue;
                    }
                }
            }
            elements.push(element);
        }
    }

    @ExplodeLoop
    private void decorateClass(VirtualFrame frame, ClassElementList elements) {
        //DecorateElements
        for (int i = 0; i < elements.size(); i++) {
            ElementDescriptor element = elements.pop();
            if(element.hasDecorators()) {
                decorateElementNode.decorateElement(element, elements);
            } else {
                elements.push(element);
            }
        }
        //DecorateClass
        if(decorators.length > 0) {
            Object[] d = new Object[decorators.length];
            for(int i = 0; i < decorators.length; i++) {
                d[i] = decorators[i].execute(frame);
            }
            decorateConstructorNode.decorateConstructor(elements, d);
        }
    }

    @ExplodeLoop
    private boolean initializeClassElements(DynamicObject proto, DynamicObject constructor, ClassElementList elements, Object[][] instanceFields, Object[][] staticFields) {
        boolean hasPrivateKey = false;
        int instanceFieldIndex = 0;
        int staticFieldIndex = 0;
        while(elements.size() > 0) {
            ElementDescriptor element = elements.pop();
            if(element.isStatic() && element.hasKey() && element.hasPrivateKey()) {
                hasPrivateKey = true;
            }
            if((element.isMethod() || element.isAccessor()) && !element.hasPrivateKey()) {
                if(element.isStatic()) {
                    JSRuntime.definePropertyOrThrow(constructor,element.getKey(),element.getDescriptor());
                }
                if(element.isPrototype()) {
                    JSRuntime.definePropertyOrThrow(proto, element.getKey(), element.getDescriptor());
                }
            }
            if(element.isField()) {
                assert !element.getDescriptor().hasValue() && !element.getDescriptor().hasGet() && !element.getDescriptor().hasSet();
                if(element.isStatic()) {
                    //defineField(constructor, element);
                    staticFields[staticFieldIndex++] = new Object[]{ element.getKey(), element.getInitialize(), false };
                } else {
                    //defineField(proto, element);
                    instanceFields[instanceFieldIndex++] = new Object[]{ element.getKey(), element.getInitialize(), false };
                }
            }
        }
        return hasPrivateKey;
    }

    @ExplodeLoop
    private void initializeMembers(VirtualFrame frame, DynamicObject proto, DynamicObject constructor, ArrayList<Object[]> instanceFields, Object[][] staticFields) {
        /* For each ClassElement e in order from NonConstructorMethodDefinitions of ClassBody */
        int instanceFieldIndex = 0;
        int staticFieldIndex = 0;
        for (ClassElementNode memberNode : memberNodes) {
            //TODO: InitializeClassElements
            /*DynamicObject homeObject = memberNode.isStatic() ? constructor : proto;
            memberNode.executeVoid(frame, homeObject, context);
            if (memberNode.isField()) {
                Object key = memberNode.executeKey(frame);
                Object value = memberNode.executeValue(frame, homeObject);
                Object[] field = new Object[]{key, value, memberNode.isAnonymousFunctionDefinition()};
                if (memberNode.isStatic() && staticFields != null) {
                    staticFields[staticFieldIndex++] = field;
                } else if (instanceFields != null) {
                    instanceFields.add(field);
                    //instanceFields[instanceFieldIndex++] = field;
                } else {
                    throw Errors.shouldNotReachHere();
                }
            }*/
        }
        //assert instanceFieldIndex == instanceFieldCount && staticFieldIndex == staticFieldCount;
    }

    @Override
    public boolean isResultAlwaysOfType(Class<?> clazz) {
        return clazz == DynamicObject.class;
    }

    @Override
    public String getFunctionName() {
        return hasName ? ((FunctionNameHolder) constructorFunctionNode).getFunctionName() : "";
    }

    @Override
    public void setFunctionName(String name) {
        ((FunctionNameHolder) constructorFunctionNode).setFunctionName(name);
    }

    /*@Override
    protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
        return create(context, (JSFunctionExpressionNode) cloneUninitialized(constructorFunctionNode, materializedTags), cloneUninitialized(classHeritageNode, materializedTags),
                        ObjectLiteralMemberNode.cloneUninitialized(memberNodes, materializedTags),
                        cloneUninitialized(writeClassBindingNode, materializedTags), hasName, instanceFieldCount, staticFieldCount, setPrivateBrandNode != null);
    }*/
}
