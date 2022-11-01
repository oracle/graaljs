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

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.builtins.helper.FetchHttpConnection;
import com.oracle.truffle.js.builtins.helper.FetchResponse;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSFetchResponse;
import com.oracle.truffle.js.builtins.FetchResponseFunctionBuiltinsFactory.FetchErrorNodeGen;
import com.oracle.truffle.js.builtins.FetchResponseFunctionBuiltinsFactory.FetchJsonNodeGen;
import com.oracle.truffle.js.builtins.FetchResponseFunctionBuiltinsFactory.FetchRedirectNodeGen;
import com.oracle.truffle.js.runtime.builtins.JSFetchResponseObject;
import com.oracle.truffle.js.runtime.objects.JSObject;

import java.net.MalformedURLException;
import java.net.URL;

public class FetchResponseFunctionBuiltins extends JSBuiltinsContainer.SwitchEnum<FetchResponseFunctionBuiltins.FetchResponseFunction> {
    public static final JSBuiltinsContainer BUILTINS = new FetchResponseFunctionBuiltins();

    protected FetchResponseFunctionBuiltins() {
        super(JSFetchResponse.CLASS_NAME, FetchResponseFunction.class);
    }

    public enum FetchResponseFunction implements BuiltinEnum<FetchResponseFunction> {
        error(0),
        json(2),
        redirect(2);

        private final int length;

        FetchResponseFunction(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, FetchResponseFunction builtinEnum) {
        switch (builtinEnum) {
            case error:
                return FetchErrorNodeGen.create(context, builtin, args().fixedArgs(0).createArgumentNodes(context));
            case json:
                return FetchJsonNodeGen.create(context, builtin, args().fixedArgs(2).createArgumentNodes(context));
            case redirect:
                return FetchRedirectNodeGen.create(context, builtin, args().fixedArgs(2).createArgumentNodes(context));
        }
        return null;
    }

    /**
     * https://fetch.spec.whatwg.org/#dom-response-error.
     */
    public abstract static class FetchErrorNode extends JSBuiltinNode {
        public FetchErrorNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected JSFetchResponseObject error() {
            FetchResponse response = new FetchResponse();
            response.setStatus(0);
            response.setStatusText("");
            response.setType(FetchResponse.FetchResponseType.error);
            return JSFetchResponse.create(getContext(), getRealm(), response);
        }
    }

    /**
     * https://fetch.spec.whatwg.org/#dom-response-json.
     */
    public abstract static class FetchJsonNode extends JSBuiltinNode {
        public FetchJsonNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected JSFetchResponseObject json(JSObject data, JSObject init) {
            FetchResponse response = new FetchResponse(data, init);
            response.setContentType("application/json");
            return JSFetchResponse.create(getContext(), getRealm(), response);
        }
    }

    /**
     * https://fetch.spec.whatwg.org/#dom-response-redirect.
     */
    public abstract static class FetchRedirectNode extends JSBuiltinNode {
        public FetchRedirectNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected JSFetchResponseObject redirect(TruffleString url, int status) {
            FetchResponse response = new FetchResponse();

            URL parsedURL;
            try {
                parsedURL = new URL(url.toString());
            } catch (MalformedURLException exp) {
                throw Errors.createTypeError(exp.getMessage());
            }

            if (!FetchHttpConnection.REDIRECT_STATUS.contains(status)) {
                throw Errors.createRangeError("Failed to execute redirect on response: Invalid status code");
            }

            response.setStatus(status);
            response.headers.set("Location", parsedURL.toString());
            return JSFetchResponse.create(getContext(), getRealm(), response);
        }
    }
}
