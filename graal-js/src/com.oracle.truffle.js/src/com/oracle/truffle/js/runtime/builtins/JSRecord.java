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
package com.oracle.truffle.js.runtime.builtins;

import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.js.builtins.RecordFunctionBuiltins;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Record;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.JSShape;
import com.oracle.truffle.js.runtime.objects.PropertyDescriptor;
import com.oracle.truffle.js.runtime.util.DefinePropertyUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Implementation of Record Exotic Object
 *
 * @see <a href="https://tc39.es/proposal-record-tuple/#record-exotic-object">5.1.1 Record Exotic Objects</a>
 */
public class JSRecord extends JSNonProxy implements JSConstructorFactory.Default.WithFunctions, PrototypeSupplier {

    public static final String TYPE_NAME = "record";
    public static final String CLASS_NAME = "Record";
    public static final String PROTOTYPE_NAME = "Record.prototype";

    public static final JSRecord INSTANCE = new JSRecord();

    private JSRecord() {
    }

    public static DynamicObject create(JSContext context, Record value) {
        DynamicObject obj = JSRecordObject.create(context.getRealm(), context.getRecordFactory(), value);
        assert isJSRecord(obj);
        return context.trackAllocation(obj);
    }

    public static Record valueOf(DynamicObject obj) {
        assert isJSRecord(obj);
        return ((JSRecordObject) obj).getValue();
    }

    public static boolean isJSRecord(Object obj) {
        return obj instanceof JSRecordObject;
    }

    @Override
    public String getClassName(DynamicObject object) {
        return getClassName();
    }

    @Override
    public String getClassName() {
        return CLASS_NAME;
    }

    @Override
    public DynamicObject createPrototype(JSRealm realm, DynamicObject constructor) {
        JSContext context = realm.getContext();
        DynamicObject prototype = JSObjectUtil.createOrdinaryPrototypeObject(realm);

        JSObjectUtil.putConstructorProperty(context, prototype, constructor);
        // TODO: JSObjectUtil.putFunctionsFromContainer(realm, prototype, RecordPrototypeBuiltins.BUILTINS);
        JSObjectUtil.putToStringTag(prototype, CLASS_NAME);

        return prototype;
    }

    @Override
    public DynamicObject getIntrinsicDefaultProto(JSRealm realm) {
        return realm.getRecordPrototype();
    }

    public static JSConstructor createConstructor(JSRealm realm) {
        return INSTANCE.createConstructorAndPrototype(realm, RecordFunctionBuiltins.BUILTINS);
    }

    /**
     * Records aren't extensible, thus [[IsExtensible]] must always return false.
     *
     * TODO: see Ref below for ideas
     * @see JSModuleNamespace#makeInitialShape(com.oracle.truffle.js.runtime.JSContext)
     *
     * @see JSNonProxy#isExtensible(com.oracle.truffle.api.object.DynamicObject)
     */
    @Override
    public Shape makeInitialShape(JSContext context, DynamicObject prototype) {
        Shape initialShape = Shape.newBuilder()
                .layout(JSShape.getLayout(INSTANCE))
                .dynamicType(INSTANCE)
                .addConstantProperty(JSObject.HIDDEN_PROTO, prototype, 0)
                .shapeFlags(JSShape.NOT_EXTENSIBLE_FLAG)
                .build();

        assert !JSShape.isExtensible(initialShape);
        return initialShape;

        // TODO: remove commented out code below
        // return JSShape.newBuilder(context, INSTANCE, prototype)
        //         .shapeFlags(JSShape.NOT_EXTENSIBLE_FLAG)
        //         .build();
    }

    /**
     * Implementation of [[GetOwnProperty]].
     */
    @Override
    public PropertyDescriptor getOwnProperty(DynamicObject thisObj, Object key) {
        assert JSRuntime.isPropertyKey(key);
        if (key instanceof Symbol) {
            return null;
        }
        assert key instanceof String;
        Object value = recordGet(thisObj, key);
        if (value == null) {
            return null;
        }
        return PropertyDescriptor.createData(value, true, false, false);
    }

