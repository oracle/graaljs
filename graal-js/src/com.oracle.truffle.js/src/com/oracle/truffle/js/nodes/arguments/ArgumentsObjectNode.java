/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.RealmNode;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.builtins.JSArgumentsArray;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSObjectFactory;
import com.oracle.truffle.js.runtime.objects.JSAttributes;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.JSProperty;
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * Allocate arguments object from arguments array.
 */
public abstract class ArgumentsObjectNode extends JavaScriptNode {
    private final boolean strict;
    @Child private RealmNode realmNode;
    private final int leadingArgCount;

    @Child private DynamicObjectLibrary putLengthNode;
    @Child private DynamicObjectLibrary putSymbolIteratorNode;
    @Child private DynamicObjectLibrary putCalleeNode;
    @Child private DynamicObjectLibrary putCallerNode;

    private static final int THROWER_ACCESSOR_PROPERTY_FLAGS = JSAttributes.notConfigurableNotEnumerable() | JSProperty.ACCESSOR;

    protected ArgumentsObjectNode(JSContext context, boolean strict, int leadingArgCount) {
        this.strict = strict;
        this.realmNode = RealmNode.create(context);
        this.leadingArgCount = leadingArgCount;

        this.putLengthNode = JSObjectUtil.createDispatched(JSArgumentsArray.LENGTH);
        this.putSymbolIteratorNode = JSObjectUtil.createDispatched(Symbol.SYMBOL_ITERATOR);
        this.putCalleeNode = JSObjectUtil.createDispatched(JSArgumentsArray.CALLEE);
    }

    public static JavaScriptNode create(JSContext context, boolean strict, int leadingArgCount) {
        return ArgumentsObjectNodeGen.create(context, strict, leadingArgCount);
    }

    protected final boolean isStrict(VirtualFrame frame) {
        // non-strict functions may have unmapped (strict) arguments, but not the other way around.
        // (namely, if simpleParameterList is false, or if it is a built-in function)
        assert strict == JSFunction.isStrict(getFunctionObject(frame)) || strict;
        return strict;
    }

    @Specialization(guards = "isStrict(frame)")
    protected DynamicObject doStrict(VirtualFrame frame) {
        Object[] arguments = getObjectArray(frame);
        JSContext context = realmNode.getContext();
        JSRealm realm = realmNode.execute(frame);

        JSObjectFactory factory = context.getStrictArgumentsFactory();
        DynamicObject argumentsObject = JSArgumentsArray.createUnmapped(factory.getShape(realm), arguments);
        factory.initProto(argumentsObject, realm);

        putLengthNode.putWithFlags(argumentsObject, JSArgumentsArray.LENGTH, arguments.length, JSAttributes.getDefaultNotEnumerable());
        putSymbolIteratorNode.putWithFlags(argumentsObject, Symbol.SYMBOL_ITERATOR, realm.getArrayProtoValuesIterator(), JSAttributes.getDefaultNotEnumerable());

        putCalleeNode.putWithFlags(argumentsObject, JSArgumentsArray.CALLEE, realm.getThrowerAccessor(), THROWER_ACCESSOR_PROPERTY_FLAGS);
        if (realm.getContext().getEcmaScriptVersion() < JSConfig.ECMAScript2017) {
            if (putCallerNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                putCallerNode = insert(JSObjectUtil.createDispatched(JSArgumentsArray.CALLER));
            }
            putCallerNode.putWithFlags(argumentsObject, JSArgumentsArray.CALLER, realm.getThrowerAccessor(), THROWER_ACCESSOR_PROPERTY_FLAGS);
        }
        return context.trackAllocation(argumentsObject);
    }

    @Specialization(guards = "!isStrict(frame)")
    protected DynamicObject doNonStrict(VirtualFrame frame) {
        Object[] arguments = getObjectArray(frame);
        JSContext context = realmNode.getContext();
        JSRealm realm = realmNode.execute(frame);
        DynamicObject callee = getFunctionObject(frame);

        JSObjectFactory factory = context.getNonStrictArgumentsFactory();
        DynamicObject argumentsObject = JSArgumentsArray.createMapped(factory.getShape(realm), arguments);
        factory.initProto(argumentsObject, realm);

        putLengthNode.putWithFlags(argumentsObject, JSArgumentsArray.LENGTH, arguments.length, JSAttributes.getDefaultNotEnumerable());
        putSymbolIteratorNode.putWithFlags(argumentsObject, Symbol.SYMBOL_ITERATOR, realm.getArrayProtoValuesIterator(), JSAttributes.getDefaultNotEnumerable());

        putCalleeNode.putWithFlags(argumentsObject, JSArgumentsArray.CALLEE, callee, JSAttributes.getDefaultNotEnumerable());
        return context.trackAllocation(argumentsObject);
    }

    private static DynamicObject getFunctionObject(VirtualFrame frame) {
        return (DynamicObject) JSArguments.getFunctionObject(frame.getArguments());
    }

    public Object[] getObjectArray(VirtualFrame frame) {
        return JSArguments.extractUserArguments(frame.getArguments(), leadingArgCount);
    }

    static boolean isInitialized(Object argumentsArray) {
        return argumentsArray != Undefined.instance;
    }

    @Override
    protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
        return ArgumentsObjectNodeGen.create(realmNode.getContext(), strict, leadingArgCount);
    }
}
