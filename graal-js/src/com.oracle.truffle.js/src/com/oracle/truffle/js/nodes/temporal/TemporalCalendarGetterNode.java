/*
 * Copyright (c) 2022, 2024, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.nodes.temporal;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.cast.JSToIntegerThrowOnInfinityNode;
import com.oracle.truffle.js.nodes.cast.JSToStringNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * Implementation of the Temporal calendarDay() et al operations.
 */
public abstract class TemporalCalendarGetterNode extends JavaScriptBaseNode {
    @Child private CalendarMethodsRecordLookupNode getMethodNode;
    @Child private JSFunctionCallNode callNode;
    @Child private JSToIntegerThrowOnInfinityNode toIntegerThrowOnInfinityNode;
    @Child private JSToStringNode toStringNode;

    protected TemporalCalendarGetterNode() {
        this.callNode = JSFunctionCallNode.createCall();
    }

    public abstract Object execute(Object calendar, JSDynamicObject dateLike, TruffleString name);

    public final Number executeInteger(Object calendar, JSDynamicObject dateLike, TruffleString name) {
        if (toIntegerThrowOnInfinityNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            toIntegerThrowOnInfinityNode = insert(JSToIntegerThrowOnInfinityNode.create());
        }
        return (Number) toIntegerThrowOnInfinityNode.execute(execute(calendar, dateLike, name));
    }

    public final TruffleString executeString(Object calendar, JSDynamicObject dateLike, TruffleString name) {
        if (toStringNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            toStringNode = insert(JSToStringNode.create());
        }
        return toStringNode.executeString(execute(calendar, dateLike, name));
    }

    @Specialization
    protected Object calendarGetter(Object calendarSlotValue, JSDynamicObject dateLike, TruffleString name,
                    @Cached ToTemporalCalendarObjectNode toCalendarObject,
                    @Cached InlinedBranchProfile errorBranch) {
        Object calendar = toCalendarObject.execute(calendarSlotValue);
        Object fn = getMethod(calendar, name);
        Object result = callNode.executeCall(JSArguments.create(calendar, fn, dateLike));

        if (result == Undefined.instance) {
            errorBranch.enter(this);
            throw Errors.createRangeError("expected a value.");
        }
        return result;
    }

    private Object getMethod(Object calendar, TruffleString name) {
        if (getMethodNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            this.getMethodNode = insert(CalendarMethodsRecordLookupNode.create(name));
        }
        assert getMethodNode.getKey().equals(name);
        return getMethodNode.execute(calendar);
    }
}
