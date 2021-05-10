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

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.js.builtins.TupleFunctionBuiltins;
import com.oracle.truffle.js.builtins.TuplePrototypeBuiltins;
import com.oracle.truffle.js.builtins.TuplePrototypeGetterBuiltins;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.Tuple;
import com.oracle.truffle.js.runtime.objects.JSAttributes;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.JSShape;
import com.oracle.truffle.js.runtime.objects.PropertyDescriptor;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.DefinePropertyUtil;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.oracle.truffle.js.runtime.objects.JSObjectUtil.putDataProperty;

/**
 * Implementation of the Tuple (exotic) object internal methods and properties.
 * See <a href="https://tc39.es/proposal-record-tuple/#tuple-exotic-object">Records & Tuples Proposal: 5.1.2 Tuple Exotic Objects</a>
 *
 * @see JSTupleObject
 * @see Tuple
 */
public final class JSTuple extends JSNonProxy implements JSConstructorFactory.Default.WithFunctions, PrototypeSupplier {

    public static final String TYPE_NAME = "tuple";
    public static final String CLASS_NAME = "Tuple";
    public static final String PROTOTYPE_NAME = "Tuple.prototype";

    public static final JSTuple INSTANCE = new JSTuple();

    public static final String LENGTH = "length";

    private JSTuple() {
    }

    public static DynamicObject create(JSContext context, Tuple value) {
        DynamicObject obj = JSTupleObject.create(context.getRealm(), context.getTupleFactory(), value);
        assert isJSTuple(obj);
        return context.trackAllocation(obj);
    }

    public static Tuple valueOf(DynamicObject obj) {
        assert isJSTuple(obj);
        return ((JSTupleObject) obj).getTupleValue();
    }

    @Override
    public DynamicObject createPrototype(final JSRealm realm, DynamicObject ctor) {
        JSContext context = realm.getContext();
        DynamicObject prototype = JSObjectUtil.createOrdinaryPrototypeObject(realm);

        JSObjectUtil.putConstructorProperty(context, prototype, ctor);
        JSObjectUtil.putFunctionsFromContainer(realm, prototype, TuplePrototypeBuiltins.BUILTINS);
        JSObjectUtil.putToStringTag(prototype, CLASS_NAME);

        // Sets the Tuple.prototype.length accessor property.
        JSObjectUtil.putBuiltinAccessorProperty(prototype, LENGTH, realm.lookupAccessor(TuplePrototypeGetterBuiltins.BUILTINS, LENGTH), JSAttributes.notConfigurableNotEnumerableNotWritable());

        // The initial value of the @@iterator property is the same function object as the initial value of the Tuple.prototype.values property.
        putDataProperty(context, prototype, Symbol.SYMBOL_ITERATOR, JSDynamicObject.getOrNull(prototype, "values"), JSAttributes.getDefaultNotEnumerable());

        return prototype;
    }

    /**
     * Tuples aren't extensible, thus [[IsExtensible]] must always return false.
     * @see JSNonProxy#isExtensible(com.oracle.truffle.api.object.DynamicObject)
     */
    @Override
    public Shape makeInitialShape(JSContext context, DynamicObject prototype) {
        Shape initialShape = JSShape.newBuilder(context, INSTANCE, null)
                .addConstantProperty(JSObject.HIDDEN_PROTO, prototype, 0)
                .shapeFlags(JSShape.NOT_EXTENSIBLE_FLAG)
                .build();

        assert !JSShape.isExtensible(initialShape);
        return initialShape;
    }

    public static JSConstructor createConstructor(JSRealm realm) {
        return INSTANCE.createConstructorAndPrototype(realm, TupleFunctionBuiltins.BUILTINS);
    }

    public static boolean isJSTuple(Object obj) {
        return obj instanceof JSTupleObject;
    }

    @Override
    public String getClassName() {
        return CLASS_NAME;
    }

    @Override
    public String getClassName(DynamicObject object) {
        return getClassName();
    }

    @Override
    public DynamicObject getIntrinsicDefaultProto(JSRealm realm) {
        return realm.getTuplePrototype();
    }

    /**
     * [[GetOwnProperty]]
     */
    @Override
    public PropertyDescriptor getOwnProperty(DynamicObject thisObj, Object key) {
        assert JSRuntime.isPropertyKey(key);
        if (key instanceof Symbol) {
            return null;
        }
        assert key instanceof String;
        Object numericIndex = JSRuntime.canonicalNumericIndexString((String) key);
        if (numericIndex == Undefined.instance) {
            return null;
        }
        assert numericIndex instanceof Number;
        Object value = tupleGet(thisObj, (Number) numericIndex);
        if (value == null) {
            return null;
        }
        return PropertyDescriptor.createData(value, true, false, false);
    }

