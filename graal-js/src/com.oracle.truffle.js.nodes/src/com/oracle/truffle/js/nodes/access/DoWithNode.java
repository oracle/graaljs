/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.ReadNode;
import com.oracle.truffle.js.nodes.cast.JSToBooleanNode;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Undefined;

public final class DoWithNode extends JSTargetableNode implements ReadNode, WriteNode {

    @Child private JSTargetableNode defaultDelegate;
    @Child private JavaScriptNode globalDelegate;
    @Child private DoWithTargetNode doWithTarget;

    public DoWithNode(JSContext context, String propertyName, JavaScriptNode withFrameSlot, JSTargetableNode defaultDelegate, JavaScriptNode globalDelegate) {
        this.defaultDelegate = defaultDelegate;
        this.globalDelegate = globalDelegate;
        this.doWithTarget = new DoWithTargetNode(context, propertyName, withFrameSlot);

        if (defaultDelegate instanceof GlobalPropertyNode) {
            ((GlobalPropertyNode) defaultDelegate).setPropertyAssumptionCheckEnabled(false);
        }
    }

    @Override
    public DoWithTargetNode getTarget() {
        return doWithTarget;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        TruffleObject target = evaluateTarget(frame);
        return executeWithTarget(frame, target);
    }

    @Override
    public TruffleObject evaluateTarget(VirtualFrame frame) {
        return doWithTarget.executeTruffleObject(frame);
    }

    @Override
    public Object executeWithTarget(VirtualFrame frame, Object target) {
        if (target != Undefined.instance) {
            // the object was found in the with chain
            if (defaultDelegate instanceof WritePropertyNode && globalDelegate != null) {
                return ((WritePropertyNode) defaultDelegate).executeWithValue(target, ((WriteNode) globalDelegate).getRhs().execute(frame));
            }
            return defaultDelegate.executeWithTarget(frame, target);
        } else {
            // not found
            if (globalDelegate == null) {
                // the globalDelegate is the same as defaultDelegate
                // this can be configured by leaving globalDelegate null.
                return defaultDelegate.execute(frame);
            } else {
                return globalDelegate.execute(frame);
            }
        }
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        DoWithNode copy = (DoWithNode) copy();
        copy.defaultDelegate = cloneUninitialized(defaultDelegate);
        copy.globalDelegate = cloneUninitialized(globalDelegate);
        copy.doWithTarget = cloneUninitialized(doWithTarget);
        return copy;
    }

    private static final class DoWithTargetNode extends JavaScriptNode {
        @Child private JavaScriptNode withVariable;
        @Child private HasPropertyCacheNode withObjectHasProperty;
        @Child private HasPropertyCacheNode globalObjectHasProperty;
        @Child private PropertyGetNode withObjectGetUnscopables;
        @Child private PropertyGetNode unscopablesGetProperty;
        @Child private JSToBooleanNode toBoolean;
        private final JSContext context;

        DoWithTargetNode(JSContext context, String propertyName, JavaScriptNode withVariable) {
            this.withVariable = withVariable;
            this.context = context;
            this.withObjectHasProperty = HasPropertyCacheNode.create(propertyName, context);
            this.globalObjectHasProperty = HasPropertyCacheNode.create(propertyName, context);
            this.withObjectGetUnscopables = PropertyGetNode.create(Symbol.SYMBOL_UNSCOPABLES, false, context);
            this.unscopablesGetProperty = PropertyGetNode.create(propertyName, false, context);
            this.toBoolean = JSToBooleanNode.create();
        }

        @Override
        public Object execute(VirtualFrame frame) {
            return executeTruffleObject(frame);
        }

        @Override
        public TruffleObject executeTruffleObject(VirtualFrame frame) {
            TruffleObject target;
            try {
                target = withVariable.executeTruffleObject(frame);
            } catch (UnexpectedResultException e) {
                throw new AssertionError("With variable must always be a TruffleObject.");
            }
            if (withObjectHasProperty.hasProperty(target) && isPropertyScopable(target)) {
                // with object has a scopable property
                return target;
            } else if (context.isOptionNashornCompatibilityMode() && hasNoSuchProperty(target, false) && !globalObjectHasProperty.hasProperty(GlobalObjectNode.getGlobalObject(context))) {
                // Nashorn extension: with object has a __noSuchProperty__ or __noSuchMethod__
                // NB: this part is not 1:1 compatible with Nashorn w.r.t. chained scopes.
                return target;
            }
            // not found, fall back to default handler
            return Undefined.instance;
        }

        /**
         * Object Environment Records - HasBinding (ES6 8.1.1.2.1).
         */
        private boolean isPropertyScopable(TruffleObject target) {
            if (context.getEcmaScriptVersion() >= 6) {
                Object unscopables = withObjectGetUnscopables.getValue(target);
                if (JSRuntime.isObject(unscopables)) {
                    boolean blocked = toBoolean.executeBoolean(unscopablesGetProperty.getValue(unscopables));
                    if (blocked) {
                        return false;
                    }
                }
            }
            return true;
        }

        private boolean hasNoSuchProperty(TruffleObject thisTruffleObj, boolean isMethod) {
            if (JSRuntime.isObject(thisTruffleObj)) {
                DynamicObject thisObj = (DynamicObject) thisTruffleObj;
                if ((!isMethod && !context.getNoSuchPropertyUnusedAssumption().isValid() && JSObject.hasOwnProperty(thisObj, JSObject.NO_SUCH_PROPERTY_NAME)) ||
                                (isMethod && !context.getNoSuchMethodUnusedAssumption().isValid() && JSObject.hasOwnProperty(thisObj, JSObject.NO_SUCH_METHOD_NAME))) {
                    return hasNoSuchPropertyImpl(thisObj, isMethod);
                }
            }
            return false;
        }

        private static boolean hasNoSuchPropertyImpl(DynamicObject thisObj, boolean isMethod) {
            assert JSRuntime.isObject(thisObj);
            Object function = JSObject.get(thisObj, isMethod ? JSObject.NO_SUCH_METHOD_NAME : JSObject.NO_SUCH_PROPERTY_NAME);
            if (JSFunction.isJSFunction(function)) {
                return true;
            }
            return false;
        }

        @Override
        protected JavaScriptNode copyUninitialized() {
            return new DoWithTargetNode(context, (String) withObjectHasProperty.getKey(), cloneUninitialized(withVariable));
        }

    }

    @Override
    public Object executeWrite(VirtualFrame frame, Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public JavaScriptNode getRhs() {
        return ((WriteNode) globalDelegate).getRhs();
    }
}
