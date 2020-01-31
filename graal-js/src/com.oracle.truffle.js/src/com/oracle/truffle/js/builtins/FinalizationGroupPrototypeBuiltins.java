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
import com.oracle.truffle.js.builtins.FinalizationGroupPrototypeBuiltinsFactory.JSFinalizationGroupCleanupSomeNodeGen;
import com.oracle.truffle.js.builtins.FinalizationGroupPrototypeBuiltinsFactory.JSFinalizationGroupRegisterNodeGen;
import com.oracle.truffle.js.builtins.FinalizationGroupPrototypeBuiltinsFactory.JSFinalizationGroupUnregisterNodeGen;
import com.oracle.truffle.js.nodes.access.IsObjectNode;
import com.oracle.truffle.js.nodes.binary.JSIdenticalNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.nodes.unary.IsCallableNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSFinalizationGroup;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * Contains builtins for {@linkplain JSFinalizationGroup}.prototype.
 */
public final class FinalizationGroupPrototypeBuiltins extends JSBuiltinsContainer.SwitchEnum<FinalizationGroupPrototypeBuiltins.FinalizationGroupPrototype> {

    public static final JSBuiltinsContainer BUILTINS = new FinalizationGroupPrototypeBuiltins();

    protected FinalizationGroupPrototypeBuiltins() {
        super(JSFinalizationGroup.PROTOTYPE_NAME, FinalizationGroupPrototype.class);
    }

    public enum FinalizationGroupPrototype implements BuiltinEnum<FinalizationGroupPrototype> {
        register(2),
        unregister(1),
        cleanupSome(0);

        private final int length;

        FinalizationGroupPrototype(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, FinalizationGroupPrototype builtinEnum) {
        switch (builtinEnum) {
            case register:
                return JSFinalizationGroupRegisterNodeGen.create(context, builtin, args().withThis().fixedArgs(3).createArgumentNodes(context));
            case unregister:
                return JSFinalizationGroupUnregisterNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case cleanupSome:
                return JSFinalizationGroupCleanupSomeNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
        }
        return null;
    }

    public abstract static class FinalizationGroupOperation extends JSBuiltinNode {
        protected final BranchProfile errorBranch = BranchProfile.create();

        public FinalizationGroupOperation(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }
    }

    /**
     * Implementation of the FinalizationGroup.prototype.register().
     */
    public abstract static class JSFinalizationGroupRegisterNode extends FinalizationGroupOperation {

        @Child protected JSIdenticalNode sameValueNode = JSIdenticalNode.createSameValue();
        @Child protected IsObjectNode isObjectNode = IsObjectNode.create();

        public JSFinalizationGroupRegisterNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = "isJSFinalizationGroup(thisObj)")
        protected DynamicObject register(DynamicObject thisObj, Object target, Object holdings, Object unregisterTokenArg) {
            if (!isObjectNode.executeBoolean(target)) {
                errorBranch.enter();
                throw Errors.createTypeError("register expects target argument of type Object");
            }
            if (sameValueNode.executeBoolean(target, holdings)) {
                errorBranch.enter();
                throw Errors.createTypeError("register expects target and holding not to be identical");
            }
            Object unregisterToken = unregisterTokenArg;
            if (!isObjectNode.executeBoolean(unregisterToken)) {
                if (unregisterToken != Undefined.instance) {
                    errorBranch.enter();
                    throw Errors.createTypeError("register expects unregisterToken argument to be an object or empty");
                }
                unregisterToken = Undefined.instance;
            }
            JSFinalizationGroup.appendToCells(thisObj, target, holdings, unregisterToken);
            return Undefined.instance;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSFinalizationGroup(thisObj)")
        protected static DynamicObject notFinalizationGroup(@SuppressWarnings("unused") Object thisObj, Object target, Object holdings, Object unregisterToken) {
            throw Errors.createTypeErrorFinalizationGroupExpected();
        }
    }

    /**
     * Implementation of the FinalizationGroup.prototype.unregister().
     */
    public abstract static class JSFinalizationGroupUnregisterNode extends FinalizationGroupOperation {

        @Child protected IsObjectNode isObjectNode = IsObjectNode.create();

        public JSFinalizationGroupUnregisterNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = "isJSFinalizationGroup(thisObj)")
        protected boolean unregister(DynamicObject thisObj, Object unregisterToken) {
            if (unregisterToken == Undefined.instance || unregisterToken == Null.instance || !isObjectNode.executeBoolean(unregisterToken)) {
                errorBranch.enter();
                throw Errors.createTypeError("unregister expects unregisterToken argument to be an object");
            }
            return JSFinalizationGroup.removeFromCells(thisObj, unregisterToken);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSFinalizationGroup(thisObj)")
        protected static boolean notFinalizationGroup(@SuppressWarnings("unused") Object thisObj, Object unregisterToken) {
            throw Errors.createTypeErrorFinalizationGroupExpected();
        }
    }

    /**
     * Implementation of the FinalizationGroup.prototype.cleanupSome().
     */
    public abstract static class JSFinalizationGroupCleanupSomeNode extends FinalizationGroupOperation {

        @Child protected IsCallableNode isCallableNode = IsCallableNode.create();

        public JSFinalizationGroupCleanupSomeNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = "isJSFinalizationGroup(thisObj)")
        protected DynamicObject cleanupSome(DynamicObject thisObj, Object callback) {
            if (JSFinalizationGroup.isCleanupJobActive(thisObj)) {
                errorBranch.enter();
                throw Errors.createTypeError("finalization job aleady active");
            }
            if (callback != Undefined.instance && !isCallableNode.executeBoolean(callback)) {
                errorBranch.enter();
                throw Errors.createTypeError("cleanupSome expects callback to be callable");
            }
            JSFinalizationGroup.cleanupFinalizationGroup(getContext(), thisObj, callback);
            return Undefined.instance;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSFinalizationGroup(thisObj)")
        protected static DynamicObject notFinalizationGroup(@SuppressWarnings("unused") Object thisObj, Object callback) {
            throw Errors.createTypeErrorFinalizationGroupExpected();
        }
    }

}