    /**
     * [[DefineOwnProperty]]
     */
    public boolean defineOwnProperty(DynamicObject thisObj, Object key, PropertyDescriptor desc, boolean doThrow) {
        assert JSRuntime.isPropertyKey(key);
        if (key instanceof Symbol) {
            return DefinePropertyUtil.reject(doThrow, "object is not extensible");
        }
        assert key instanceof String;
        if (desc.getIfHasWritable(false) || !desc.getIfHasEnumerable(true) || desc.getIfHasConfigurable(false)) {
            return DefinePropertyUtil.reject(doThrow, "object is not extensible");
        }
        Object numericIndex = JSRuntime.canonicalNumericIndexString((String) key);
        if (numericIndex == Undefined.instance) {
            return DefinePropertyUtil.reject(doThrow, "key could not be mapped to a numeric index");
        }
        assert numericIndex instanceof Number;
        Object current = tupleGet(thisObj, (Number) numericIndex);
        if (current == null) {
            return DefinePropertyUtil.reject(doThrow, "object is not extensible");
        }
        if (desc.isAccessorDescriptor()) {
            return DefinePropertyUtil.reject(doThrow, "object is not extensible");
        }
        if (desc.hasValue() && JSRuntime.isSameValue(desc.getValue(), current)) {
            return true;
        }
        return DefinePropertyUtil.reject(doThrow, "cannot redefine property");
    }

    /**
     * [[HasProperty]]
     */
    @Override
    public boolean hasOwnProperty(DynamicObject thisObj, Object key) {
        assert JSRuntime.isPropertyKey(key);
        if (key instanceof Symbol) {
            return false;
        }
        assert key instanceof String;
        Object numericIndex = JSRuntime.canonicalNumericIndexString((String) key);
        if (numericIndex == Undefined.instance) {
            return false;
        }
        assert numericIndex instanceof Number;
        return isValidTupleIndex(thisObj, (Number) numericIndex);
    }

    /**
     * [[Get]]
     */
    @TruffleBoundary
    @Override
    public final Object getOwnHelper(DynamicObject store, Object thisObj, Object key, Node encapsulatingNode) {
        assert JSRuntime.isPropertyKey(key);
        if (!(key instanceof Symbol)) {
            assert key instanceof String;
            Object numericIndex = JSRuntime.canonicalNumericIndexString((String) key);
            if (numericIndex != Undefined.instance) {
                assert numericIndex instanceof Number;
                return tupleGet(store, (Number) numericIndex);
            }
        }
        return super.getOwnHelper(store, thisObj, key, encapsulatingNode);
    }

    @TruffleBoundary
    @Override
    public Object getOwnHelper(DynamicObject store, Object thisObj, long index, Node encapsulatingNode) {
        return tupleGet(store, index);
    }

    /**
     * [[Set]]
     */
    @Override
    public boolean set(DynamicObject thisObj, Object key, Object value, Object receiver, boolean isStrict, Node encapsulatingNode) {
        assert JSRuntime.isPropertyKey(key);
        return false;
    }

    @Override
    public boolean set(DynamicObject thisObj, long index, Object value, Object receiver, boolean isStrict, Node encapsulatingNode) {
        return false;
    }

    /**
     * [[Delete]]
     */
    @Override
    public boolean delete(DynamicObject thisObj, Object key, boolean isStrict) {
        assert JSRuntime.isPropertyKey(key);
        if (key instanceof Symbol) {
            return true;
        }
        assert key instanceof String;
        Object numericIndex = JSRuntime.canonicalNumericIndexString((String) key);
        if (numericIndex == Undefined.instance) {
            return true;
        }
        assert numericIndex instanceof Number;
        return !isValidTupleIndex(thisObj, (Number) numericIndex);
    }

    @Override
    public boolean delete(DynamicObject thisObj, long index, boolean isStrict) {
        return !isValidTupleIndex(thisObj, index);
    }

    /**
     * [[OwnPropertyKeys]]
     */
    @TruffleBoundary
    @Override
    public List<Object> getOwnPropertyKeys(DynamicObject thisObj, boolean strings, boolean symbols) {
        Tuple tuple = valueOf(thisObj);
        int len = tuple.getArraySizeInt();
        if (len == 0 || !strings) {
            return Collections.emptyList();
        }
        String[] keys = new String[len];
        for (int i = 0; i < len; i++) {
            keys[i] = Integer.toString(i);
        }
        return Arrays.asList(keys);
    }

    private Object tupleGet(DynamicObject T, Number numericIndex) {
        assert isJSTuple(T);
        assert JSRuntime.isInteger(numericIndex);
        Tuple tuple = valueOf(T);
        if (!isValidTupleIndex(T, numericIndex)) {
            return null;
        }
        long longIndex = JSRuntime.toLong(numericIndex);
        return tuple.getElement(longIndex);
    }

    private boolean isValidTupleIndex(DynamicObject T, Number numericIndex) {
        assert isJSTuple(T);
        assert JSRuntime.isInteger(numericIndex);
        if (numericIndex.equals(-0.0)) {
            return false;
        }
        long longIndex = JSRuntime.toLong(numericIndex);
        return longIndex >= 0 && longIndex < valueOf(T).getArraySize();
    }

    @Override
    public boolean usesOrdinaryGetOwnProperty() {
        return false;
    }
}
