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
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.exception.AbstractTruffleException;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.builtins.helper.FetchRequest;
import com.oracle.truffle.js.builtins.helper.TruffleJSONParser;
import com.oracle.truffle.js.nodes.control.TryCatchNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.nodes.promise.NewPromiseCapabilityNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSArrayBuffer;
import com.oracle.truffle.js.runtime.builtins.JSArrayBufferObject;
import com.oracle.truffle.js.runtime.builtins.JSFetchRequest;
import com.oracle.truffle.js.runtime.builtins.JSFetchRequestObject;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.PromiseCapabilityRecord;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.builtins.FetchRequestPrototypeBuiltinsFactory.JSFetchRequestCloneNodeGen;
import com.oracle.truffle.js.builtins.FetchRequestPrototypeBuiltinsFactory.JSFetchRequestBodyArrayBufferNodeGen;
import com.oracle.truffle.js.builtins.FetchRequestPrototypeBuiltinsFactory.JSFetchRequestBodyBlobNodeGen;
import com.oracle.truffle.js.builtins.FetchRequestPrototypeBuiltinsFactory.JSFetchRequestBodyFormDataNodeGen;
import com.oracle.truffle.js.builtins.FetchRequestPrototypeBuiltinsFactory.JSFetchRequestBodyJsonNodeGen;
import com.oracle.truffle.js.builtins.FetchRequestPrototypeBuiltinsFactory.JSFetchRequestBodyTextNodeGen;

/**
 * Contains builtins for {@linkplain JSFetchRequest}.prototype.
 */
public final class FetchRequestPrototypeBuiltins extends JSBuiltinsContainer.SwitchEnum<FetchRequestPrototypeBuiltins.FetchRequestPrototype> {
    public static final JSBuiltinsContainer BUILTINS = new FetchRequestPrototypeBuiltins();

    protected FetchRequestPrototypeBuiltins() {
        super(JSFetchRequest.PROTOTYPE_NAME, FetchRequestPrototypeBuiltins.FetchRequestPrototype.class);
    }

    public enum FetchRequestPrototype implements BuiltinEnum<FetchRequestPrototypeBuiltins.FetchRequestPrototype> {
        clone(0),
        // body
        arrayBuffer(0),
        blob(0),
        formData(0),
        json(0),
        text(0);

        private final int length;

        FetchRequestPrototype(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, FetchRequestPrototypeBuiltins.FetchRequestPrototype builtinEnum) {
        switch (builtinEnum) {
            case clone:
                return JSFetchRequestCloneNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
            case arrayBuffer:
                return JSFetchRequestBodyArrayBufferNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
            case blob:
                return JSFetchRequestBodyBlobNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
            case formData:
                return JSFetchRequestBodyFormDataNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
            case json:
                return JSFetchRequestBodyJsonNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
            case text:
                return JSFetchRequestBodyTextNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
        }
        return null;
    }

    public abstract static class JSFetchRequestOperation extends JSBuiltinNode {
        @Child NewPromiseCapabilityNode newPromiseCapability;
        @Child JSFunctionCallNode promiseResolutionCallNode;
        @Child TryCatchNode.GetErrorObjectNode getErrorObjectNode;
        private final BranchProfile errorBranch = BranchProfile.create();
        private final ConditionProfile isRequest = ConditionProfile.createBinaryProfile();

        public JSFetchRequestOperation(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            this.newPromiseCapability = NewPromiseCapabilityNode.create(context);
            this.promiseResolutionCallNode = JSFunctionCallNode.createCall();
        }

        protected JSDynamicObject toPromise(Object resolution) {
            PromiseCapabilityRecord promiseCapability = newPromiseCapability.execute(getRealm().getPromiseConstructor());
            try {
                promiseResolutionCallNode.executeCall(JSArguments.createOneArg(Undefined.instance, promiseCapability.getResolve(), resolution));
            } catch (AbstractTruffleException ex) {
                errorBranch.enter();
                if (getErrorObjectNode == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    getErrorObjectNode = insert(TryCatchNode.GetErrorObjectNode.create(getContext()));
                }
                Object error = getErrorObjectNode.execute(ex);
                promiseResolutionCallNode.executeCall(JSArguments.createOneArg(Undefined.instance, promiseCapability.getReject(), error));
            }
            return promiseCapability.getPromise();
        }

        protected final JSFetchRequestObject asFetchRequest(Object object) {
            if (isRequest.profile(JSFetchRequest.isJSFetchRequest(object))) {
                return (JSFetchRequestObject) object;
            } else {
                throw Errors.createTypeError("Not a Request");
            }
        }
    }

    public abstract static class JSFetchRequestCloneNode extends FetchRequestPrototypeBuiltins.JSFetchRequestOperation {
        public JSFetchRequestCloneNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected JSFetchRequestObject doOperation(Object thisRequest) {
            FetchRequest request = asFetchRequest(thisRequest).getRequestMap();
            FetchRequest cloned = request.copy();
            return JSFetchRequest.create(getContext(), getRealm(), cloned);
        }
    }

    public abstract static class JSFetchRequestBodyArrayBufferNode extends FetchRequestPrototypeBuiltins.JSFetchRequestOperation {
        public JSFetchRequestBodyArrayBufferNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected JSDynamicObject doOperation(Object thisRequest) {
            FetchRequest request = asFetchRequest(thisRequest).getRequestMap();
            String body = request.consumeBody();
            JSArrayBufferObject arrayBuffer = JSArrayBuffer.createArrayBuffer(getContext(), getRealm(), body.getBytes());
            return toPromise(arrayBuffer);
        }
    }

    public abstract static class JSFetchRequestBodyBlobNode extends FetchRequestPrototypeBuiltins.JSFetchRequestOperation {
        public JSFetchRequestBodyBlobNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected JSDynamicObject doOperation(@SuppressWarnings("unused") Object thisRequest) {
            throw Errors.notImplemented("JSFetchRequestBodyBlobNode");
        }
    }

    public abstract static class JSFetchRequestBodyFormDataNode extends FetchRequestPrototypeBuiltins.JSFetchRequestOperation {
        public JSFetchRequestBodyFormDataNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected JSDynamicObject doOperation(@SuppressWarnings("unused") Object thisRequest) {
            throw Errors.notImplemented("JSFetchRequestBodyFormDataNode");
        }
    }

    public abstract static class JSFetchRequestBodyJsonNode extends FetchRequestPrototypeBuiltins.JSFetchRequestOperation {
        public JSFetchRequestBodyJsonNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected JSDynamicObject doOperation(Object thisRequest) {
            FetchRequest request = asFetchRequest(thisRequest).getRequestMap();
            TruffleJSONParser parser = new TruffleJSONParser(getContext());
            return toPromise(parser.parse(TruffleString.fromJavaStringUncached(request.consumeBody(), TruffleString.Encoding.UTF_8), getRealm()));
        }
    }

    public abstract static class JSFetchRequestBodyTextNode extends FetchRequestPrototypeBuiltins.JSFetchRequestOperation {
        public JSFetchRequestBodyTextNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected JSDynamicObject doOperation(Object thisRequest) {
            FetchRequest request = asFetchRequest(thisRequest).getRequestMap();
            String body = request.consumeBody();
            return toPromise(TruffleString.fromJavaStringUncached(body == null ? "" : body, TruffleString.Encoding.UTF_8));
        }
    }
}
