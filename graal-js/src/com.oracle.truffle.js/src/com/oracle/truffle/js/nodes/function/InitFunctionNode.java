/*
 * Copyright (c) 2018, 2025, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.Properties;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunctionData;
import com.oracle.truffle.js.runtime.builtins.JSFunctionObject;
import com.oracle.truffle.js.runtime.objects.Accessor;
import com.oracle.truffle.js.runtime.objects.JSAttributes;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSProperty;
import com.oracle.truffle.js.runtime.objects.Undefined;

public class InitFunctionNode extends JavaScriptBaseNode {
    private final JSFunctionData functionData;
    private final JSContext context;
    @Child private DynamicObject.PutConstantNode setPrototypeNode;
    @Child private DynamicObject.PutConstantNode setLengthNode;
    @Child private DynamicObject.PutConstantNode setNameNode;
    @Child private DynamicObject.PutConstantNode setArgumentsNode;
    @Child private DynamicObject.PutConstantNode setCallerNode;
    @Child private DynamicObject.PutNode setArgumentsAccessorNode;
    @Child private DynamicObject.PutNode setCallerAccessorNode;
    private final int prototypeFlags;
    private final int lengthFlags;
    private final int nameFlags;
    private final int argumentsCallerFlags;
    private final boolean strictProperties;

    protected InitFunctionNode(JSFunctionData functionData, JSContext context, boolean strictProperties, boolean isConstructor, boolean isBound, boolean isGenerator, boolean prototypeNotWritable) {
        this.functionData = functionData;
        this.context = context;
        this.strictProperties = strictProperties;
        boolean hasPrototype = (isConstructor && !isBound) || isGenerator;
        if (hasPrototype) {
            int prototypeAttributes = prototypeNotWritable ? JSAttributes.notConfigurableNotEnumerableNotWritable() : JSAttributes.notConfigurableNotEnumerableWritable();
            this.prototypeFlags = prototypeAttributes | JSProperty.PROXY;
            this.setPrototypeNode = DynamicObject.PutConstantNode.create();
        } else {
            this.prototypeFlags = 0;
        }

        int lengthAttributes = context.getEcmaScriptVersion() < 6 ? JSAttributes.notConfigurableNotEnumerableNotWritable() : JSAttributes.configurableNotEnumerableNotWritable();
        this.lengthFlags = lengthAttributes | JSProperty.PROXY;
        this.setLengthNode = DynamicObject.PutConstantNode.create();

        int nameAttributes = JSAttributes.configurableNotEnumerableNotWritable();
        this.nameFlags = nameAttributes | JSProperty.PROXY;
        this.setNameNode = DynamicObject.PutConstantNode.create();

        int argumentsCallerAttributes = 0;
        if (context.getEcmaScriptVersion() >= 6) {
            if (!strictProperties) {
                argumentsCallerAttributes = JSAttributes.notConfigurableNotEnumerableNotWritable();
                this.setArgumentsNode = DynamicObject.PutConstantNode.create();
                this.setCallerNode = DynamicObject.PutConstantNode.create();
            }
        } else {
            if (strictProperties) {
                argumentsCallerAttributes = JSAttributes.notConfigurableNotEnumerable() | JSProperty.ACCESSOR;
                this.setArgumentsAccessorNode = DynamicObject.PutNode.create();
                this.setCallerAccessorNode = DynamicObject.PutNode.create();
            }
        }
        this.argumentsCallerFlags = argumentsCallerAttributes;
    }

    protected InitFunctionNode(JSFunctionData functionData) {
        this(functionData, functionData.getContext(), functionData.hasStrictFunctionProperties(), functionData.isConstructor(), functionData.isBound(), functionData.isGenerator(),
                        functionData.isPrototypeNotWritable());
        assert !functionData.isBuiltin();
    }

    public static InitFunctionNode create(JSContext context, boolean strictProperties, boolean isConstructor, boolean isBound, boolean isGenerator, boolean prototypeNotWritable) {
        return new InitFunctionNode(null, context, strictProperties, isConstructor, isBound, isGenerator, prototypeNotWritable);
    }

    public static InitFunctionNode create(JSFunctionData functionData) {
        return new InitFunctionNode(functionData);
    }

    public final JSFunctionObject execute(JSFunctionObject function) {
        return execute(function, functionData.getLength(), functionData.getName());
    }

    public final JSFunctionObject execute(JSFunctionObject function, @SuppressWarnings("hiding") JSFunctionData functionData) {
        return execute(function, functionData.getLength(), functionData.getName());
    }

    public final JSFunctionObject execute(JSFunctionObject function, int length, TruffleString name) {
        // setLengthNode.putWithFlags(function, JSFunction.LENGTH, length, lengthFlags);
        Properties.putConstant(setLengthNode, function, JSFunction.LENGTH, JSFunction.LENGTH_PROXY, lengthFlags);
        assert function instanceof JSFunctionObject.BoundOrWrapped || length == (int) JSFunction.LENGTH_PROXY.get(function);

        // setNameNode.putWithFlags(function, JSFunction.NAME, name, nameFlags);
        Properties.putConstant(setNameNode, function, JSFunction.NAME, JSFunction.NAME_PROXY, nameFlags);
        assert function instanceof JSFunctionObject.BoundOrWrapped || Strings.equals(name, (TruffleString) JSFunction.NAME_PROXY.get(function));

        if (context.getEcmaScriptVersion() >= 6) {
            if (!strictProperties) {
                if (context.isOptionV8CompatibilityMode()) {
                    Properties.putConstant(setArgumentsNode, function, JSFunction.ARGUMENTS, JSFunction.ARGUMENTS_PROXY, argumentsCallerFlags | JSProperty.PROXY);
                    Properties.putConstant(setCallerNode, function, JSFunction.CALLER, JSFunction.CALLER_PROXY, argumentsCallerFlags | JSProperty.PROXY);
                } else {
                    Properties.putConstant(setArgumentsNode, function, JSFunction.ARGUMENTS, Undefined.instance, argumentsCallerFlags);
                    Properties.putConstant(setCallerNode, function, JSFunction.CALLER, Undefined.instance, argumentsCallerFlags);
                }
            }
        } else {
            if (strictProperties) {
                Accessor throwerAccessor = JSFunction.getRealm(function).getThrowerAccessor();
                Properties.putWithFlags(setArgumentsAccessorNode, function, JSFunction.ARGUMENTS, throwerAccessor, argumentsCallerFlags);
                Properties.putWithFlags(setCallerAccessorNode, function, JSFunction.CALLER, throwerAccessor, argumentsCallerFlags);
            }
        }

        if (setPrototypeNode != null) {
            Properties.putConstant(setPrototypeNode, function, JSObject.PROTOTYPE, JSFunction.PROTOTYPE_PROXY, prototypeFlags);
        }

        return function;
    }
}
