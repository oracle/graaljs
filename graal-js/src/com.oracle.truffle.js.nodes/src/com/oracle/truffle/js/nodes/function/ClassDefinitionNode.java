/*
 * Copyright (c) 2015, 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.function;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.CreateObjectNode;
import com.oracle.truffle.js.nodes.access.ObjectLiteralNode.ObjectLiteralMemberNode;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Null;

/**
 * ES6 14.5.14 Runtime Semantics: ClassDefinitionEvaluation.
 */
public class ClassDefinitionNode extends JavaScriptNode implements FunctionNameHolder {

    private final JSContext context;
    @Child private JSFunctionExpressionNode constructorFunctionNode;
    @Child private JavaScriptNode classHeritageNode;
    @Children private final ObjectLiteralMemberNode[] memberNodes;

    @Child private PropertyGetNode getPrototypeNode;
    @Child private CreateMethodPropertyNode setConstructorNode;
    @Child private CreateObjectNode.CreateObjectWithPrototypeNode createObjectNode;
    @Child private DefineMethodNode defineConstructorMethodNode;

    private final boolean hasName;

    protected ClassDefinitionNode(JSContext context, JSFunctionExpressionNode constructorFunctionNode, JavaScriptNode classHeritageNode, ObjectLiteralMemberNode[] memberNodes, boolean hasName) {
        this.context = context;
        this.constructorFunctionNode = constructorFunctionNode;
        this.classHeritageNode = classHeritageNode;
        this.memberNodes = memberNodes;
        this.hasName = hasName;

        this.getPrototypeNode = PropertyGetNode.create(JSObject.PROTOTYPE, false, context);
        this.setConstructorNode = CreateMethodPropertyNode.create(context, JSObject.CONSTRUCTOR);
        this.createObjectNode = CreateObjectNode.createWithPrototype(context, null);
        this.defineConstructorMethodNode = DefineMethodNode.create(context, constructorFunctionNode);
    }

    public static ClassDefinitionNode create(JSContext context, JSFunctionExpressionNode constructorFunction, JavaScriptNode classHeritage, ObjectLiteralMemberNode[] members, boolean hasName) {
        return new ClassDefinitionNode(context, constructorFunction, classHeritage, members, hasName);
    }

    @Override
    public DynamicObject execute(VirtualFrame frame) {
        JSRealm realm = context.getRealm();
        Object protoParent = realm.getObjectPrototype();
        Object constructorParent = realm.getFunctionPrototype();
        if (classHeritageNode != null) {
            Object superclass = classHeritageNode.execute(frame);
            if (superclass == Null.instance) {
                protoParent = null;
            } else if (!JSRuntime.isConstructor(superclass)) {
                // 6.f. if IsConstructor(superclass) is false, throw a TypeError.
                throw Errors.createTypeError("not a constructor", this);
            } else if (JSRuntime.isGenerator(superclass)) {
                // 6.g.i. if superclass.[[FunctionKind]] is "generator", throw a TypeError
                throw Errors.createTypeError("class cannot extend a generator function", this);
            } else {
                protoParent = getPrototypeNode.getValue(superclass);
                if (protoParent == Null.instance) {
                    protoParent = null;
                } else if (!JSRuntime.isObject(protoParent)) {
                    assert protoParent != Null.instance;
                    throw Errors.createTypeError("protoParent is neither Object nor Null", this);
                }
                constructorParent = superclass;
            }
        }

        /* Let proto be ObjectCreate(protoParent). */
        DynamicObject proto = createObjectNode.executeDynamicObject(frame, ((DynamicObject) protoParent));

        /*
         * Let constructorInfo be the result of performing DefineMethod for constructor with
         * arguments proto and constructorParent as the optional functionPrototype argument.
         */
        DynamicObject constructor = defineConstructorMethodNode.execute(frame, proto, (DynamicObject) constructorParent);

        /*
         * TODO If ClassHeritage_opt is present, set F's [[ConstructorKind]] internal slot to
         * "derived".
         *
         * Perform MakeConstructor(F, writablePrototype=false, proto).
         *
         * Perform MakeClassConstructor(F).
         */
        JSFunction.setClassPrototype(constructor, proto);

        /* Perform CreateMethodProperty(proto, "constructor", F). */
        setConstructorNode.executeVoid(proto, constructor);

        initializeMembers(frame, proto, constructor);

        return constructor;
    }

    @ExplodeLoop
    private void initializeMembers(VirtualFrame frame, DynamicObject proto, DynamicObject constructor) {
        /* For each ClassElement m in order from NonConstructorMethodDefinitions of ClassBody */
        for (ObjectLiteralMemberNode memberNode : memberNodes) {
            memberNode.executeVoid(frame, memberNode.isStatic() ? constructor : proto, context);
        }
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

    @Override
    protected JavaScriptNode copyUninitialized() {
        return create(context, cloneUninitialized(constructorFunctionNode), cloneUninitialized(classHeritageNode), ObjectLiteralMemberNode.cloneUninitialized(memberNodes), hasName);
    }
}
