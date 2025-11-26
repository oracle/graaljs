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
package com.oracle.truffle.js.nodes.arguments;

import java.util.Set;

import com.oracle.truffle.api.dsl.Idempotent;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.Properties;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.builtins.JSArgumentsArray;
import com.oracle.truffle.js.runtime.builtins.JSArgumentsObject;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunctionObject;
import com.oracle.truffle.js.runtime.builtins.JSObjectFactory;
import com.oracle.truffle.js.runtime.objects.JSAttributes;
import com.oracle.truffle.js.runtime.objects.JSProperty;
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * Allocate arguments object from arguments array.
 */
public abstract class ArgumentsObjectNode extends JavaScriptNode {
    protected final boolean strict;
    private final int leadingArgCount;
    private final JSContext context;

    @Child private DynamicObject.PutNode putLengthNode;
    @Child private DynamicObject.PutNode putSymbolIteratorNode;
    @Child private DynamicObject.PutNode putCalleeNode;
    @Child private DynamicObject.PutNode putCallerNode;

    private static final int THROWER_ACCESSOR_PROPERTY_FLAGS = JSAttributes.notConfigurableNotEnumerable() | JSProperty.ACCESSOR;

    protected ArgumentsObjectNode(JSContext context, boolean strict, int leadingArgCount) {
        this.strict = strict;
        this.leadingArgCount = leadingArgCount;
        this.context = context;

        this.putLengthNode = DynamicObject.PutNode.create();
        this.putSymbolIteratorNode = DynamicObject.PutNode.create();
        this.putCalleeNode = DynamicObject.PutNode.create();
        this.putCallerNode = strict && context.getEcmaScriptVersion() < JSConfig.ECMAScript2017 ? DynamicObject.PutNode.create() : null;
    }

    public static JavaScriptNode create(JSContext context, boolean strict, int leadingArgCount) {
        return ArgumentsObjectNodeGen.create(context, strict, leadingArgCount);
    }

    @Idempotent
    protected final boolean isStrict() {
        return strict;
    }

    @Specialization(guards = "isStrict()")
    protected final JSArgumentsObject doUnmapped(VirtualFrame frame) {
        Object[] arguments = getObjectArray(frame);
        JSRealm realm = getRealm();
        assert realm == JSFunction.getRealm(getFunctionObject(frame));

        JSObjectFactory factory = context.getStrictArgumentsFactory();
        JSArgumentsObject argumentsObject = JSArgumentsArray.createUnmapped(factory.getShape(realm), factory.getPrototype(realm), arguments);
        factory.initProto(argumentsObject, realm);

        Properties.putWithFlags(putLengthNode, argumentsObject, JSArgumentsArray.LENGTH, arguments.length, JSAttributes.getDefaultNotEnumerable());
        Properties.putWithFlags(putSymbolIteratorNode, argumentsObject, Symbol.SYMBOL_ITERATOR, realm.getArrayProtoValuesIterator(), JSAttributes.getDefaultNotEnumerable());

        Properties.putWithFlags(putCalleeNode, argumentsObject, JSArgumentsArray.CALLEE, realm.getThrowerAccessor(), THROWER_ACCESSOR_PROPERTY_FLAGS);
        if (context.getEcmaScriptVersion() < JSConfig.ECMAScript2017) {
            Properties.putWithFlags(putCallerNode, argumentsObject, JSArgumentsArray.CALLER, realm.getThrowerAccessor(), THROWER_ACCESSOR_PROPERTY_FLAGS);
        }
        return context.trackAllocation(argumentsObject);
    }

    @Specialization(guards = "!isStrict()")
    protected final JSArgumentsObject doMapped(VirtualFrame frame) {
        Object[] arguments = getObjectArray(frame);
        JSFunctionObject callee = getFunctionObject(frame);
        // non-strict functions may have unmapped (strict) arguments, but not the other way around.
        // (namely, if simpleParameterList is false, or if it is a built-in function)
        assert !JSFunction.isStrict(callee);

        JSRealm realm = getRealm();
        assert realm == JSFunction.getRealm(callee);

        JSObjectFactory factory = context.getNonStrictArgumentsFactory();
        JSArgumentsObject argumentsObject = JSArgumentsArray.createMapped(factory.getShape(realm), factory.getPrototype(realm), arguments);
        factory.initProto(argumentsObject, realm);

        Properties.putWithFlags(putLengthNode, argumentsObject, JSArgumentsArray.LENGTH, arguments.length, JSAttributes.getDefaultNotEnumerable());
        Properties.putWithFlags(putSymbolIteratorNode, argumentsObject, Symbol.SYMBOL_ITERATOR, realm.getArrayProtoValuesIterator(), JSAttributes.getDefaultNotEnumerable());

        Properties.putWithFlags(putCalleeNode, argumentsObject, JSArgumentsArray.CALLEE, callee, JSAttributes.getDefaultNotEnumerable());
        return context.trackAllocation(argumentsObject);
    }

    private static JSFunctionObject getFunctionObject(VirtualFrame frame) {
        return (JSFunctionObject) JSArguments.getFunctionObject(frame.getArguments());
    }

    public Object[] getObjectArray(VirtualFrame frame) {
        return JSArguments.extractUserArguments(frame.getArguments(), leadingArgCount);
    }

    static boolean isInitialized(Object argumentsArray) {
        return argumentsArray != Undefined.instance;
    }

    @Override
    protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
        return ArgumentsObjectNodeGen.create(context, strict, leadingArgCount);
    }
}
