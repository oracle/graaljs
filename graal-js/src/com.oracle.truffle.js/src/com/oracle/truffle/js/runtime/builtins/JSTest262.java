/*
 * Copyright (c) 2018, 2024, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.runtime.builtins;

import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.builtins.PerformanceBuiltins;
import com.oracle.truffle.js.builtins.testing.Test262Builtins;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.objects.JSAttributes;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;

public final class JSTest262 {

    public static final TruffleString CLASS_NAME = Strings.constant("Test262");
    public static final TruffleString GLOBAL_PROPERTY_NAME = Strings.constant("$262");
    public static final TruffleString ABSTRACT_MODULE_SOURCE = Strings.constant("AbstractModuleSource");

    private JSTest262() {
    }

    public static JSObject create(JSRealm realm) {
        JSObject obj = JSOrdinary.createInit(realm);
        JSObjectUtil.putDataProperty(obj, Strings.CREATE_REALM, realm.lookupFunction(Test262Builtins.BUILTINS, Strings.CREATE_REALM), JSAttributes.getDefaultNotEnumerable());
        JSObjectUtil.putDataProperty(obj, Strings.DETACH_ARRAY_BUFFER, realm.lookupFunction(Test262Builtins.BUILTINS, Strings.DETACH_ARRAY_BUFFER), JSAttributes.getDefaultNotEnumerable());
        JSObjectUtil.putDataProperty(obj, Strings.EVAL_SCRIPT, realm.lookupFunction(Test262Builtins.BUILTINS, Strings.EVAL_SCRIPT), JSAttributes.getDefaultNotEnumerable());
        JSObjectUtil.putDataProperty(obj, Strings.GLOBAL, realm.getGlobalObject(), JSAttributes.getDefaultNotEnumerable());
        JSObjectUtil.putDataProperty(obj, Strings.GC, realm.lookupFunction(Test262Builtins.BUILTINS, Strings.GC), JSAttributes.getDefaultNotEnumerable());
        JSObject agent = createAgent(realm);
        JSObjectUtil.putDataProperty(obj, Strings.AGENT, agent, JSAttributes.getDefaultNotEnumerable());
        if (realm.getContextOptions().isSourcePhaseImports()) {
            JSObjectUtil.putDataProperty(obj, ABSTRACT_MODULE_SOURCE, realm.getAbstractModuleSourceConstructor(), JSAttributes.getDefaultNotEnumerable());
        }
        return obj;
    }

    private static JSObject createAgent(JSRealm realm) {
        JSObject agent = JSOrdinary.createInit(realm);
        JSObjectUtil.putDataProperty(agent, Strings.START, realm.lookupFunction(Test262Builtins.BUILTINS, Strings.AGENT_START), JSAttributes.getDefaultNotEnumerable());
        JSObjectUtil.putDataProperty(agent, Strings.BROADCAST, realm.lookupFunction(Test262Builtins.BUILTINS, Strings.AGENT_BROADCAST), JSAttributes.getDefaultNotEnumerable());
        JSObjectUtil.putDataProperty(agent, Strings.GET_REPORT, realm.lookupFunction(Test262Builtins.BUILTINS, Strings.AGENT_GET_REPORT), JSAttributes.getDefaultNotEnumerable());
        JSObjectUtil.putDataProperty(agent, Strings.SLEEP, realm.lookupFunction(Test262Builtins.BUILTINS, Strings.AGENT_SLEEP), JSAttributes.getDefaultNotEnumerable());
        JSObjectUtil.putDataProperty(agent, Strings.MONOTONIC_NOW, realm.lookupFunction(PerformanceBuiltins.BUILTINS, Strings.NOW), JSAttributes.getDefaultNotEnumerable());
        JSObjectUtil.putDataProperty(agent, Strings.RECEIVE_BROADCAST, realm.lookupFunction(Test262Builtins.BUILTINS, Strings.AGENT_RECEIVE_BROADCAST), JSAttributes.getDefaultNotEnumerable());
        JSObjectUtil.putDataProperty(agent, Strings.REPORT, realm.lookupFunction(Test262Builtins.BUILTINS, Strings.AGENT_REPORT), JSAttributes.getDefaultNotEnumerable());
        JSObjectUtil.putDataProperty(agent, Strings.LEAVING, realm.lookupFunction(Test262Builtins.BUILTINS, Strings.AGENT_LEAVING), JSAttributes.getDefaultNotEnumerable());
        return agent;
    }
}
