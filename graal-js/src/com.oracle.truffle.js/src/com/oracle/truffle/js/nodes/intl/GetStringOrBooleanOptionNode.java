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
package com.oracle.truffle.js.nodes.intl;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.cast.JSToBooleanNode;
import com.oracle.truffle.js.nodes.cast.JSToStringNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.objects.Undefined;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * GetStringOrBooleanOption() operation.
 */
public abstract class GetStringOrBooleanOptionNode extends JavaScriptBaseNode {
    private final Set<String> values;
    private final Object trueValue;
    private final Object falsyValue;
    private final Object fallback;
    private final BranchProfile errorBranch;
    @Child PropertyGetNode propertyGetNode;
    @Child JSToStringNode toStringNode;
    @Child JSToBooleanNode toBooleanNode;

    protected GetStringOrBooleanOptionNode(JSContext context, String property, String[] values, Object trueValue, Object falsyValue, Object fallback) {
        this.values = new HashSet<>(Arrays.asList(values));
        this.trueValue = trueValue;
        this.falsyValue = falsyValue;
        this.fallback = fallback;
        this.errorBranch = BranchProfile.create();
        this.propertyGetNode = PropertyGetNode.create(property, false, context);
        this.toStringNode = JSToStringNode.create();
        this.toBooleanNode = JSToBooleanNode.create();
    }

    public abstract Object executeValue(Object options);

    public static GetStringOrBooleanOptionNode create(JSContext context, String property, String[] values, Object trueValue, Object falsyValue, Object fallback) {
        return GetStringOrBooleanOptionNodeGen.create(context, property, values, trueValue, falsyValue, fallback);
    }

    @Specialization
    public Object getOption(Object options) {
        Object value = propertyGetNode.getValue(options);
        if (value == Undefined.instance) {
            return fallback;
        }
        if (value == Boolean.TRUE) {
            return trueValue;
        }
        boolean valueBoolean = toBooleanNode.executeBoolean(value);
        if (propertyGetNode.getContext().getEcmaScriptVersion() < JSConfig.StagingECMAScriptVersion) {
            return valueBoolean ? trueValue : falsyValue;
        }
        if (!valueBoolean) {
            return falsyValue;
        }
        String stringValue = toStringNode.executeString(value);
        if (!isValid(stringValue)) {
            errorBranch.enter();
            throw Errors.createRangeErrorFormat("Value %s out of range for options property %s", this, stringValue, propertyGetNode.getKey());
        }
        return stringValue;
    }

    @TruffleBoundary
    private boolean isValid(String value) {
        return values.contains(value);
    }

}
