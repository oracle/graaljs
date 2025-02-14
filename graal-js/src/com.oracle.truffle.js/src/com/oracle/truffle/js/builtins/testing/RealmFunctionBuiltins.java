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
package com.oracle.truffle.js.builtins.testing;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.js.builtins.JSBuiltinsContainer;
import com.oracle.truffle.js.builtins.testing.RealmFunctionBuiltinsFactory.RealmCreateNodeGen;
import com.oracle.truffle.js.builtins.testing.RealmFunctionBuiltinsFactory.RealmCurrentNodeGen;
import com.oracle.truffle.js.builtins.testing.RealmFunctionBuiltinsFactory.RealmDetachGlobalNodeGen;
import com.oracle.truffle.js.builtins.testing.RealmFunctionBuiltinsFactory.RealmDisposeNodeGen;
import com.oracle.truffle.js.builtins.testing.RealmFunctionBuiltinsFactory.RealmEvalNodeGen;
import com.oracle.truffle.js.builtins.testing.RealmFunctionBuiltinsFactory.RealmGlobalNodeGen;
import com.oracle.truffle.js.builtins.testing.RealmFunctionBuiltinsFactory.RealmNavigateNodeGen;
import com.oracle.truffle.js.builtins.testing.RealmFunctionBuiltinsFactory.RealmOwnerNodeGen;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.nodes.ScriptNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.Evaluator;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunctionObject;
import com.oracle.truffle.js.runtime.builtins.JSProxy;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * Contains builtins for the Realm function (V8 compatibility).
 */
public final class RealmFunctionBuiltins extends JSBuiltinsContainer.SwitchEnum<RealmFunctionBuiltins.RealmFunction> {

    public static final JSBuiltinsContainer BUILTINS = new RealmFunctionBuiltins();

    protected RealmFunctionBuiltins() {
        super(JSRealm.REALM_BUILTIN_CLASS_NAME, RealmFunction.class);
    }

    public enum RealmFunction implements BuiltinEnum<RealmFunction> {
        create(0),
        createAllowCrossRealmAccess(0),
        global(1),
        dispose(1),
        current(0),
        eval(2),
        owner(1),
        detachGlobal(1),
        navigate(1);

        private final int length;

        RealmFunction(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, RealmFunction builtinEnum) {
        switch (builtinEnum) {
            case create:
            case createAllowCrossRealmAccess:
                return RealmCreateNodeGen.create(context, builtin, args().fixedArgs(0).createArgumentNodes(context));
            case global:
                return RealmGlobalNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            case dispose:
                return RealmDisposeNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            case current:
                return RealmCurrentNodeGen.create(context, builtin, args().fixedArgs(0).createArgumentNodes(context));
            case eval:
                return RealmEvalNodeGen.create(context, builtin, args().fixedArgs(2).createArgumentNodes(context));
            case owner:
                return RealmOwnerNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            case detachGlobal:
                return RealmDetachGlobalNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            case navigate:
                return RealmNavigateNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
        }
        return null;
    }

    protected static JSRealm topLevelRealm(Node node) {
        return JSRealm.getMain(node);
    }

    protected static int toRealmIndexOrThrow(JSRealm topLevelRealm, Object index) {
        int realmIdx = JSRuntime.intValue(JSRuntime.toNumber(index));
        if (realmIdx < 0) {
            throw Errors.createTypeError("Invalid realm index");
        }
        JSRealm jsrealm = topLevelRealm.getFromRealmList(realmIdx);
        if (jsrealm == null) {
            throw Errors.createTypeError("Invalid realm index");
        }
        return realmIdx;
    }

    public abstract static class RealmCreateNode extends JSBuiltinNode {

        public RealmCreateNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @TruffleBoundary
        @Specialization
        protected Object createRealm() {
            JSRealm topLevelRealm = topLevelRealm(this);
            JSRealm newRealm = topLevelRealm.createChildRealm();
            return topLevelRealm.getIndexFromRealmList(newRealm);
        }
    }

    public abstract static class RealmDisposeNode extends JSBuiltinNode {

        public RealmDisposeNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @TruffleBoundary
        @Specialization
        protected Object dispose(Object index) {
            JSRealm topLevelRealm = topLevelRealm(this);
            int realmIndex = toRealmIndexOrThrow(topLevelRealm, index);
            topLevelRealm.removeFromRealmList(realmIndex);
            return Undefined.instance;
        }
    }

    public abstract static class RealmGlobalNode extends JSBuiltinNode {

