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

import com.ibm.icu.text.BreakIterator;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.access.CreateDataPropertyNode;
import com.oracle.truffle.js.runtime.Boundaries;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.JSOrdinary;
import com.oracle.truffle.js.runtime.builtins.intl.JSSegmenter;
import com.oracle.truffle.js.runtime.util.IntlUtil;

public class CreateSegmentDataObjectNode extends JavaScriptBaseNode {
    private final JSContext context;

    @Child CreateDataPropertyNode createSegmentPropertyNode;
    @Child CreateDataPropertyNode createIndexPropertyNode;
    @Child CreateDataPropertyNode createInputPropertyNode;
    @Child CreateDataPropertyNode createIsWordLikePropertyNode;

    protected CreateSegmentDataObjectNode(JSContext context) {
        super();
        this.context = context;
        createSegmentPropertyNode = CreateDataPropertyNode.create(context, IntlUtil.SEGMENT);
        createIndexPropertyNode = CreateDataPropertyNode.create(context, IntlUtil.INDEX);
        createInputPropertyNode = CreateDataPropertyNode.create(context, IntlUtil.INPUT);
    }

    public static CreateSegmentDataObjectNode create(JSContext context) {
        return new CreateSegmentDataObjectNode(context);
    }

    public DynamicObject execute(BreakIterator icuIterator, JSSegmenter.Granularity granularity, String string, int startIndex, int endIndex) {
        DynamicObject result = JSOrdinary.create(context, getRealm());
        createSegmentPropertyNode.executeVoid(result, Boundaries.substring(string, startIndex, endIndex));
        createIndexPropertyNode.executeVoid(result, startIndex);
        createInputPropertyNode.executeVoid(result, string);
        if (granularity == JSSegmenter.Granularity.WORD) {
            createIsWordLikeProperty(result, isWordLike(icuIterator));
        }
        return result;
    }

    private void createIsWordLikeProperty(DynamicObject target, boolean isWordLike) {
        if (createIsWordLikePropertyNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            createIsWordLikePropertyNode = insert(CreateDataPropertyNode.create(context, IntlUtil.IS_WORD_LIKE));
        }
        createIsWordLikePropertyNode.executeVoid(target, isWordLike);
    }

    @CompilerDirectives.TruffleBoundary
    private static boolean isWordLike(BreakIterator icuIterator) {
        return (icuIterator.getRuleStatus() != BreakIterator.WORD_NONE);
    }

}
