/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.engine;

import java.util.*;

import javax.script.*;

import com.oracle.truffle.api.object.*;
import com.oracle.truffle.js.parser.env.*;
import com.oracle.truffle.js.runtime.*;
import com.oracle.truffle.js.runtime.builtins.*;
import com.oracle.truffle.js.runtime.objects.*;

public class GraalJSBindings implements Bindings {
    static final String GRAALJS_CONTEXT = "graaljs.context";
    private final JSContext jsContext;
    private final JSRealm realm;

    public GraalJSBindings(JSContext jsContext, JSRealm realm) {
        this.jsContext = jsContext;
        this.realm = realm;
        patchScriptEngineFileName(realm);
        addArgumentsProperty();
    }

    public JSContext getJSContext() {
        return jsContext;
    }

    public JSRealm getJSRealm() {
        return realm;
    }

    @Override
    public int size() {
        return getOwnProperties().size();
    }

    @Override
    public boolean isEmpty() {
        return getOwnProperties().isEmpty();
    }

    @Override
    public boolean containsValue(Object value) {
        return getOwnProperties().contains(value);
    }

    @Override
    public void clear() {
        DynamicObject globalObj = realm.getGlobalObject();
        for (String propName : keySet()) {
            JSObject.delete(globalObj, propName);
        }
    }

    @Override
    public Set<String> keySet() {
        return getOwnPropertiesMap().keySet();
    }

    @Override
    public Collection<Object> values() {
        return getOwnPropertiesMap().values();
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
        return getOwnPropertiesMap().entrySet();
    }

    @Override
    public Object put(String name, Object value) {
        boolean isArgs = name.equals(Environment.ARGUMENTS_NAME);
        // Specially handle 'arguments' property
        Object val = isArgs ? convertArgumentsProperty(value) : value;
        // Do not add properties called TRUFFLEJS_CONTEXT
        if (!name.equals(GRAALJS_CONTEXT)) {
            // The 'arguments' property is not visible
            return put(realm, name, val, isArgs ? JSAttributes.getDefaultNotEnumerable() : JSAttributes.getDefault());
        } else {
            return null;
        }
    }

    public Object put(String name, Object value, int flags) {
        return put(realm, name, value, flags);
    }

    @Override
    public void putAll(Map<? extends String, ? extends Object> toMerge) {
        for (Entry<? extends String, ? extends Object> entry : toMerge.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public boolean containsKey(Object key) {
        String name = JSRuntime.toString(key);
        return JSObject.hasOwnProperty(realm.getGlobalObject(), name);
    }

    @Override
    public Object get(Object key) {
        String name = JSRuntime.toString(key);
        Object retVal = JSObject.get(realm.getGlobalObject(), name);
        return retVal != Undefined.instance ? retVal : null;
    }

    @Override
    public Object remove(Object key) {
        String name = JSRuntime.toString(key);
        Object prevVal = get(key);
        JSObject.delete(realm.getGlobalObject(), name);
        return prevVal;
    }

    public Object convertArgumentsProperty(Object value) {
        return JSArray.isJSArray(value) ? value : JSArray.createConstant(jsContext, value instanceof Object[] ? (Object[]) value : new Object[]{value});
    }

    private static void patchScriptEngineFileName(JSRealm realm) {
        // ScriptEngine.FILENAME is a non-enumerable property.
        // This is required for compatibility with NashornScriptEngine.createNashornGlobal(...).
        DynamicObject globalObj = realm.getGlobalObject();
        if (!JSObject.hasOwnProperty(globalObj, ScriptEngine.FILENAME)) {
            putGlobalImpl(realm, ScriptEngine.FILENAME, Undefined.instance, JSAttributes.getDefaultNotEnumerable());
        }
    }

    private void addArgumentsProperty() {
        // Set the 'arguments' property to its default value
        put(Environment.ARGUMENTS_NAME, JSArguments.EMPTY_ARGUMENTS_ARRAY);
    }

    private List<String> getOwnProperties() {
        return JSObject.enumerableOwnNames(realm.getGlobalObject());
    }

    private Map<String, Object> getOwnPropertiesMap() {
        HashMap<String, Object> map = new HashMap<>();
        DynamicObject globalObj = realm.getGlobalObject();
        for (String key : getOwnProperties()) {
            Object pValue = JSObject.get(globalObj, key);
            map.put(key, pValue);
        }
        return map;
    }

    private static Object put(JSRealm realm, String name, Object value, int flags) {
        DynamicObject globalObj = realm.getGlobalObject();
        Object jsvalue = JSRuntime.toJSNull(value);
        if (JSObject.hasOwnProperty(globalObj, name)) {
            return setGlobalImpl(realm, name, jsvalue);
        } else {
            putGlobalImpl(realm, name, jsvalue, flags);
            return null;
        }
    }

    private static Object setGlobalImpl(JSRealm realm, String name, Object value) {
        DynamicObject globalObj = realm.getGlobalObject();
        Object prevVal = JSObject.get(globalObj, name);
        JSObject.set(globalObj, name, value);
        return prevVal;
    }

    private static void putGlobalImpl(JSRealm realm, String name, Object value, int flags) {
        DynamicObject globalObj = realm.getGlobalObject();
        JSObjectUtil.putDataProperty(realm.getContext(), globalObj, name, value, flags);
    }
}
