/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
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
