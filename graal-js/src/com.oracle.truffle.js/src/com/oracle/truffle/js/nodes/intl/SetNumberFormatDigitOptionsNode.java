/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.intl.JSNumberFormat;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.IntlUtil;

/**
 * SetNumberFormatDigitOptions() operation.
 */
public abstract class SetNumberFormatDigitOptionsNode extends JavaScriptBaseNode {
    @Child GetNumberOptionNode getMinIntDigitsOption;
    @Child PropertyGetNode getMinFracDigitsOption;
    @Child PropertyGetNode getMaxFracDigitsOption;
    @Child PropertyGetNode getMinSignificantDigitsOption;
    @Child PropertyGetNode getMaxSignificantDigitsOption;
    @Child DefaultNumberOptionNode getMnsdDNO;
    @Child DefaultNumberOptionNode getMxsdDNO;
    @Child DefaultNumberOptionNode getMnfdDNO;
    @Child DefaultNumberOptionNode getMxfdDNO;
    @Child GetStringOptionNode getRoundingPriorityOption;
    private final BranchProfile errorBranch = BranchProfile.create();

    protected SetNumberFormatDigitOptionsNode(JSContext context) {
        this.getMinIntDigitsOption = GetNumberOptionNode.create(context, IntlUtil.MINIMUM_INTEGER_DIGITS);
        this.getMinFracDigitsOption = PropertyGetNode.create(IntlUtil.MINIMUM_FRACTION_DIGITS, context);
        this.getMaxFracDigitsOption = PropertyGetNode.create(IntlUtil.MAXIMUM_FRACTION_DIGITS, context);
        this.getMinSignificantDigitsOption = PropertyGetNode.create(IntlUtil.MINIMUM_SIGNIFICANT_DIGITS, context);
        this.getMaxSignificantDigitsOption = PropertyGetNode.create(IntlUtil.MAXIMUM_SIGNIFICANT_DIGITS, context);
        this.getMnsdDNO = DefaultNumberOptionNode.create();
        this.getMxsdDNO = DefaultNumberOptionNode.create();
        this.getMnfdDNO = DefaultNumberOptionNode.create();
        this.getMxfdDNO = DefaultNumberOptionNode.create();
        this.getRoundingPriorityOption = GetStringOptionNode.create(context, IntlUtil.ROUNDING_PRIORITY, new String[]{IntlUtil.AUTO, IntlUtil.MORE_PRECISION, IntlUtil.LESS_PRECISION}, IntlUtil.AUTO);
    }

    public static SetNumberFormatDigitOptionsNode create(JSContext context) {
        return SetNumberFormatDigitOptionsNodeGen.create(context);
    }

    public abstract Object execute(JSNumberFormat.BasicInternalState intlObj, Object options, int mnfdDefault, int mxfdDefault, boolean compactNotation);

    @Specialization
    public Object setNumberFormatDigitOptions(JSNumberFormat.BasicInternalState intlObj, Object options, int mnfdDefault, int mxfdDefault, boolean compactNotation) {
        int mnid = getMinIntDigitsOption.executeInt(options, 1, 21, 1);
        Object mnfdValue = getMinFracDigitsOption.getValue(options);
        Object mxfdValue = getMaxFracDigitsOption.getValue(options);
        Object mnsdValue = getMinSignificantDigitsOption.getValue(options);
        Object mxsdValue = getMaxSignificantDigitsOption.getValue(options);
        intlObj.setMinimumIntegerDigits(mnid);
        String roundingPriority = getRoundingPriorityOption.executeValue(options);
        boolean hasSd = mnsdValue != Undefined.instance || mxsdValue != Undefined.instance;
        boolean hasFd = mnfdValue != Undefined.instance || mxfdValue != Undefined.instance;
        boolean autoRoundingPriority = IntlUtil.AUTO.equals(roundingPriority);
        boolean needSd = hasSd || !autoRoundingPriority;
        boolean needFd = (!hasSd && (hasFd || !compactNotation)) || !autoRoundingPriority;
        if (needSd) {
            if (hasSd) {
                int mnsd = getMnsdDNO.executeInt(mnsdValue, 1, 21, 1);
                int mxsd = getMxsdDNO.executeInt(mxsdValue, mnsd, 21, 21);
                intlObj.setMinimumSignificantDigits(mnsd);
                intlObj.setMaximumSignificantDigits(mxsd);
            } else {
                intlObj.setMinimumSignificantDigits(1);
                intlObj.setMaximumSignificantDigits(21);
            }
        }
        if (needFd) {
            if (hasFd) {
                int mnfd = getMnfdDNO.executeInt(mnfdValue, 0, 20, -1);
                int mxfd = getMxfdDNO.executeInt(mxfdValue, 0, 20, -1);
                if (mnfd == -1) {
                    mnfd = Math.min(mnfdDefault, mxfd);
                } else if (mxfd == -1) {
                    mxfd = Math.max(mxfdDefault, mnfd);
                } else if (mnfd > mxfd) {
                    errorBranch.enter();
                    throw Errors.createRangeError("minimumFractionDigits higher than maximumFractionDigits");
                }
                intlObj.setMinimumFractionDigits(mnfd);
                intlObj.setMaximumFractionDigits(mxfd);
            } else {
                intlObj.setMinimumFractionDigits(mnfdDefault);
                intlObj.setMaximumFractionDigits(mxfdDefault);
            }
        }
        if (needSd || needFd) {
            if (IntlUtil.MORE_PRECISION.equals(roundingPriority) || IntlUtil.LESS_PRECISION.equals(roundingPriority)) {
                intlObj.setRoundingType(roundingPriority);
            } else if (hasSd) {
                intlObj.setRoundingType(IntlUtil.SIGNIFICANT_DIGITS);
            } else {
                intlObj.setRoundingType(IntlUtil.FRACTION_DIGITS);
            }
        } else {
            intlObj.setRoundingType(IntlUtil.MORE_PRECISION);
            intlObj.setMinimumFractionDigits(0);
            intlObj.setMaximumFractionDigits(0);
            intlObj.setMinimumSignificantDigits(1);
            intlObj.setMaximumSignificantDigits(2);
        }
        return Undefined.instance;
    }

}
