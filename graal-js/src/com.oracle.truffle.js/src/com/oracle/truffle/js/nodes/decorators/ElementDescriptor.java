package com.oracle.truffle.js.nodes.decorators;

import com.oracle.truffle.js.runtime.objects.PropertyDescriptor;

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

    private ElementDescriptor(){}

    private static void checkPrivateKey(int placement, PropertyDescriptor descriptor) {
        if(!JSPlacement.isOwn(placement) && !JSPlacement.isPrototype(placement)) {
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
        elem.setKind(JSKind.getKindMethod());
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
        elem.setKind(JSKind.getKindAccessor());
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
        elem.setKind(JSKind.getKindField());
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
        elem.setKind(JSKind.getKindHook());
        elem.setPlacement(placement);
        elem.setStart(start);
        elem.setReplace(replace);
        elem.setFinish(finish);
        return elem;
    }

    public int getKind() {
        return kind;
    }

    public String getKindString() {
        return JSKind.toString(kind);
    }

    public void setKind(int kind) {
        this.kind = kind;
    }

    public boolean hasKey() { return key != null;}

    public Object getKey() {
        return key;
    }

    public void setKey(Object key) {
        this.key = key;
    }

    public boolean hasDescriptor() {return descriptor != null;}

    public PropertyDescriptor getDescriptor() {
        return descriptor;
    }

    public void setDescriptor(PropertyDescriptor descriptor) {
        this.descriptor = descriptor;
    }

    public int getPlacement() {
        return placement;
    }

    public String getPlacementString() {
        return JSPlacement.toString(placement);
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

    public boolean isMethod() { return JSKind.isMethod(kind); }
    public boolean isAccessor() { return JSKind.isAccessor(kind); }
    public boolean isField() { return JSKind.isField(kind); }
    public boolean isHook() { return JSKind.isHook(kind); }

    public boolean isStatic() { return JSPlacement.isStatic(placement); }
    public boolean isPrototype() { return JSPlacement.isPrototype(placement); }
    public boolean isOwn() { return JSPlacement.isOwn(placement); }
}
