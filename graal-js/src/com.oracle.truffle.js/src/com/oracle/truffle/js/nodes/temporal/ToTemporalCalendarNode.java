/*
 * Copyright (c) 2021, 2021, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.js.runtime.util.TemporalConstants.CALENDAR;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.access.IsObjectNode;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.cast.JSToStringNode;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalCalendar;
import com.oracle.truffle.js.runtime.builtins.temporal.TemporalCalendar;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.util.TemporalUtil;

/**
 * Implementation of ToTemporalCalendar() operation.
 */
public abstract class ToTemporalCalendarNode extends JavaScriptBaseNode {

    private final ConditionProfile isObjectProfile = ConditionProfile.createBinaryProfile();
    private final ConditionProfile isCalendarProfile = ConditionProfile.createBinaryProfile();
    private final ConditionProfile hasCalendarProfile = ConditionProfile.createBinaryProfile();
    private final ConditionProfile hasCalendar2Profile = ConditionProfile.createBinaryProfile();
    private final BranchProfile parseBranch = BranchProfile.create();

    private final JSContext context;
    @Child private PropertyGetNode getCalendarPropertyNode;

    protected ToTemporalCalendarNode(JSContext context) {
        this.context = context;
    }

    public static ToTemporalCalendarNode create(JSContext context) {
        return ToTemporalCalendarNodeGen.create(context);
    }

    public abstract DynamicObject executeDynamicObject(Object value);

    @Specialization
    public DynamicObject toTemporalCalendar(Object itemParam,
                    @Cached("create()") IsObjectNode isObjectNode,
                    @Cached("create()") JSToStringNode toStringNode) {
        Object item = itemParam;
        if (isObjectProfile.profile(isObjectNode.executeBoolean(item))) {
            DynamicObject itemObj = (DynamicObject) item;
            if (isCalendarProfile.profile(item instanceof TemporalCalendar)) {
                return ((TemporalCalendar) item).getCalendar();
            }
            if (hasCalendarProfile.profile(!JSObject.hasProperty(itemObj, CALENDAR))) {
                return itemObj;
            }
            item = getCalendarProperty(itemObj);
            if (hasCalendar2Profile.profile(isObjectNode.executeBoolean(item) && !JSObject.hasProperty((DynamicObject) item, CALENDAR))) {
                return (DynamicObject) item;
            }
        }
        String string = toStringNode.executeString(item);
        if (!JSTemporalCalendar.isBuiltinCalendar(string)) {
            parseBranch.enter();
            string = TemporalUtil.parseTemporalCalendarString(string);
        }
        return JSTemporalCalendar.create(context, string);
    }

    private Object getCalendarProperty(DynamicObject obj) {
        if (getCalendarPropertyNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            getCalendarPropertyNode = insert(PropertyGetNode.create(CALENDAR, false, context));
        }
        return getCalendarPropertyNode.getValue(obj);
    }
}
