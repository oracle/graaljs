/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.access;

import java.util.List;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameUtil;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.control.StatementNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSException;
import com.oracle.truffle.js.runtime.objects.Dead;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.PropertyDescriptor;
import com.oracle.truffle.js.runtime.objects.Undefined;

public class GlobalDeclarationInstantiationNode extends StatementNode {
    private final JSContext context;
    @Children private final DeclareGlobalNode[] globalDeclarations;

    public static final DeclareGlobalNode[] EMPTY_DECLARATION_ARRAY = new DeclareGlobalNode[0];

    protected GlobalDeclarationInstantiationNode(JSContext context, DeclareGlobalNode[] globalDeclarations) {
        this.context = context;
        this.globalDeclarations = globalDeclarations;
    }

    public static JavaScriptNode create(JSContext context, DeclareGlobalNode[] globalDeclarations) {
        return new GlobalDeclarationInstantiationNode(context, globalDeclarations);
    }

    public static JavaScriptNode create(JSContext context, List<DeclareGlobalNode> globalDeclarations) {
        return create(context, globalDeclarations.toArray(EMPTY_DECLARATION_ARRAY));
    }

    @Override
    public Object execute(VirtualFrame frame) {
        MaterializedFrame globalScope = context.getRealm().getGlobalScope();
        DynamicObject globalObject = context.getRealm().getGlobalObject();

        // verify that declarations are definable
        for (DeclareGlobalNode declaration : globalDeclarations) {
            String varName = declaration.varName;
            if (hasLexicalDeclaration(globalScope, varName)) {
                throw throwSyntaxErrorVariableAlreadyDeclared(varName);
            }
            if (declaration.isLexicallyDeclared()) {
                if (hasRestrictedGlobalProperty(globalObject, varName)) {
                    throw throwSyntaxErrorVariableAlreadyDeclared(varName);
                }
            }
        }

        instantiateDeclarations(frame);
        return EMPTY;
    }

    @ExplodeLoop
    private void instantiateDeclarations(VirtualFrame frame) {
        for (DeclareGlobalNode declaration : globalDeclarations) {
            declaration.executeVoid(frame, context);
        }
    }

    @TruffleBoundary
    private static boolean hasLexicalDeclaration(MaterializedFrame globalScopeFrame, String varName) {
        FrameSlot slot = globalScopeFrame.getFrameDescriptor().findFrameSlot(varName);
        return slot != null && (!globalScopeFrame.isObject(slot) || FrameUtil.getObjectSafe(globalScopeFrame, slot) != Dead.instance());
    }

    @TruffleBoundary
    private static boolean hasRestrictedGlobalProperty(DynamicObject globalObject, String varName) {
        PropertyDescriptor desc = JSObject.getOwnProperty(globalObject, varName);
        return desc != null && !desc.getConfigurable();
    }

    private static JSException throwSyntaxErrorVariableAlreadyDeclared(String varName) {
        CompilerDirectives.transferToInterpreter();
        throw Errors.createSyntaxError("Variable \"" + varName + "\" has already been declared");
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return new GlobalDeclarationInstantiationNode(context, cloneUninitialized(globalDeclarations));
    }

    private static DeclareGlobalNode[] cloneUninitialized(DeclareGlobalNode[] members) {
        DeclareGlobalNode[] copy = members.clone();
        for (int i = 0; i < copy.length; i++) {
            copy[i] = copy[i].copyUninitialized();
        }
        return copy;
    }

    @Override
    public boolean isResultAlwaysOfType(Class<?> clazz) {
        assert EMPTY == Undefined.instance;
        return clazz == Undefined.class;
    }
}
