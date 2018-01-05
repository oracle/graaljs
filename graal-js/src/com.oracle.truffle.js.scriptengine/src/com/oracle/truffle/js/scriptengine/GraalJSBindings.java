/*
 * Copyright (c) 2017, 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.scriptengine;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.script.Bindings;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

public final class GraalJSBindings extends AbstractMap<String, Object> implements Bindings {

    private final Context context;
    private final Value global;
    private final EntrySet entrySet;
    private final Value deleteProperty;
    private final Value nullValue;

    GraalJSBindings(Context context) {
        this.context = context;
        this.global = GraalJSScriptEngine.evalInternal(context, "this");
        this.entrySet = new EntrySet();
        this.deleteProperty = GraalJSScriptEngine.evalInternal(context, "(function(obj, prop) {delete obj[prop]})");
        this.nullValue = GraalJSScriptEngine.evalInternal(context, "null");
    }

    @Override
    public Set<java.util.Map.Entry<String, Object>> entrySet() {
        return entrySet;
    }

    @Override
    public Object put(String name, Object v) {
        Value prev = global.getMember(name);
        global.putMember(name, convertHostToGuest(v));
        return convertGuestToHost(prev);
    }

    public Context getContext() {
        return context;
    }

    Object convertHostToGuest(Object v) {
        if (v == null) {
            return nullValue;
        }
        return v;
    }

    static Object convertGuestToHost(Value value) {
        if (value.isHostObject()) {
            return value.asHostObject();
        } else if (value.isBoolean()) {
            return value.asBoolean();
        } else if (value.isNumber()) {
            if (value.fitsInInt()) {
                return value.asInt();
            } else if (value.fitsInLong()) {
                return value.asLong();
            } else if (value.fitsInDouble()) {
                return value.asDouble();
            }
        } else if (value.isString()) {
            return value.asString();
        } else if (value.isNull()) {
            return null;
        }
        return value;
    }

    private class EntrySet extends AbstractSet<Map.Entry<String, Object>> {

        @Override
        public Iterator<java.util.Map.Entry<String, Object>> iterator() {
            return new EntryIterator(global);
        }

        @Override
        public int size() {
            return global.getMemberKeys().size();
        }

    }

    private class EntryIterator implements Iterator<Map.Entry<String, Object>> {

        private final Value value;
        private final Iterator<String> keyIterator;
        private String currentKey;

        EntryIterator(Value value) {
            this.keyIterator = value.getMemberKeys().iterator();
            this.value = value;
        }

        @Override
        public boolean hasNext() {
            return keyIterator.hasNext();
        }

        @Override
        public Map.Entry<String, Object> next() {
            return new Entry(currentKey = keyIterator.next());
        }

        @Override
        public void remove() {
            if (currentKey == null) {
                throw new IllegalStateException();
            }
            deleteProperty.execute(value, currentKey);
        }
    }

    private class Entry implements Map.Entry<String, Object> {

        private final String key;

        Entry(String key) {
            this.key = key;
        }

        @Override
        public String getKey() {
            return key;
        }

        @Override
        public Object getValue() {
            return convertGuestToHost(global.getMember(key));
        }

        @Override
        public Object setValue(Object v) {
            Object prev = getValue();
            global.putMember(key, convertHostToGuest(v));
            return prev;
        }

    }

}
