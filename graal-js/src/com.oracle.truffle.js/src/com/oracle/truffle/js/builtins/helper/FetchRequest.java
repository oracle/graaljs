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
import com.oracle.truffle.js.runtime.builtins.JSFetchRequest;
import com.oracle.truffle.js.runtime.objects.JSObject;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Set;

/**
 * The internal data structure for {@linkplain JSFetchRequest}.
 */
public class FetchRequest extends FetchBody {
    // RequestInit fields https://fetch.spec.whatwg.org/#requestinit
    private static final TruffleString METHOD = Strings.constant("method");
    private static final TruffleString REFERRER = Strings.constant("referrer");
    private static final TruffleString REFERRER_POLICY = Strings.constant("referrerPolicy");
    private static final TruffleString REDIRECT = Strings.constant("redirect");
    private static final TruffleString BODY = Strings.constant("body");
    private static final TruffleString HEADERS = Strings.constant("headers");
    // Non-standard init fields
    private static final TruffleString FOLLOW = Strings.constant("follow");

    private URL url;
    private String method;
    private String redirectMode;
    private int redirectCount;
    private int follow;
    public FetchHeaders headers;

    private String referrer;
    private boolean isReferrerUrl;
    private String referrerPolicy;

    public URL getUrl() {
        return url;
    }

    public void setUrl(URL url) {
        this.url = url;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public int getRedirectCount() {
        return redirectCount;
    }

    public String getRedirectMode() {
        return redirectMode;
    }

    public void incrementRedirectCount() {
        redirectCount++;
    }

    public int getFollow() {
        return follow;
    }

    public FetchHeaders getHeaders() {
        return headers;
    }

    public String getReferrer() {
        return referrer;
    }

    public boolean isReferrerUrl() {
        return isReferrerUrl;
    }

    public String getReferrerPolicy() {
        return referrerPolicy;
    }

    public void setReferrerPolicy(String referrerPolicy) {
        if (!FetchHttpConnection.VALID_POLICY.contains(referrerPolicy)) {
            throw Errors.createTypeError("Invalid referrerPolicy: " + referrerPolicy);
        }
        this.referrerPolicy = referrerPolicy;
    }

    public FetchRequest() { }

    /**
     * Par. 5.4, Request constructor step 5.
     *
     * @param input A string
     * @param init A RequestInit object, https://fetch.spec.whatwg.org/#requestinit
     */
    public FetchRequest(TruffleString input, JSObject init) {
        try {
            url = new URL(input.toString());
        } catch (MalformedURLException e) {
            throw Errors.createTypeError("Invalid URL: " + e.getMessage());
        }

        // Par. 5.4, Request constructor step 5.3
        if (url.getUserInfo() != null) {
            throw Errors.createTypeError(url + " is an url with embedded credentials");
        }

        setDefault();
        applyRequestInit(init);
    }

    public void applyRequestInit(JSObject init) {
        // Par. 5.4, Request constructor step 14
        if (JSObject.hasProperty(init, REFERRER)) {
            if (JSObject.get(init, REFERRER).equals("")) {
                // Par. 5.4, Request constructor step 14.2
                referrer = "no-referrer";
            } else {
                String initReferrer = JSToStringNode.create().executeString(JSObject.get(init, REFERRER)).toString();
                try {
                    // Par. 5.4, Request constructor step 14.3.1
                    URL parsedReferrer = new URL(initReferrer);
                    // Par. 5.4, Request constructor step 14.3.3
                    if (parsedReferrer.toString().matches("^about:(\\/\\/)?client$")) {
                        referrer = "client";
                    } else {
                        referrer = initReferrer;
                        isReferrerUrl = true;
                    }
                } catch (MalformedURLException e) {
                    throw Errors.createTypeError("Invalid URL" + initReferrer);
                }
            }
        }

        // Par. 5.4, Request constructor step 15
        if (JSObject.hasProperty(init, REFERRER_POLICY)) {
            String initReferrerPolicy = JSToStringNode.create().executeString(JSObject.get(init, REFERRER_POLICY)).toString();
            setReferrerPolicy(initReferrerPolicy);
        }

        // Par. 5.4, Request constructor step 22
        if (JSObject.hasProperty(init, REDIRECT)) {
            redirectMode = JSToStringNode.create().executeString(JSObject.get(init, REDIRECT)).toString();
        }

        // Par. 5.4, Request constructor step 25
        if (JSObject.hasProperty(init, METHOD)) {
            String initMethod = JSToStringNode.create().executeString(JSObject.get(init, METHOD)).toString();
            // Par. 5.4, Request constructor step 25.2
            if (Set.of("CONNECT", "TRACE", "TRACK").contains(initMethod.toUpperCase())) {
                throw Errors.createTypeError("Forbidden method name");
            }
            // Par. 5.4, Request constructor step 25.3, 25.4
            if (Set.of("DELETE", "GET", "HEAD", "OPTIONS", "POST", "PUT").contains(initMethod.toUpperCase())) {
                method = initMethod.toUpperCase();
            } else {
                method = initMethod;
            }
        }

        // Par. 5.4, Request constructor step 32
        if (JSObject.hasProperty(init, HEADERS)) {
            headers = new FetchHeaders(JSObject.get(init, HEADERS));
        }

        // Par. 5.4, Request constructor step 34
        if (JSObject.hasProperty(init, BODY) && (method.equals("GET") ||  method.equals("HEAD"))) {
            throw Errors.createTypeError("Request with GET/HEAD method cannot have body");
        }

        // Par. 5.4, Request constructor step 36
        if (JSObject.hasProperty(init, BODY)) {
            Object initBody = JSObject.get(init, BODY);
            setBody(initBody);
            // Par. 5.4, Request constructor step 36.4
            headers.set("Content-Type", getContentType());
        }

        if (JSObject.hasProperty(init, FOLLOW)) {
            follow = JSToInt32Node.create().executeInt(JSObject.get(init, BODY));
        }
    }

    private void setDefault() {
        method = "GET";
        headers = new FetchHeaders();
        body = null;
        referrer = "client";
        referrerPolicy = "";
        redirectMode = "follow";
        follow = 20;
    }

    public FetchRequest copy() {
        if (isBodyUsed()) {
            throw Errors.createError("cannot clone body after it is used");
        }

        FetchRequest clone = new FetchRequest();
        clone.url = this.url;
        clone.body = this.body;
        clone.method = this.method;
        clone.referrer = this.referrer;
        clone.referrerPolicy = this.referrerPolicy;
        clone.redirectMode = this.redirectMode;
        clone.follow = this.follow;
        clone.redirectCount = this.redirectCount;
        clone.headers = new FetchHeaders();
        this.headers.keys().forEach(k -> clone.headers.append(k, this.headers.get(k)));
        return clone;
    }
}
