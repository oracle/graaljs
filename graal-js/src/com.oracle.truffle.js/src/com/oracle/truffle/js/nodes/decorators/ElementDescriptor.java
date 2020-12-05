package com.oracle.truffle.js.nodes.decorators;

import com.oracle.truffle.js.runtime.objects.JSProperty;
import com.oracle.truffle.js.runtime.objects.PropertyDescriptor;
import com.oracle.truffle.js.runtime.objects.Undefined;

public class ElementDescriptor {
    private int kind;
    private Object key;
    private PropertyDescriptor descriptor;
    private int placement;
    private Object initialize;
    private Object start;
    private Object replace;
    private Object finish;
    private Object[] decorators;

    private static final int KIND_METHOD = 1 << 0;
    private static final int KIND_ACCESSOR = 1 << 1;
    private static final int KIND_FIELD = 1 << 2;
    private static final int KIND_HOOK = 1 << 3;

    private static final int PLACEMENT_STATIC = 1 << 0;
    private static final int PLACEMENT_PROTOTYPE = 1 << 1;
    private static final int PLACEMENT_OWN = 1 << 2;

    private ElementDescriptor(){}

    private static void checkPrivateKey(int placement, PropertyDescriptor descriptor) {
        if(placement != PLACEMENT_OWN && placement != PLACEMENT_PROTOTYPE) {
            //TODO: throw placement error
        }
        if(descriptor.getEnumerable()){
            //TODO: throw enumerable error
        }
        if(descriptor.getConfigurable()) {
            //TODO: throw configurable error
        }
    }

    public static ElementDescriptor createEmpty() { return new ElementDescriptor(); }

    public static ElementDescriptor createMethod(Object key, PropertyDescriptor descriptor, int placement, boolean isPrivate) {
        if(isPrivate) {
            checkPrivateKey(placement, descriptor);
        }
        if(!descriptor.isDataDescriptor()) {
            //TODO: throw error
        }
        ElementDescriptor elem = new ElementDescriptor();
        elem.setKind(KIND_METHOD);
        elem.setKey(key);
        elem.setDescriptor(descriptor);
        elem.setPlacement(placement);
        return elem;
    }

    public static ElementDescriptor createAccessor(Object key, PropertyDescriptor descriptor, int placement, boolean isPrivate) {
        if(isPrivate){
            checkPrivateKey(placement, descriptor);
        }
        if(!descriptor.isAccessorDescriptor()) {
            //TODO: throw error
        }
        ElementDescriptor elem = new ElementDescriptor();
        elem.setKind(KIND_ACCESSOR);
        elem.setKey(key);
        elem.setDescriptor(descriptor);
        elem.setPlacement(placement);
        return elem;
    }

    public static ElementDescriptor createField(Object key, PropertyDescriptor descriptor, int placement, Object initialize, boolean isPrivate){
        if(isPrivate) {
            checkPrivateKey(placement, descriptor);
        }
        if(descriptor.hasGet()) {
            //TODO:throw get error
        }
        if(descriptor.hasSet()) {
            //TODO: throw set error
        }
        if(descriptor.hasValue()) {
            //TODO: throw set error
        }
        ElementDescriptor elem = new ElementDescriptor();
        elem.setKind(KIND_FIELD);
        elem.setKey(key);
        elem.setDescriptor(descriptor);
        elem.setPlacement(placement);
        elem.setInitialize(initialize);
        return elem;
    }

    public static ElementDescriptor createHook(int placement, Object start, Object replace, Object finish) {
        if(start == null && replace == null && finish == null) {
            //TODO: throw hook error
        }
        if(replace != null && finish != null) {
            //TODO: throw replace and finish error
        }
        ElementDescriptor elem = new ElementDescriptor();
        elem.setKind(KIND_HOOK);
        elem.setPlacement(placement);
        elem.setStart(start);
        elem.setReplace(replace);
        elem.setFinish(finish);
        return elem;
    }

    public int getKind() {
        return kind;
    }

    public void setKind(int kind) {
        this.kind = kind;
    }

    public Object getKey() {
        return key;
    }

    public void setKey(Object key) {
        this.key = key;
    }

    public PropertyDescriptor getDescriptor() {
        return descriptor;
    }

    public void setDescriptor(PropertyDescriptor descriptor) {
        this.descriptor = descriptor;
    }

    public int getPlacement() {
        return placement;
    }

    public void setPlacement(int placement) {
        this.placement = placement;
    }

    public Object getInitialize() {
        return initialize;
    }

    public void setInitialize(Object initialize) {
        this.initialize = initialize;
    }

    public Object getStart() {
        return start;
    }

    public void setStart(Object start) {
        this.start = start;
    }

    public Object getReplace() {
        return replace;
    }

    public void setReplace(Object replace) {
        this.replace = replace;
    }

    public Object getFinish() {
        return finish;
    }

    public void setFinish(Object finish) {
        this.finish = finish;
    }

    public Object[] getDecorators() {
        return decorators;
    }

    public void setDecorators(Object[] decorators) {
        this.decorators = decorators;
    }

    public boolean isMethod() { return (this.kind & KIND_METHOD) != 0; }
    public boolean isAccessor() { return (this.kind & KIND_ACCESSOR) != 0; }
    public boolean isField() { return (this.kind & KIND_FIELD) != 0; }
    public boolean isHook() { return (this.kind & KIND_HOOK) !=0; }

    public boolean isStatic() { return (this.placement & PLACEMENT_STATIC) != 0; }
    public boolean isPrototype() { return (this.placement & PLACEMENT_PROTOTYPE) != 0; }
    public boolean isOwn() { return (this.placement & PLACEMENT_OWN) != 0; }
}
