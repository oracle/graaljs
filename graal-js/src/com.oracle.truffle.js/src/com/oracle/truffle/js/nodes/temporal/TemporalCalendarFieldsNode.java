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

import java.util.List;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.InlinedConditionProfile;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.runtime.Boundaries;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.temporal.CalendarMethodsRecord;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.util.TemporalUtil;

/**
 * Implementation of the Temporal calendarFields() operation.
 */
public abstract class TemporalCalendarFieldsNode extends JavaScriptBaseNode {

    @Child private JSFunctionCallNode callFieldsNode;

    protected TemporalCalendarFieldsNode() {
    }

    public abstract List<TruffleString> execute(CalendarMethodsRecord calendarRec, List<TruffleString> strings);

    @Specialization
    protected List<TruffleString> calendarFields(CalendarMethodsRecord calendarRec, List<TruffleString> strings,
                    @Cached InlinedConditionProfile fieldsUndefined) {
        if (calendarRec.receiver() instanceof TruffleString) {
            return strings;
        }
        JSDynamicObject fieldsArray = JSArray.createConstant(getLanguage().getJSContext(), getRealm(), Boundaries.listToArray(strings));
        fieldsArray = callFields(calendarRec.fields(), calendarRec.receiver(), new Object[]{fieldsArray});
        return TemporalUtil.iterableToListOfTypeString(fieldsArray);
    }

    private JSDynamicObject callFields(Object fieldsFn, Object calendar, Object[] args) {
        if (callFieldsNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            callFieldsNode = insert(JSFunctionCallNode.createCall());
        }
        return TemporalUtil.toDynamicObject(callFieldsNode.executeCall(JSArguments.create(calendar, fieldsFn, args)));
    }
}
