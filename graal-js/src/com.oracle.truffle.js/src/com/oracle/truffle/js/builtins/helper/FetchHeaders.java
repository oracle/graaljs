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

import com.oracle.truffle.js.nodes.cast.JSToStringNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.builtins.JSFetchHeaders;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.Undefined;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Internal data structure for {@linkplain JSFetchHeaders}.
 */
public class FetchHeaders {

    // sorted by header name, https://fetch.spec.whatwg.org/#convert-header-names-to-a-sorted-lowercase-set
    private final SortedMap<String, List<String>> headers;

    public FetchHeaders() {
        headers = new TreeMap<>();
    }

    public FetchHeaders(Map<String, List<String>> init) {
        headers = new TreeMap<>();
        if (!init.isEmpty()) {
            init.keySet().stream().filter(Objects::nonNull).forEach(k -> headers.put(k.toLowerCase(), init.get(k)));
        }
    }

    public FetchHeaders(Object init) {
        headers = new TreeMap<>();

        if (init == Null.instance || init == Undefined.instance) {
            return;
        }

        if (JSFetchHeaders.isJSFetchHeaders(init)) {
            FetchHeaders initHeaders = JSFetchHeaders.getInternalData(init);
            initHeaders.keys().forEach(k -> append(k, initHeaders.get(k)));
            return;
        }

        if (JSObject.isJSObject(init)) {
            JSObject obj = (JSObject) init;
            Object[] keys = JSObject.getKeyArray(obj);

            Arrays.stream(keys).forEach(k -> {
                JSToStringNode toString = JSToStringNode.create();
                String name = toString.executeString(k).toString();
                String value = toString.executeString(JSObject.get(obj, k)).toString();
                append(name, value);
            });
        } else {
            throw Errors.createTypeError("Failed to construct 'Headers': The provided value has incorrect type");
        }
    }

    public void append(String name, String value) {
        name = name.toLowerCase();
        String normalizedValue = value.trim();
        validateHeaderName(name);
        validateHeaderValue(name, normalizedValue);
        headers.computeIfAbsent(name, v -> new ArrayList<>()).add(normalizedValue);
    }

    public void delete(String name) {
        name = name.toLowerCase();
        validateHeaderName(name);
        headers.remove(name);
    }

    public String get(String name) {
        name = name.toLowerCase();
        validateHeaderName(name);
        List<String> match = headers.get(name);

        if (match == null) {
            return null;
        }

        return String.join(", ", match);
    }

    public boolean has(String name) {
        name = name.toLowerCase();
        validateHeaderName(name);
        return headers.containsKey(name);
    }

    public void set(String name, String value) {
        name = name.toLowerCase();
        String normalizedValue = value.trim();
        validateHeaderName(name);
        validateHeaderValue(name, normalizedValue);
        headers.computeIfAbsent(name, v -> new ArrayList<>()).clear();
        headers.get(name).add(normalizedValue);
    }

    public Map<String, String> entries() {
        return headers.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> get(e.getKey())));
    }

    public Set<String> keys() {
        return headers.keySet();
    }

    /**
     * validated by field-name token production as in https://datatracker.ietf.org/doc/html/rfc7230#section-3.2.
     */
    private void validateHeaderName(String name) {
        if (name.isEmpty()
                || name.isBlank()
                //token regex: https://github.com/nodejs/node/blob/4d8674b50f6050d5dad649dbd32ce60cbd24f362/lib/_http_common.js#L204
                || !name.matches("^[\\^_`\\w\\-!#$%&'*+.|~]+$")) {
            throw Errors.createTypeError("Header name must be a valid HTTP token: [ " + name + "]");
        }
    }

    /**
     * validated by field-name token production as in https://datatracker.ietf.org/doc/html/rfc7230#section-3.2.
     */
    private void validateHeaderValue(String name, String value) {
        // field-vchar regex: https://github.com/nodejs/node/blob/4d8674b50f6050d5dad649dbd32ce60cbd24f362/lib/_http_common.js#L214
        long matches = Pattern.compile("[^\\x09\\x20-\\x7e\\x80-\\xff]").matcher(value).results().count();
        if (matches > 0) {
            throw Errors.createTypeError("Invalid character in header field [ " + name + ": " + value + "]");
        }
    }
}
