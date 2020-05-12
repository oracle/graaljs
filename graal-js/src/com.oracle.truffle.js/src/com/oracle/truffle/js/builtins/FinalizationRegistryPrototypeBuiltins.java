/*
 * Copyright (c) 2020, 2020, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.builtins;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.js.builtins.FinalizationRegistryPrototypeBuiltinsFactory.JSFinalizationRegistryCleanupSomeNodeGen;
import com.oracle.truffle.js.builtins.FinalizationRegistryPrototypeBuiltinsFactory.JSFinalizationRegistryRegisterNodeGen;
import com.oracle.truffle.js.builtins.FinalizationRegistryPrototypeBuiltinsFactory.JSFinalizationRegistryUnregisterNodeGen;
import com.oracle.truffle.js.nodes.access.IsObjectNode;
import com.oracle.truffle.js.nodes.binary.JSIdenticalNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.nodes.unary.IsCallableNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSFinalizationRegistry;
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * Contains builtins for {@linkplain JSFinalizationRegistry}.prototype.
 */
public final class FinalizationRegistryPrototypeBuiltins extends JSBuiltinsContainer.SwitchEnum<FinalizationRegistryPrototypeBuiltins.FinalizationRegistryPrototype> {

    public static final JSBuiltinsContainer BUILTINS = new FinalizationRegistryPrototypeBuiltins();

    protected FinalizationRegistryPrototypeBuiltins() {
        super(JSFinalizationRegistry.PROTOTYPE_NAME, FinalizationRegistryPrototype.class);
    }

    public enum FinalizationRegistryPrototype implements BuiltinEnum<FinalizationRegistryPrototype> {
        register(2),
        unregister(1),
        cleanupSome(0);

        private final int length;

        FinalizationRegistryPrototype(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, FinalizationRegistryPrototype builtinEnum) {
        switch (builtinEnum) {
            case register:
                return JSFinalizationRegistryRegisterNodeGen.create(context, builtin, args().withThis().fixedArgs(3).createArgumentNodes(context));
            case unregister:
                return JSFinalizationRegistryUnregisterNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case cleanupSome:
                return JSFinalizationRegistryCleanupSomeNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
        }
        return null;
    }

    public abstract static class FinalizationRegistryOperation extends JSBuiltinNode {
        protected final BranchProfile errorBranch = BranchProfile.create();

        public FinalizationRegistryOperation(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        protected void invalidUnregisterToken(Object token) {
            errorBranch.enter();
            throw Errors.createTypeErrorFormat("unregisterToken ('%s') must be an object", JSRuntime.safeToString(token));
        }
    }

    /**
     * Implementation of the FinalizationRegistry.prototype.register().
     */
    public abstract static class JSFinalizationRegistryRegisterNode extends FinalizationRegistryOperation {

        @Child protected JSIdenticalNode sameValueNode = JSIdenticalNode.createSameValue();
        @Child protected IsObjectNode isObjectNode = IsObjectNode.create();

        public JSFinalizationRegistryRegisterNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = "isJSFinalizationRegistry(thisObj)")
        protected DynamicObject register(DynamicObject thisObj, Object target, Object holdings, Object unregisterTokenArg) {
            if (!isObjectNode.executeBoolean(target)) {
                errorBranch.enter();
                throw Errors.createTypeError("FinalizationRegistry.prototype.register: target must be an object");
            }
            if (sameValueNode.executeBoolean(target, holdings)) {
                errorBranch.enter();
                throw Errors.createTypeError("FinalizationRegistry.prototype.register: target and holdings must not be same");
            }
            Object unregisterToken = unregisterTokenArg;
            if (!isObjectNode.executeBoolean(unregisterToken)) {
                if (unregisterToken != Undefined.instance) {
                    invalidUnregisterToken(unregisterToken);
                }
                unregisterToken = Undefined.instance;
            }
            JSFinalizationRegistry.appendToCells(thisObj, target, holdings, unregisterToken);
            return Undefined.instance;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSFinalizationRegistry(thisObj)")
        protected static DynamicObject notFinalizationRegistry(@SuppressWarnings("unused") Object thisObj, Object target, Object holdings, Object unregisterToken) {
            throw Errors.createTypeErrorFinalizationRegistryExpected();
        }
    }

    /**
     * Implementation of the FinalizationRegistry.prototype.unregister().
     */
    public abstract static class JSFinalizationRegistryUnregisterNode extends FinalizationRegistryOperation {

        @Child protected IsObjectNode isObjectNode = IsObjectNode.create();

        public JSFinalizationRegistryUnregisterNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = "isJSFinalizationRegistry(thisObj)")
        protected boolean unregister(DynamicObject thisObj, Object unregisterToken) {
            if (!isObjectNode.executeBoolean(unregisterToken)) {
                invalidUnregisterToken(unregisterToken);
            }
            return JSFinalizationRegistry.removeFromCells(thisObj, unregisterToken);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSFinalizationRegistry(thisObj)")
        protected static boolean notFinalizationRegistry(@SuppressWarnings("unused") Object thisObj, Object unregisterToken) {
            throw Errors.createTypeErrorFinalizationRegistryExpected();
        }
    }

    /**
     * Implementation of the FinalizationRegistry.prototype.cleanupSome().
     */
    public abstract static class JSFinalizationRegistryCleanupSomeNode extends FinalizationRegistryOperation {

        @Child protected IsCallableNode isCallableNode = IsCallableNode.create();

        public JSFinalizationRegistryCleanupSomeNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = "isJSFinalizationRegistry(thisObj)")
        protected DynamicObject cleanupSome(DynamicObject thisObj, Object callback) {
            if (JSFinalizationRegistry.isCleanupJobActive(thisObj)) {
                errorBranch.enter();
                throw Errors.createTypeError("finalization job aleady active");
            }
            if (callback != Undefined.instance && !isCallableNode.executeBoolean(callback)) {
                errorBranch.enter();
                throw Errors.createTypeError("FinalizationRegistry: cleanup must be callable");
            }
            JSFinalizationRegistry.cleanupFinalizationRegistry(getContext(), thisObj, callback);
            return Undefined.instance;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSFinalizationRegistry(thisObj)")
        protected static DynamicObject notFinalizationRegistry(@SuppressWarnings("unused") Object thisObj, Object callback) {
            throw Errors.createTypeErrorFinalizationRegistryExpected();
        }
    }

}
