package com.oracle.truffle.js.runtime.builtins;

import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.Tuple;
import com.oracle.truffle.js.runtime.objects.JSNonProxyObject;

/**
 * A Tuple object is an exotic object that encapsulates a Tuple value.
 * This class implements this wrapper and thus serves as adapter from a primitive Tuple value to an object type.
 *
 * @see JSTuple
 * @see Tuple
 */
public final class JSTupleObject extends JSNonProxyObject {

    private final Tuple value;

    protected JSTupleObject(Shape shape, Tuple value) {
        super(shape);
        this.value = value;
    }

    public Tuple getTupleValue() {
        return value;
    }

    @Override
    public String getClassName() {
        return JSTuple.CLASS_NAME;
    }

    public static DynamicObject create(JSRealm realm, JSObjectFactory factory, Tuple value) {
        return factory.initProto(new JSTupleObject(factory.getShape(realm), value), realm);
    }
}
