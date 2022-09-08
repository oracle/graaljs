/*
 * Copyright (c) 2018, 2022, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.nodes.cast.JSToStringNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.nodes.unary.IsCallableNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSFetchHeaders;
import com.oracle.truffle.js.runtime.builtins.JSFetchHeadersObject;
import com.oracle.truffle.js.builtins.FetchHeadersPrototypeBuiltinsFactory.JSFetchHeadersAppendNodeGen;
import com.oracle.truffle.js.builtins.FetchHeadersPrototypeBuiltinsFactory.JSFetchHeadersDeleteNodeGen;
import com.oracle.truffle.js.builtins.FetchHeadersPrototypeBuiltinsFactory.JSFetchHeadersGetNodeGen;
import com.oracle.truffle.js.builtins.FetchHeadersPrototypeBuiltinsFactory.JSFetchHeadersHasNodeGen;
import com.oracle.truffle.js.builtins.FetchHeadersPrototypeBuiltinsFactory.JSFetchHeadersSetNodeGen;
import com.oracle.truffle.js.builtins.FetchHeadersPrototypeBuiltinsFactory.JSFetchHeadersForEachNodeGen;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.Undefined;

import java.util.Map;

/**
 * Contains builtins for {@linkplain JSFetchHeaders}.prototype.
 */
public class FetchHeadersPrototypeBuiltins extends JSBuiltinsContainer.SwitchEnum<FetchHeadersPrototypeBuiltins.FetchHeadersPrototype> {
    public static final JSBuiltinsContainer BUILTINS = new FetchHeadersPrototypeBuiltins();

    protected FetchHeadersPrototypeBuiltins() {
        super(JSFetchHeaders.PROTOTYPE_NAME, FetchHeadersPrototypeBuiltins.FetchHeadersPrototype.class);
    }

    public enum FetchHeadersPrototype implements BuiltinEnum<FetchHeadersPrototypeBuiltins.FetchHeadersPrototype> {
        append(2),
        delete(1),
        get(1),
        has(1),
        set(2),
        forEach(1);

        private final int length;

        FetchHeadersPrototype(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, FetchHeadersPrototypeBuiltins.FetchHeadersPrototype builtinEnum) {
        switch (builtinEnum) {
            case append:
                return JSFetchHeadersAppendNodeGen.create(context, builtin, args().withThis().fixedArgs(2).createArgumentNodes(context));
            case delete:
                return JSFetchHeadersDeleteNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case get:
                return JSFetchHeadersGetNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case has:
                return JSFetchHeadersHasNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case set:
                return JSFetchHeadersSetNodeGen.create(context, builtin, args().withThis().fixedArgs(2).createArgumentNodes(context));
            case forEach:
                return JSFetchHeadersForEachNodeGen.create(context, builtin, args().withThis().fixedArgs(2).createArgumentNodes(context));
        }
        return null;
    }

    public abstract static class JSFetchHeadersOperation extends JSBuiltinNode {
        private final ConditionProfile isHeaders = ConditionProfile.createBinaryProfile();

        public JSFetchHeadersOperation(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        protected final JSFetchHeadersObject asFetchHeaders(Object object) {
            if (isHeaders.profile(JSFetchHeaders.isJSFetchHeaders(object))) {
                return (JSFetchHeadersObject) object;
            } else {
                throw Errors.createTypeError("Not a Headers object");
            }
        }
    }

    /**
     * https://fetch.spec.whatwg.org/#dom-headers-append.
     */
    public abstract static class JSFetchHeadersAppendNode extends FetchHeadersPrototypeBuiltins.JSFetchHeadersOperation {
        public JSFetchHeadersAppendNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object append(Object thisHeaders, TruffleString name, TruffleString value) {
            asFetchHeaders(thisHeaders).getHeadersMap().append(name.toString(), value.toString());
            return Undefined.instance;
        }
    }

    /**
     * https://fetch.spec.whatwg.org/#dom-headers-delete.
     */
    public abstract static class JSFetchHeadersDeleteNode extends FetchHeadersPrototypeBuiltins.JSFetchHeadersOperation {
        public JSFetchHeadersDeleteNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object delete(Object thisHeaders, TruffleString name) {
            asFetchHeaders(thisHeaders).getHeadersMap().delete(name.toString());
            return Undefined.instance;
        }
    }

    /**
     * https://fetch.spec.whatwg.org/#dom-headers-get.
     */
    public abstract static class JSFetchHeadersGetNode extends FetchHeadersPrototypeBuiltins.JSFetchHeadersOperation {
        public JSFetchHeadersGetNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object get(Object thisHeaders, Object name, @Cached("create()") JSToStringNode toString) {
            String value = asFetchHeaders(thisHeaders).getHeadersMap().get(toString.executeString(name).toString());
            if (value == null) {
                return Undefined.instance;
            }
            return TruffleString.fromJavaStringUncached(value, TruffleString.Encoding.UTF_8);
        }
    }

    /**
     * https://fetch.spec.whatwg.org/#dom-headers-has.
     */
    public abstract static class JSFetchHeadersHasNode extends FetchHeadersPrototypeBuiltins.JSFetchHeadersOperation {
        public JSFetchHeadersHasNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected boolean has(Object thisHeaders, Object name) {
            return asFetchHeaders(thisHeaders).getHeadersMap().has(name.toString());
        }
    }

    /**
     * https://fetch.spec.whatwg.org/#dom-headers-set.
     */
    public abstract static class JSFetchHeadersSetNode extends FetchHeadersPrototypeBuiltins.JSFetchHeadersOperation {
        public JSFetchHeadersSetNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object set(Object thisHeaders, Object name, Object value) {
            asFetchHeaders(thisHeaders).getHeadersMap().set(name.toString(), value.toString());
            return Undefined.instance;
        }
    }

    public abstract static class JSFetchHeadersForEachNode extends FetchHeadersPrototypeBuiltins.JSFetchHeadersOperation {
        @Child private IsCallableNode isCallableNode;
        @Child private JSFunctionCallNode callFunctionNode;

        public JSFetchHeadersForEachNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        private boolean isCallable(Object object) {
            if (isCallableNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                isCallableNode = insert(IsCallableNode.create());
            }
            return isCallableNode.executeBoolean(object);
        }

        private Object call(Object function, Object target, Object... userArguments) {
            if (callFunctionNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                callFunctionNode = insert(JSFunctionCallNode.createCall());
            }
            return callFunctionNode.executeCall(JSArguments.create(target, function, userArguments));
        }

        @Specialization(guards = {"isCallable.executeBoolean(callback)"}, limit = "1")
        protected Object forEachFunction(JSFetchHeadersObject thisHeaders, JSDynamicObject callback, Object thisArg,
                                         @Cached @SuppressWarnings("unused") IsCallableNode isCallable,
                                         @Cached("createCall()") JSFunctionCallNode callNode) {
            Map<String, String> entries = asFetchHeaders(thisHeaders).getHeadersMap().entries();

            for (Map.Entry<String, String> e : entries.entrySet()) {
                TruffleString key = TruffleString.fromJavaStringUncached(e.getKey(), TruffleString.Encoding.UTF_8);
                TruffleString value = TruffleString.fromJavaStringUncached(e.getValue(), TruffleString.Encoding.UTF_8);
                callNode.executeCall(JSArguments.create(thisArg, callback, new Object[]{value, key, thisHeaders}));
            }

            return Undefined.instance;
        }
    }
}
