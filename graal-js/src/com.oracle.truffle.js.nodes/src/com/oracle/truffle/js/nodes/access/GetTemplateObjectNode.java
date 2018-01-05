/*
 * Copyright (c) 2015, 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.access;

import java.util.*;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.object.*;
import com.oracle.truffle.js.nodes.*;
import com.oracle.truffle.js.runtime.*;
import com.oracle.truffle.js.runtime.builtins.*;
import com.oracle.truffle.js.runtime.objects.*;

/**
 * ES6 12.2.9.3 Runtime Semantics: GetTemplateObject(templateLiteral).
 */
public class GetTemplateObjectNode extends JavaScriptNode {
    private final JSContext context;
    @Child private ArrayLiteralNode rawStrings;
    @Child private ArrayLiteralNode cookedStrings;
    @Child private RealmNode realmNode;

    @CompilationFinal private DynamicObject cachedTemplate;

    protected GetTemplateObjectNode(JSContext context, ArrayLiteralNode rawStrings, ArrayLiteralNode cookedStrings) {
        this.context = context;
        this.rawStrings = rawStrings;
        this.cookedStrings = cookedStrings;
        this.realmNode = RealmNode.create(context);
    }

    public static GetTemplateObjectNode create(JSContext context, ArrayLiteralNode rawStrings, ArrayLiteralNode cookedStrings) {
        return new GetTemplateObjectNode(context, rawStrings, cookedStrings);
    }

    @Override
    public DynamicObject execute(VirtualFrame frame) {
        if (cachedTemplate != null) {
            return cachedTemplate;
        }

        CompilerDirectives.transferToInterpreterAndInvalidate();
        DynamicObject template = cookedStrings.executeDynamicObject(frame);
        DynamicObject rawObj = rawStrings.executeDynamicObject(frame);
        JSObject.setIntegrityLevel(rawObj, true);
        JSObjectUtil.putDataProperty(context, template, "raw", rawObj, JSAttributes.notConfigurableNotEnumerableNotWritable());
        JSObject.setIntegrityLevel(template, true);

        JSRealm realm = realmNode.execute(frame);
        Map<List<String>, DynamicObject> templateRegistry = realm.getTemplateRegistry();
        Object[] rawStringArray = JSArray.toArray(rawObj);
        List<String> key = Arrays.asList(Arrays.copyOf(rawStringArray, rawStringArray.length, String[].class));
        DynamicObject cached = templateRegistry.get(key);
        if (cached == null) {
            realm.getTemplateRegistry().put(key, cached = template);
        }
        return cachedTemplate = cached;
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return new GetTemplateObjectNode(context, cloneUninitialized(rawStrings), cloneUninitialized(cookedStrings));
    }
}
