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
package com.oracle.truffle.js.nodes.intl;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.cast.JSToNumberNode;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.objects.Undefined;

public abstract class GetNumberOptionNode extends JavaScriptBaseNode {

    private final Number maximum;
    @Child PropertyGetNode propertyGetNode;

    @Child JSToNumberNode toNumberNode = JSToNumberNode.create();

    protected GetNumberOptionNode(JSContext context, String property, Number maximum) {
        this.maximum = maximum;
        this.propertyGetNode = PropertyGetNode.create(property, false, context);
    }

    public abstract Number executeValue(Object options, Number minimum, Number fallback);

    public static GetNumberOptionNode create(JSContext context, String property, Number maximum) {
        return GetNumberOptionNodeGen.create(context, property, maximum);
    }

    protected Number makeFinalSelection(Number value, Number minimum) {
        ensureSelectedValueIsValid(value, minimum);
        return value.intValue();
    }

    @TruffleBoundary
    private void ensureSelectedValueIsValid(Number value, Number minimum) {
        if (JSRuntime.isNaN(value) || minimum.doubleValue() > value.doubleValue() || maximum.doubleValue() < value.doubleValue()) {
            throw Errors.createRangeError(String.format("invalid value %s found where only values between %s and %s are allowed", value, minimum, maximum));
        }
    }

    @Specialization(guards = "!isUndefined(options)")
    public Number getOption(Object options, Number minimum, Number fallback) {
        Object propertyValue = propertyGetNode.getValue(options);
        if (propertyValue == Undefined.instance) {
            return fallback;
        }
        return makeFinalSelection(toOptionType(propertyValue), minimum);
    }

    @Specialization(guards = "isUndefined(options)")
    @SuppressWarnings("unused")
    public Number getOptionFromUndefined(Object options, Number minimum, Number fallback) {
        return fallback;
    }

    protected Number toOptionType(Object propertyValue) {
        return toNumberNode.executeNumber(propertyValue);
    }
}