    /**
     * Implementation of [[DefineOwnProperty]].
     */
    // TODO: Not sure if this code is reachable according to proposal and ecma262 specs
    @Override
    public boolean defineOwnProperty(DynamicObject thisObj, Object key, PropertyDescriptor desc, boolean doThrow) {
        assert JSRuntime.isPropertyKey(key);
        if (key instanceof Symbol) {
            return DefinePropertyUtil.reject(doThrow, "object is not extensible");
        }
        if (desc.getIfHasWritable(false) || !desc.getIfHasEnumerable(true) || desc.getIfHasConfigurable(false)) {
            return DefinePropertyUtil.reject(doThrow, "object is not extensible");
        }
        Object value = recordGet(thisObj, key);
        if (value == null) {
            return DefinePropertyUtil.reject(doThrow, "object is not extensible");
        }
        assert desc.isDataDescriptor();
        if (desc.hasValue() && JSRuntime.isSameValue(desc.getValue(), value)) {
            return true;
        }
        return DefinePropertyUtil.reject(doThrow, "cannot redefine property");
    }

    /**
     * Implementation of [[HasProperty]].
     * This methods also implements the abstract operation 5.1.1.10 RecordHasProperty (R, P).
     */
    // TODO: Not sure if this code is reachable according to proposal and ecma262 specs
    @Override
    public boolean hasOwnProperty(DynamicObject thisObj, Object key) {
        assert JSRuntime.isPropertyKey(key);
        if (key instanceof Symbol) {
            return false;
        }
        assert key instanceof String;
        Record record = valueOf(thisObj);
        return record.hasKey((String) key);
    }

    /**
     * Implementation of [[Get]].
     *
     * @return the value associated with the given key
     * @see JSClass#get(com.oracle.truffle.api.object.DynamicObject, java.lang.Object)
     */
    @Override
    public Object getOwnHelper(DynamicObject store, Object thisObj, Object key, Node encapsulatingNode) {
        assert JSRuntime.isPropertyKey(key);
        if (key instanceof Symbol) {
            return null; // will get mapped to Undefined.instance
        }
        return recordGet(store, key);
    }

    /**
     * Implementation of [[Set]].
     *
     * @return always false
     */
    @Override
    public boolean set(DynamicObject thisObj, Object key, Object value, Object receiver, boolean isStrict, Node encapsulatingNode) {
        assert JSRuntime.isPropertyKey(key);
        return false;
    }

    @Override
    public boolean set(DynamicObject thisObj, long index, Object value, Object receiver, boolean isStrict, Node encapsulatingNode) {
        return set(thisObj, String.valueOf(index), value, receiver, isStrict, encapsulatingNode);
    }

    /**
     * Implementation of [[Delete]].
     */
    @Override
    public boolean delete(DynamicObject thisObj, Object key, boolean isStrict) {
        assert JSRuntime.isPropertyKey(key);
        if (key instanceof Symbol) {
            return true;
        }
        return !hasOwnProperty(thisObj, key);
    }

    @Override
    public boolean delete(DynamicObject thisObj, long index, boolean isStrict) {
        return delete(thisObj, String.valueOf(index), isStrict);
    }

    /**
     * Implementation of [[OwnPropertyKeys]].
     */
    @Override
    public List<Object> getOwnPropertyKeys(DynamicObject thisObj, boolean strings, boolean symbols) {
        if (!strings) {
            return Collections.emptyList();
        }
        assert isJSRecord(thisObj);
        Record value = valueOf(thisObj);
        return new ArrayList<>(value.getKeys());
    }

    /**
     * Implementation of the abstract operation RecordGet.
     *
     * @see <a href="https://tc39.es/proposal-record-tuple/#record-exotic-object">5.1.1.11 RecordGet (R, P)</a>
     */
    public Object recordGet(DynamicObject object, Object key) {
        assert key instanceof String;
        Record record = valueOf(object);
        Object value = record.get((String) key);
        assert JSRuntime.isJSPrimitive(value);
        return value;
    }

    @Override
    public boolean usesOrdinaryGetOwnProperty() {
        return false;
    }
}
