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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.cast.JSToObjectNode;
import com.oracle.truffle.js.nodes.intl.InitializeCollatorNodeGen.CreateOptionsObjectNodeGen;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.JSCollator;
import com.oracle.truffle.js.runtime.builtins.JSUserObject;

/*
 * https://tc39.github.io/ecma402/#sec-initializecollator
 */
public abstract class InitializeCollatorNode extends JavaScriptBaseNode {

    @Child JSToCanonicalizedLocaleListNode toCanonicalizedLocaleListNode;
    @Child CreateOptionsObjectNode createOptionsNode;

    @Child GetStringOptionNode getUsageOption;
    @Child GetStringOptionNode getLocaleMatcherOption;
    @Child GetBooleanOptionNode getNumericOption;
    @Child GetStringOptionNode getCaseFirstOption;
    @Child GetStringOptionNode getSensitivityOption;
    @Child GetBooleanOptionNode getIgnorePunctuationOption;

    protected InitializeCollatorNode(JSContext context) {
        this.toCanonicalizedLocaleListNode = JSToCanonicalizedLocaleListNode.create(context);
        this.createOptionsNode = CreateOptionsObjectNodeGen.create(context);
        this.getUsageOption = GetStringOptionNode.create(context, "usage", new String[]{"sort", "search"}, "sort");
        this.getLocaleMatcherOption = GetStringOptionNode.create(context, "localeMatcher", new String[]{"lookup", "best fit"}, "best fit");
        this.getNumericOption = GetBooleanOptionNode.create(context, "numeric", null);
        this.getCaseFirstOption = GetStringOptionNode.create(context, "caseFirst", new String[]{"upper", "lower", "false"}, null);
        this.getSensitivityOption = GetStringOptionNode.create(context, "sensitivity", new String[]{"base", "accent", "case", "variant"}, null);
        this.getIgnorePunctuationOption = GetBooleanOptionNode.create(context, "ignorePunctuation", false);
    }

    public abstract DynamicObject executeInit(DynamicObject collator, Object locales, Object options);

    public static InitializeCollatorNode createInitalizeCollatorNode(JSContext context) {
        return InitializeCollatorNodeGen.create(context);
    }

    @Specialization
    public DynamicObject initializeCollator(DynamicObject collatorObj, Object localesArg, Object optionsArg) {

        JSCollator.InternalState state = JSCollator.getInternalState(collatorObj);

        String[] locales = toCanonicalizedLocaleListNode.executeLanguageTags(localesArg);
        DynamicObject options = createOptionsNode.execute(optionsArg);
        String usage = getUsageOption.executeValue(options);
        String optLocaleMatcher = getLocaleMatcherOption.executeValue(options);
        Boolean optkn = getNumericOption.executeValue(options);
        String optkf = getCaseFirstOption.executeValue(options);
        String sensitivity = getSensitivityOption.executeValue(options);
        Boolean ignorePunctuation = getIgnorePunctuationOption.executeValue(options);

        JSCollator.initializeCollator(state, locales, usage, optLocaleMatcher, optkn, optkf, sensitivity, ignorePunctuation);

        return collatorObj;
    }

    public abstract static class CreateOptionsObjectNode extends JavaScriptBaseNode {

        @Child JSToObjectNode toObjectNode;
        private final JSContext context;

        public JSContext getContext() {
            return context;
        }

        public CreateOptionsObjectNode(JSContext context) {
            super();
            this.context = context;
        }

        public abstract DynamicObject execute(Object opts);

        @SuppressWarnings("unused")
        @Specialization(guards = "isUndefined(opts)")
        public DynamicObject fromUndefined(Object opts) {
            return JSUserObject.createWithPrototype(null, getContext());
        }

        @Specialization(guards = "!isUndefined(opts)")
        public DynamicObject fromOtherThenUndefined(Object opts) {
            return toDynamicObject(opts);
        }

        private DynamicObject toDynamicObject(Object o) {
            if (toObjectNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toObjectNode = insert(JSToObjectNode.createToObject(getContext()));
            }
            return (DynamicObject) toObjectNode.executeTruffleObject(o);
        }
    }
}
