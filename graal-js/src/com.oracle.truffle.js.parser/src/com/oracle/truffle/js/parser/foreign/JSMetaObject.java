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
package com.oracle.truffle.js.parser.foreign;

import java.util.Objects;

import org.graalvm.collections.EconomicMap;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.TruffleLanguage.Env;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.KeyInfo;
import com.oracle.truffle.api.interop.MessageResolution;
import com.oracle.truffle.api.interop.Resolve;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.nodes.Node;

@MessageResolution(receiverType = JSMetaObject.class)
public final class JSMetaObject implements TruffleObject {
    private final String type;
    private final String subtype;
    private final String className;
    private final String description;
    private final Env env;
    private EconomicMap<String, String> map;

    public JSMetaObject(String type, String subtype, String className, String description, TruffleLanguage.Env env) {
        this.type = Objects.requireNonNull(type);
        this.subtype = subtype;
        this.className = className;
        this.description = description;
        this.env = env;
    }

    public String getType() {
        return type;
    }

    public String getClassName() {
        return className;
    }

    EconomicMap<String, String> getMap() {
        if (map == null) {
            map = EconomicMap.create();
            map.put("type", type);
            if (subtype != null) {
                map.put("subtype", subtype);
            }
            if (className != null) {
                map.put("className", className);
            }
            if (description != null) {
                map.put("description", description);
            }
        }
        return map;
    }

    static boolean isInstance(TruffleObject object) {
        return object instanceof JSMetaObject;
    }

    @Override
    public ForeignAccess getForeignAccess() {
        return JSMetaObjectForeign.ACCESS;
    }

    @Resolve(message = "READ")
    abstract static class ReadNode extends Node {
        @TruffleBoundary
        static Object access(JSMetaObject meta, String key) {
            String value = meta.getMap().get(key);
            if (value == null) {
                throw UnknownIdentifierException.raise(key);
            }
            return value;
        }
    }

    @Resolve(message = "KEYS")
    abstract static class KeysNode extends Node {
        @TruffleBoundary
        static Object access(JSMetaObject meta) {
            String[] keys = new String[meta.getMap().size()];
            int i = 0;
            for (String key : meta.getMap().getKeys()) {
                keys[i++] = key;
            }
            return meta.env.asGuestValue(keys);
        }
    }

    @Resolve(message = "KEY_INFO")
    abstract static class KeyInfoNode extends Node {
        @TruffleBoundary
        static int access(JSMetaObject meta, String key) {
            return meta.getMap().containsKey(key) ? KeyInfo.READABLE : KeyInfo.NONE;
        }
    }
}
