/*
 * Copyright (c) 2024, 2025, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NeverDefault;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalCalendarHolder;
import com.oracle.truffle.js.runtime.util.IntlUtil;
import com.oracle.truffle.js.runtime.util.TemporalUtil;

/**
 * Implementation of ToTemporalCalendarIdentifier() operation.
 */
@GenerateUncached
@ImportStatic(Strings.class)
public abstract class ToTemporalCalendarIdentifierNode extends JavaScriptBaseNode {

    protected ToTemporalCalendarIdentifierNode() {
    }

    @NeverDefault
    public static ToTemporalCalendarIdentifierNode create() {
        return ToTemporalCalendarIdentifierNodeGen.create();
    }

    @NeverDefault
    public static ToTemporalCalendarIdentifierNode getUncached() {
        return ToTemporalCalendarIdentifierNodeGen.getUncached();
    }

    public abstract TruffleString executeString(Object temporalCalendarLike);

    @Specialization
    protected TruffleString doJSTemporalCalendarHolder(JSTemporalCalendarHolder temporalCalendarLike) {
        return temporalCalendarLike.getCalendar();
    }

    @Specialization
    public TruffleString doString(TruffleString temporalCalendarLike,
                    @Cached TruffleString.ToJavaStringNode toJavaString,
                    @Cached TruffleString.FromJavaStringNode fromJavaString) {
        TruffleString identifier = TemporalUtil.parseTemporalCalendarString(temporalCalendarLike);
        String identifierJLS = toJavaString.execute(identifier);
        return Strings.fromJavaString(fromJavaString, IntlUtil.canonicalizeCalendar(identifierJLS));
    }

    @Specialization(guards = {"!isString(temporalCalendarLike)", "!isJSTemporalCalendarHolder(temporalCalendarLike)"})
    public TruffleString doOther(Object temporalCalendarLike) {
        throw Errors.createTypeErrorNotAString(temporalCalendarLike);
    }

}
