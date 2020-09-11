/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.builtins.PerformanceBuiltins;
import com.oracle.truffle.js.builtins.Test262Builtins;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.objects.JSAttributes;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;

public final class JSTest262 {

    public static final String CLASS_NAME = "Test262";
    public static final String GLOBAL_PROPERTY_NAME = "$262";

    private JSTest262() {
    }

    public static DynamicObject create(JSRealm realm) {
        DynamicObject obj = JSOrdinary.createInit(realm);
        JSObjectUtil.putDataProperty(obj, "createRealm", realm.lookupFunction(Test262Builtins.BUILTINS, "createRealm"), JSAttributes.getDefaultNotEnumerable());
        JSObjectUtil.putDataProperty(obj, "detachArrayBuffer", realm.lookupFunction(Test262Builtins.BUILTINS, "detachArrayBuffer"), JSAttributes.getDefaultNotEnumerable());
        JSObjectUtil.putDataProperty(obj, "evalScript", realm.lookupFunction(Test262Builtins.BUILTINS, "evalScript"), JSAttributes.getDefaultNotEnumerable());
        JSObjectUtil.putDataProperty(obj, "global", realm.getGlobalObject(), JSAttributes.getDefaultNotEnumerable());
        JSObjectUtil.putDataProperty(obj, "gc", realm.lookupFunction(Test262Builtins.BUILTINS, "gc"), JSAttributes.getDefaultNotEnumerable());
        DynamicObject agent = createAgent(realm);
        JSObjectUtil.putDataProperty(obj, "agent", agent, JSAttributes.getDefaultNotEnumerable());
        return obj;
    }

    private static DynamicObject createAgent(JSRealm realm) {
        DynamicObject agent = JSOrdinary.createInit(realm);
        JSObjectUtil.putDataProperty(agent, "start", realm.lookupFunction(Test262Builtins.BUILTINS, "agentStart"), JSAttributes.getDefaultNotEnumerable());
        JSObjectUtil.putDataProperty(agent, "broadcast", realm.lookupFunction(Test262Builtins.BUILTINS, "agentBroadcast"), JSAttributes.getDefaultNotEnumerable());
        JSObjectUtil.putDataProperty(agent, "getReport", realm.lookupFunction(Test262Builtins.BUILTINS, "agentGetReport"), JSAttributes.getDefaultNotEnumerable());
        JSObjectUtil.putDataProperty(agent, "sleep", realm.lookupFunction(Test262Builtins.BUILTINS, "agentSleep"), JSAttributes.getDefaultNotEnumerable());
        JSObjectUtil.putDataProperty(agent, "monotonicNow", realm.lookupFunction(PerformanceBuiltins.BUILTINS, "now"), JSAttributes.getDefaultNotEnumerable());
        JSObjectUtil.putDataProperty(agent, "receiveBroadcast", realm.lookupFunction(Test262Builtins.BUILTINS, "agentReceiveBroadcast"), JSAttributes.getDefaultNotEnumerable());
        JSObjectUtil.putDataProperty(agent, "report", realm.lookupFunction(Test262Builtins.BUILTINS, "agentReport"), JSAttributes.getDefaultNotEnumerable());
        JSObjectUtil.putDataProperty(agent, "leaving", realm.lookupFunction(Test262Builtins.BUILTINS, "agentLeaving"), JSAttributes.getDefaultNotEnumerable());
        return agent;
    }
}
