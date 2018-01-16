/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.trufflenode.node;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.js.nodes.access.GetPrototypeNode;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JavaScriptRootNode;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.trufflenode.GraalJSAccess;
import com.oracle.truffle.trufflenode.NativeAccess;
import com.oracle.truffle.trufflenode.info.Accessor;
import com.oracle.truffle.trufflenode.info.FunctionTemplate;

/**
 *
 * @author Jan Stola
 */
public class ExecuteNativeAccessorNode extends JavaScriptRootNode {

    private final GraalJSAccess graalAccess;
    private final Accessor accessor;
    private final FunctionTemplate signature;
    private final boolean getter;
    private final BranchProfile errorBranch = BranchProfile.create();
    @Child private GetPrototypeNode getPrototypeNode;
    @Child private PropertyGetNode prototypePropertyGetNode;
    @Child private PropertyGetNode holderPropertyGetNode;

    public ExecuteNativeAccessorNode(GraalJSAccess graalAccess, JSContext context, Accessor accessor, boolean getter) {
        this.graalAccess = graalAccess;
        this.accessor = accessor;
        this.signature = accessor.getSignature();
        this.getter = getter;
        this.getPrototypeNode = GetPrototypeNode.create();
        this.prototypePropertyGetNode = PropertyGetNode.create(JSObject.PROTOTYPE, false, context);
        this.holderPropertyGetNode = PropertyGetNode.create(GraalJSAccess.HOLDER_KEY, false, context);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        Object[] arguments = frame.getArguments();
        if (signature != null) {
            DynamicObject functionObject = signature.getFunctionObject();
            if (functionObject != null) {
                Object functionPrototype = prototypePropertyGetNode.getValue(functionObject);
                Object instancePrototype = getPrototypeNode.executeJSObject(arguments[0]);
                if (functionPrototype != instancePrototype) {
                    errorBranch.enter();
                    throw Errors.createTypeError(incompatibleReceiverMessage());
                }
            }
        }
        long functionPointer = getter ? accessor.getGetterPtr() : accessor.getSetterPtr();
        Object holder = holderPropertyGetNode.getValue(arguments[1]);
        return executeAccessorMethod(functionPointer, holder, arguments);
    }

    @CompilerDirectives.TruffleBoundary
    private String incompatibleReceiverMessage() {
        return "Method " + accessor.getName() + " called on incompatible receiver";
    }

    @CompilerDirectives.TruffleBoundary
    private Object executeAccessorMethod(long functionPointer, Object holder, Object[] arguments) {
        Object result;
        if (getter) {
            result = NativeAccess.executeAccessorGetter(functionPointer, holder, accessor.getName(), arguments, accessor.getData());
            result = graalAccess.correctReturnValue(result);
        } else {
            NativeAccess.executeAccessorSetter(functionPointer, holder, accessor.getName(), arguments, accessor.getData());
            result = Undefined.instance;
        }
        return result;
    }

}
