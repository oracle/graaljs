package com.oracle.truffle.js.nodes.decorators;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.objects.Accessor;
import com.oracle.truffle.js.runtime.objects.JSAttributes;
import com.oracle.truffle.js.runtime.objects.PropertyDescriptor;

public abstract class PrivateClassElementAddNode extends JavaScriptBaseNode {
    protected final JSContext context;

    protected PrivateClassElementAddNode(JSContext context) {this.context = context; }

    public static PrivateClassElementAddNode create(JSContext context) { return PrivateClassElementAddNodeGen.create(context); }

    public abstract void execute(Object target, Object key, PropertyDescriptor desc);

    @Specialization(guards = {"isJSObject(target)"}, limit = "3")
    void doAdd(DynamicObject target, HiddenKey key, PropertyDescriptor desc,
               @CachedLibrary("target")DynamicObjectLibrary access) {
        if(!access.containsKey(target, key)) {
            int attributes = JSAttributes.fromConfigurableEnumerableWritable(desc.getConfigurable(), desc.getEnumerable(), desc.getWritable());
            if(desc.isDataDescriptor()) {
                access.putWithFlags(target, key, desc.getValue(), attributes);
            } else {
                assert desc.isAccessorDescriptor();
                Accessor a = new Accessor((DynamicObject) desc.getGet(), (DynamicObject) desc.getSet());
                access.putWithFlags(target, key, a, attributes);
            }
        } else {
            duplicate(key);
        }
    }

    @TruffleBoundary
    private Object duplicate(HiddenKey key) {
        throw Errors.createTypeErrorCannotAddPrivateMember(key.getName(), this);
    }

    @TruffleBoundary
    @Fallback
    void doFallback(Object target, Object key, PropertyDescriptor desc) {
        throw Errors.createTypeErrorCannotSetProperty(key.toString(), target, this);
    }
}