        public RealmGlobalNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @TruffleBoundary
        @Specialization
        protected Object global(Object index) {
            JSRealm topLevelRealm = topLevelRealm(this);
            int realmIndex = toRealmIndexOrThrow(topLevelRealm, index);
            JSRealm jsrealm = topLevelRealm.getFromRealmList(realmIndex);
            return jsrealm.getGlobalObject();
        }

    }

    public abstract static class RealmCurrentNode extends JSBuiltinNode {

        public RealmCurrentNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @TruffleBoundary
        @Specialization
        protected Object current() {
            JSRealm topLevelRealm = topLevelRealm(this);
            JSRealm currentRealm = topLevelRealm.getCurrentV8Realm();
            return topLevelRealm.getIndexFromRealmList(currentRealm);
        }
    }

    public abstract static class RealmEvalNode extends JSBuiltinNode {

        public RealmEvalNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @TruffleBoundary
        @Specialization
        protected Object eval(Object index, Object code) {
            JSRealm topLevelRealm = topLevelRealm(this);
            int realmIndex = toRealmIndexOrThrow(topLevelRealm, index);
            JSRealm selectedRealm = topLevelRealm.getFromRealmList(realmIndex);
            String sourceText = JSRuntime.toJavaString(code);
            Source source = Source.newBuilder(JavaScriptLanguage.ID, sourceText, Evaluator.EVAL_SOURCE_NAME).build();
            JSRealm currentV8Realm = topLevelRealm.getCurrentV8Realm();
            topLevelRealm.setCurrentV8Realm(selectedRealm);
            try {
                ScriptNode script = getContext().getEvaluator().parseEval(getContext(), this, source, null);
                return script.runEval(IndirectCallNode.getUncached(), selectedRealm);
            } finally {
                topLevelRealm.setCurrentV8Realm(currentV8Realm);
            }
        }
    }

    public abstract static class RealmOwnerNode extends JSBuiltinNode {

        public RealmOwnerNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @TruffleBoundary
        @Specialization
        protected Object owner(Object object) {
            JSRealm realm;
            if (JSObject.isJSObject(object)) {
                realm = creationRealm((JSObject) object);
            } else {
                throw Errors.createError("Invalid argument");
            }
            JSRealm topLevelRealm = topLevelRealm(this);
            int index = topLevelRealm.getIndexFromRealmList(realm);
            return (index == -1) ? Undefined.instance : index;
        }

        private static JSRealm creationRealm(JSObject object) {
            if (object instanceof JSFunctionObject function) {
                return JSFunction.getRealm(function);
            } else {
                return creationRealmFromConstructor(object);
            }
        }

        private static JSRealm creationRealmFromConstructor(JSObject object) {
            Object nonProxy = JSProxy.getTargetNonProxy(object);
            if (nonProxy instanceof JSObject) {
                JSDynamicObject prototype = JSObject.getPrototype((JSDynamicObject) nonProxy);
                if (prototype != Null.instance) {
                    Object constructor = JSRuntime.getDataProperty(prototype, JSObject.CONSTRUCTOR);
                    if (JSFunction.isJSFunction(constructor)) {
                        return JSFunction.getRealm((JSFunctionObject) constructor);
                    }
                }
            }
            return null;
        }

    }

    public abstract static class RealmDetachGlobalNode extends JSBuiltinNode {

        public RealmDetachGlobalNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @TruffleBoundary
        @Specialization
        protected Object detachGlobal(Object index) {
            JSRealm topLevelRealm = topLevelRealm(this);
            int realmIndex = toRealmIndexOrThrow(topLevelRealm, index);
            JSRealm realm = topLevelRealm.getFromRealmList(realmIndex);
            JSObject.setPrototype(realm.getGlobalObject(), Null.instance);
            realm.setGlobalObject(Undefined.instance);
            return Undefined.instance;
        }
    }

    public abstract static class RealmNavigateNode extends JSBuiltinNode {

        public RealmNavigateNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @TruffleBoundary
        @Specialization
        protected Object navigate(Object index) {
            JSRealm topLevelRealm = topLevelRealm(this);
            int realmIndex = toRealmIndexOrThrow(topLevelRealm, index);
            JSRealm realm = topLevelRealm.getFromRealmList(realmIndex);

            // detachGlobal(index)
            JSObject.setPrototype(realm.getGlobalObject(), Null.instance);
            realm.setGlobalObject(Undefined.instance);

            JSRealm newRealm = topLevelRealm.createChildRealm();
            int tempIdx = topLevelRealm.getIndexFromRealmList(newRealm);
            topLevelRealm.removeFromRealmList(tempIdx);
            topLevelRealm.setInRealmList(tempIdx, newRealm);

            return Undefined.instance;
        }
    }

}
