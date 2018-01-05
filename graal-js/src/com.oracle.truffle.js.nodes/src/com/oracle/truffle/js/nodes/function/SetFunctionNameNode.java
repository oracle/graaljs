/*
 * Copyright (c) 2015, 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.function;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.PropertyDescriptor;

public class SetFunctionNameNode extends JavaScriptBaseNode {
    private final ConditionProfile isSymbolProfile;

    protected SetFunctionNameNode() {
        this.isSymbolProfile = ConditionProfile.createBinaryProfile();
    }

    public static SetFunctionNameNode create() {
        return new SetFunctionNameNode();
    }

    public Object execute(Object functionValue, Object propertyKey) {
        return execute(functionValue, propertyKey, null);
    }

    public Object execute(Object functionValue, Object propertyKey, String prefix) {
        assert JSFunction.isJSFunction(functionValue);
        assert JSRuntime.isPropertyKey(propertyKey);
        String name = isSymbolProfile.profile(propertyKey instanceof Symbol) ? ((Symbol) propertyKey).toFunctionNameString() : (String) propertyKey;
        if (prefix != null && !prefix.isEmpty()) {
            name = concatenate(prefix, name);
        }

        return setFunctionName((DynamicObject) functionValue, name);
    }

    @TruffleBoundary
    private static String concatenate(String prefix, String name) {
        return new StringBuilder(prefix.length() + 1 + name.length()).append(prefix).append(' ').append(name).toString();
    }

    private static Object setFunctionName(DynamicObject functionValue, String name) {
        PropertyDescriptor propDesc = PropertyDescriptor.createData(name, false, false, true);
        JSRuntime.definePropertyOrThrow(functionValue, JSFunction.NAME, propDesc, JSObject.getJSContext(functionValue));
        return functionValue;
    }
}
