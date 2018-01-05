/*
 * Copyright (c) 2012, 2014, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.cast;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.CompileRegexNode;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.JSRegExp;

/**
 * Implements a cast from an value to a RegExp Object, as defined by String.prototype.match and
 * String.prototype.search.
 */
public abstract class JSToRegExpNode extends JavaScriptBaseNode {
    protected final JSContext context;

    protected JSToRegExpNode(JSContext context) {
        this.context = context;
    }

    public abstract DynamicObject execute(Object target);

    public static JSToRegExpNode create(JSContext context) {
        return JSToRegExpNodeGen.create(context);
    }

    @Specialization(guards = "isJSRegExp(regExp)")
    protected DynamicObject returnRegExp(DynamicObject regExp) {
        return regExp;
    }

    @Specialization(guards = "!isJSRegExp(patternObj)")
    protected DynamicObject createRegExp(Object patternObj,
                    @Cached("createUndefinedToEmpty()") JSToStringNode toStringNode,
                    @Cached("create(context)") CompileRegexNode compileRegexNode) {
        String pattern = toStringNode.executeString(patternObj);
        TruffleObject regex = compileRegexNode.compile(pattern);
        return JSRegExp.create(context, regex);
    }
}
