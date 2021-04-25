package com.oracle.truffle.js.runtime.builtins;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.js.builtins.TupleFunctionBuiltins;
import com.oracle.truffle.js.builtins.TuplePrototypeBuiltins;
import com.oracle.truffle.js.builtins.TuplePrototypeGetterBuiltins;
import com.oracle.truffle.js.runtime.Boundaries;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.Tuple;
import com.oracle.truffle.js.runtime.objects.JSAttributes;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;

import static com.oracle.truffle.js.runtime.objects.JSObjectUtil.putDataProperty;

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

    @Override
    public Shape makeInitialShape(JSContext context, DynamicObject prototype) {
        return JSObjectUtil.getProtoChildShape(prototype, INSTANCE, context);
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

    @TruffleBoundary
    @Override
    public final Object getOwnHelper(DynamicObject store, Object thisObj, Object key, Node encapsulatingNode) {
        long idx = JSRuntime.propertyKeyToArrayIndex(key);
        if (JSRuntime.isArrayIndex(idx)) {
            return getOwnHelper(store, thisObj, idx, encapsulatingNode);
        }
        return super.getOwnHelper(store, thisObj, key, encapsulatingNode);
    }

    @TruffleBoundary
    @Override
    public Object getOwnHelper(DynamicObject store, Object thisObj, long index, Node encapsulatingNode) {
        assert isJSTuple(store);
        Tuple tuple = ((JSTupleObject) store).getTupleValue();
        if (tuple.hasElement(index)) {
            return tuple.getElement(index);
        }
        return super.getOwnHelper(store, thisObj, Boundaries.stringValueOf(index), encapsulatingNode);
    }

    @TruffleBoundary
    @Override
    public boolean hasProperty(DynamicObject thisObj, long index) {
        assert isJSTuple(thisObj);
        Tuple tuple = ((JSTupleObject) thisObj).getTupleValue();
        return tuple.hasElement(index);
    }

    // TODO: override [[GetOwnProperty]], [[Delete]], etc.
}
