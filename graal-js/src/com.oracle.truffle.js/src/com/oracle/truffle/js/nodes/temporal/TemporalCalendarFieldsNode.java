/*
 * Copyright (c) 2022, 2022, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.access.GetMethodNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.runtime.Boundaries;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.TemporalConstants;
import com.oracle.truffle.js.runtime.util.TemporalUtil;

/**
 * Implementation of the Temporal calendarFields() operation.
 */
public abstract class TemporalCalendarFieldsNode extends JavaScriptBaseNode {

    private final JSContext ctx;
    @Child private GetMethodNode getMethodFieldsNode;
    @Child private JSFunctionCallNode callFieldsNode;
    private final ConditionProfile fieldsUndefined = ConditionProfile.createBinaryProfile();

    protected TemporalCalendarFieldsNode(JSContext ctx) {
        this.ctx = ctx;
        this.getMethodFieldsNode = GetMethodNode.create(ctx, TemporalConstants.FIELDS);
    }

    public static TemporalCalendarFieldsNode create(JSContext ctx) {
        return TemporalCalendarFieldsNodeGen.create(ctx);
    }

    public abstract List<TruffleString> execute(DynamicObject calendar, List<TruffleString> strings);

    @Specialization
    protected List<TruffleString> calendarFields(DynamicObject calendar, List<TruffleString> strings) {
        Object fields = getMethodFieldsNode.executeWithTarget(calendar);
        if (fieldsUndefined.profile(fields == Undefined.instance)) {
            return strings;
        } else {
            DynamicObject fieldsArray = JSArray.createConstant(ctx, getRealm(), Boundaries.listToArray(strings));
            fieldsArray = callFields(fields, calendar, new Object[]{fieldsArray});
            return TemporalUtil.iterableToListOfTypeString(fieldsArray);
        }
    }

    private DynamicObject callFields(Object fieldsFn, DynamicObject calendar, Object[] args) {
        if (callFieldsNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            callFieldsNode = insert(JSFunctionCallNode.createCall());
        }
        return TemporalUtil.toDynamicObject(callFieldsNode.executeCall(JSArguments.create(calendar, fieldsFn, args)));
    }
}
