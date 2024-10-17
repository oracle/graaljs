/*
 * Copyright (c) 2018, 2024, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.nodes.access;

import java.util.Set;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.cast.JSToBooleanNode;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * Reads the {@code with} binding object and performs Object Environment HasBinding(name). Returns
 * {@code undefined} if the binding was not found or blocked by an {@code unscopables} property.
 */
public final class WithTargetNode extends JavaScriptNode {
    @Child private JavaScriptNode withVariable;
    @Child private HasPropertyCacheNode withObjectHasProperty;
    @Child private HasPropertyCacheNode globalObjectHasProperty;
    @Child private PropertyGetNode withObjectGetUnscopables;
    @Child private PropertyGetNode unscopablesGetProperty;
    @Child private JSToBooleanNode toBoolean;
    private final JSContext context;

    private WithTargetNode(JSContext context, TruffleString propertyName, JavaScriptNode withVariable) {
        this.withVariable = withVariable;
        this.context = context;
        this.withObjectHasProperty = HasPropertyCacheNode.create(propertyName, context);
        this.globalObjectHasProperty = HasPropertyCacheNode.create(propertyName, context);
        this.withObjectGetUnscopables = PropertyGetNode.create(Symbol.SYMBOL_UNSCOPABLES, false, context);
        this.unscopablesGetProperty = PropertyGetNode.create(propertyName, false, context);
        this.toBoolean = JSToBooleanNode.create();
    }

    public static JavaScriptNode create(JSContext context, TruffleString propertyName, JavaScriptNode withVariable) {
        return new WithTargetNode(context, propertyName, withVariable);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        Object target = withVariable.execute(frame);
        if (withObjectHasProperty.hasProperty(target) && isPropertyScopable(target)) {
            // with object has a scopable property
            return target;
        } else if (context.isOptionNashornCompatibilityMode() && hasNoSuchProperty(target, false) && !globalObjectHasProperty.hasProperty(getRealm().getGlobalObject())) {
            // Nashorn extension: with object has a __noSuchProperty__ or __noSuchMethod__
            // NB: this part is not 1:1 compatible with Nashorn w.r.t. chained scopes.
            return target;
        }
        // not found, fall back to default handler
        return Undefined.instance;
    }

    /**
     * Object Environment Records - HasBinding().
     */
    private boolean isPropertyScopable(Object target) {
        if (context.getEcmaScriptVersion() >= 6) {
            if (JSDynamicObject.isJSDynamicObject(target)) {
                Object unscopables = withObjectGetUnscopables.getValue(target);
                if (JSRuntime.isObject(unscopables)) {
                    boolean blocked = toBoolean.executeBoolean(unscopablesGetProperty.getValue(unscopables));
                    if (blocked) {
                        return false;
                    }
                }
            } else {
                // no unscopables in foreign objects
                return true;
            }
        }
        return true;
    }

    private boolean hasNoSuchProperty(Object thisTruffleObj, boolean isMethod) {
        if (JSRuntime.isObject(thisTruffleObj)) {
            JSDynamicObject thisObj = (JSDynamicObject) thisTruffleObj;
            if ((!isMethod && !context.getNoSuchPropertyUnusedAssumption().isValid() && JSObject.hasOwnProperty(thisObj, JSObject.NO_SUCH_PROPERTY_NAME)) ||
                            (isMethod && !context.getNoSuchMethodUnusedAssumption().isValid() && JSObject.hasOwnProperty(thisObj, JSObject.NO_SUCH_METHOD_NAME))) {
                return hasNoSuchPropertyImpl(thisObj, isMethod);
            }
        }
        return false;
    }

    private static boolean hasNoSuchPropertyImpl(JSDynamicObject thisObj, boolean isMethod) {
        assert JSRuntime.isObject(thisObj);
        Object function = JSObject.get(thisObj, isMethod ? JSObject.NO_SUCH_METHOD_NAME : JSObject.NO_SUCH_PROPERTY_NAME);
        return JSFunction.isJSFunction(function);
    }

    @Override
    protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
        return new WithTargetNode(context, (TruffleString) withObjectHasProperty.getKey(), cloneUninitialized(withVariable, materializedTags));
    }
}
