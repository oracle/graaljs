/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.builtins;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.js.builtins.RealmFunctionBuiltinsFactory.RealmCreateNodeGen;
import com.oracle.truffle.js.builtins.RealmFunctionBuiltinsFactory.RealmCurrentNodeGen;
import com.oracle.truffle.js.builtins.RealmFunctionBuiltinsFactory.RealmDisposeNodeGen;
import com.oracle.truffle.js.builtins.RealmFunctionBuiltinsFactory.RealmEvalNodeGen;
import com.oracle.truffle.js.builtins.RealmFunctionBuiltinsFactory.RealmGlobalNodeGen;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.runtime.AbstractJavaScriptLanguage;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.Evaluator;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * Contains builtins for the Realm function (constructor).
 */
public final class RealmFunctionBuiltins extends JSBuiltinsContainer.SwitchEnum<RealmFunctionBuiltins.RealmFunction> {

    protected RealmFunctionBuiltins() {
        super(JSRealm.REALM_BUILTIN_CLASS_NAME, RealmFunction.class);
    }

    public enum RealmFunction implements BuiltinEnum<RealmFunction> {
        create(0),
        global(1),
        dispose(1),
        current(0),
        eval(2);

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
                return RealmCreateNodeGen.create(context, builtin, args().fixedArgs(0).createArgumentNodes(context));
            case global:
                return RealmGlobalNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            case dispose:
                return RealmDisposeNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            case current:
                return RealmCurrentNodeGen.create(context, builtin, args().fixedArgs(0).createArgumentNodes(context));
            case eval:
                return RealmEvalNodeGen.create(context, builtin, args().fixedArgs(2).createArgumentNodes(context));
        }
        return null;
    }

    protected static int getIdx(Object realm) {
        int realmIdx = JSRuntime.intValue(JSRuntime.toNumber(realm));
        if (realmIdx < 0) {
            throw Errors.createTypeError("Invalid realm index");
        }
        return realmIdx;
    }

    public abstract static class RealmCreateNode extends JSBuiltinNode {

        public RealmCreateNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object createRealm() {
            JSRealm newRealm = getContext().createChildContext().getRealm();
            return getContext().getIndexFromRealmList(newRealm);
        }
    }

    public abstract static class RealmDisposeNode extends JSBuiltinNode {

        public RealmDisposeNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object dispose(Object realm) {
            int idx = getIdx(realm);
            if (idx >= 0) {
                getContext().setInRealmList(idx, null);
            }
            return Undefined.instance;
        }
    }

    public abstract static class RealmGlobalNode extends JSBuiltinNode {

        public RealmGlobalNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object dispose(Object realm) {
            int idx = getIdx(realm);
            JSRealm jsrealm = getContext().getFromRealmList(idx);
            return jsrealm.getGlobalObject();
        }
    }

    public abstract static class RealmCurrentNode extends JSBuiltinNode {

        public RealmCurrentNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object current() {
            return getContext().getIndexFromRealmList(getContext().getRealm());
        }
    }

    public abstract static class RealmEvalNode extends JSBuiltinNode {

        public RealmEvalNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object eval(Object realm, Object code) {
            int idx = getIdx(realm);
            JSRealm jsrealm = getContext().getFromRealmList(idx);
            String sourceText = JSRuntime.toString(code);
            return eval(jsrealm, sourceText);
        }

        @TruffleBoundary
        private Object eval(JSRealm realm, String sourceText) {
            Source source = Source.newBuilder(sourceText).name(Evaluator.EVAL_SOURCE_NAME).mimeType(AbstractJavaScriptLanguage.APPLICATION_MIME_TYPE).build();
            return realm.getContext().getEvaluator().evaluate(realm, this, source);
        }

    }
}
