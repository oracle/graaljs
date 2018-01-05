/*
 * Copyright (c) 2014, 2014, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.runtime.util;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.nodes.NodeCloneable;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.ObjectType;
import com.oracle.truffle.js.runtime.Boundaries;
import com.oracle.truffle.js.runtime.builtins.JSClass;
import com.oracle.truffle.js.runtime.objects.JSShape;

public final class JSClassProfile extends NodeCloneable {
    @CompilationFinal private JSClass expectedJSClass;
    @CompilationFinal private boolean polymorphicJSClass;

    private JSClassProfile() {
    }

    public static JSClassProfile create() {
        return new JSClassProfile();
    }

    public JSClass getJSClass(DynamicObject jsobject) {
        ObjectType jsobjectClass = JSShape.getJSClassNoCast(jsobject.getShape());
        if (!polymorphicJSClass) {
            if (jsobjectClass == expectedJSClass) {
                return expectedJSClass;
            } else {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                if (expectedJSClass == null) {
                    expectedJSClass = (JSClass) jsobjectClass;
                } else {
                    polymorphicJSClass = true;
                }
            }
        }
        return (JSClass) jsobjectClass;
    }

    public JSClass profile(JSClass jsobjectClass) {
        if (!polymorphicJSClass) {
            if (jsobjectClass == expectedJSClass) {
                return expectedJSClass;
            } else {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                if (expectedJSClass == null) {
                    expectedJSClass = jsobjectClass;
                } else {
                    polymorphicJSClass = true;
                }
            }
        }
        return jsobjectClass;
    }

    @Override
    public String toString() {
        return polymorphicJSClass ? "polymorphic" : Boundaries.stringValueOf(expectedJSClass);
    }
}
