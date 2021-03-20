package com.oracle.truffle.js.runtime.builtins;

import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.js.builtins.TupleFunctionBuiltins;
import com.oracle.truffle.js.builtins.TuplePrototypeBuiltins;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.Tuple;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;

public final class JSTuple extends JSNonProxy implements JSConstructorFactory.Default.WithFunctions, PrototypeSupplier {

    public static final String TYPE_NAME = "tuple";
    public static final String CLASS_NAME = "Tuple";
    public static final String PROTOTYPE_NAME = "Tuple.prototype";

    public static final JSTuple INSTANCE = new JSTuple();

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
        return prototype;
    }

    @Override
    public Shape makeInitialShape(JSContext context, DynamicObject prototype) {
        Shape initialShape = JSObjectUtil.getProtoChildShape(prototype, INSTANCE, context);
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
}
