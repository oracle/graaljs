/*
 * Copyright (c) 2017, 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.scriptengine;

import java.util.AbstractMap;
import java.util.Map;
import java.util.Set;

import javax.script.Bindings;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.TypeLiteral;
import org.graalvm.polyglot.Value;

final class GraalJSBindings extends AbstractMap<String, Object> implements Bindings {

    private static final TypeLiteral<Map<String, Object>> STRING_MAP = new TypeLiteral<Map<String, Object>>() {
    };

    private final Context context;
    private final Map<String, Object> global;
    private final Value deleteProperty;

    GraalJSBindings(Context context) {
        this.context = context;
        this.global = GraalJSScriptEngine.evalInternal(context, "this").as(STRING_MAP);
        this.deleteProperty = GraalJSScriptEngine.evalInternal(context, "(function(obj, prop) {delete obj[prop]})");
    }

    @Override
    public Object put(String name, Object v) {
        return global.put(name, v);
    }

    @Override
    public void clear() {
        for (String key : global.keySet()) {
            remove(key);
        }
    }

    @Override
    public Object get(Object key) {
        return global.get(key);
    }

    @Override
    public Object remove(Object key) {
        Object prev = get(key);
        deleteProperty.execute(global, key);
        return prev;
    }

    public Context getContext() {
        return context;
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
        return global.entrySet();
    }

}
