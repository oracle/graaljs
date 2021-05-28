package com.oracle.truffle.js.nodes.access;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.objects.JSAttributes;

public abstract class CreateNonEnumerableDataPropertyNode extends JavaScriptBaseNode {
    protected final JSContext context;
    protected final Object key;
    @Child protected IsJSObjectNode isObject;

    protected CreateNonEnumerableDataPropertyNode(JSContext context, Object key) {
        this.context = context;
        this.key = key;
        this.isObject = IsJSObjectNode.create();
    }

    public static CreateNonEnumerableDataPropertyNode create(JSContext context, Object key) {
        return CreateNonEnumerableDataPropertyNodeGen.create(context, key);
    }

    public abstract void executeVoid(Object object, Object value);

    @Specialization(guards = {"context.getPropertyCacheLimit() > 0", "isObject.executeBoolean(object)"})
    protected static void doCached(Object object, Object value,
                                   @Cached("makeDefinePropertyCache()") PropertySetNode propertyCache) {
        propertyCache.setValue(object, value);
    }

    @Specialization(guards = {"context.getPropertyCacheLimit() == 0", "isJSObject(object)"})
    protected final void doUncached(DynamicObject object, Object value) {
        JSRuntime.createNonEnumerableDataPropertyOrThrow(object, key, value);
    }

    @Specialization(guards = "!isJSObject(object)")
    protected final void doNonObject(Object object, Object value) {
        throw Errors.createTypeErrorNotAnObject(object, this);
    }

    protected final PropertySetNode makeDefinePropertyCache() {
        return PropertySetNode.createImpl(key, false, context, true, true, JSAttributes.getDefaultNotEnumerable());
    }
}
