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
