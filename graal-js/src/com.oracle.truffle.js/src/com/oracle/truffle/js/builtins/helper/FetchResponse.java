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
package com.oracle.truffle.js.builtins.helper;

import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.nodes.cast.JSToInt32Node;
import com.oracle.truffle.js.nodes.cast.JSToStringNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.JSFetchResponse;
import com.oracle.truffle.js.runtime.objects.JSObject;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * The internal data structure for {@linkplain JSFetchResponse}.
 */
public class FetchResponse extends FetchBody {
    // ResponseInit fields https://fetch.spec.whatwg.org/#responseinit
    private static final TruffleString STATUS = Strings.constant("status");
    private static final TruffleString STATUS_TEXT = Strings.constant("statusText");
    private static final TruffleString HEADERS = Strings.constant("headers");
    //non-standard fields
    private static final TruffleString URL = Strings.constant("url");

    private URL url;
    private int status;
    private String statusText;
    private int counter;
    private FetchResponseType type;
    public FetchHeaders headers;

    public enum FetchResponseType {
        basic, cors, default_, error, opaque, opaqueredirect
    }

    public FetchResponse() {
        setDefault();
    }

    public FetchResponse(Object body, JSObject init) {
        setDefault();
        setBody(body);

        headers = new FetchHeaders(init);

        if (init.hasProperty(HEADERS)) {
            headers = new FetchHeaders(JSObject.get(init, HEADERS));
        }

        if (body != null && !headers.has("Content-Type")) {
            headers.append("Content-Type", getContentType());
        }

        if (init.hasProperty(STATUS)) {
            status = JSToInt32Node.create().executeInt(JSObject.get(init, STATUS));
        }

        if (init.hasProperty(STATUS_TEXT)) {
            statusText = JSToStringNode.create().executeString(JSObject.get(init, STATUS_TEXT)).toString();
        }

        if (init.hasProperty(URL)) {
            String initUrl = JSToStringNode.create().executeString(JSObject.get(init, URL)).toString();
            try {
                url = new URL(initUrl);
            } catch (MalformedURLException e) {
                throw Errors.createTypeError("Invalid url: " + initUrl);
            }
        }
    }

    private void setDefault() {
        status = 200;
        statusText = "";
        headers = new FetchHeaders();
        type = FetchResponseType.default_;
        counter = 0;
        body = null;
    }

    public URL getUrl() {
        return url;
    }

    public void setUrl(URL url) {
        this.url = url;
    }

    public FetchResponseType getType() {
        return type;
    }

    public void setType(FetchResponseType type) {
        this.type = type;
    }

    public void setUrl(TruffleString str) throws MalformedURLException {
        this.url = new URL(str.toString());
    }

    public int getStatus() {
        return status;
    }

    public boolean getOk() {
        return status >= HttpURLConnection.HTTP_OK && status < 300;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getStatusText() {
        return statusText;
    }

    public void setStatusText(String statusText) {
        this.statusText = statusText == null ? "" : statusText;
    }

    public void setCounter(int counter) {
        this.counter = counter;
    }

    public boolean getRedirected() {
        return counter > 0;
    }

    public FetchHeaders getHeaders() {
        return headers;
    }

    public void setHeaders(FetchHeaders headers) {
        this.headers = headers;
    }

    public FetchResponse copy() {
        if (isBodyUsed()) {
            throw Errors.createError("cannot clone body after it is used");
        }

        FetchResponse clone = new FetchResponse();
        clone.url = this.url;
        clone.status = this.status;
        clone.statusText = this.statusText;
        clone.type = this.type;
        clone.counter = this.counter;
        clone.body = this.body;
        clone.headers = new FetchHeaders();
        this.headers.keys().forEach(k -> clone.headers.append(k, this.headers.get(k)));
        return clone;
    }
}
