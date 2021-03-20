package com.oracle.truffle.js.nodes.decorators;

import com.oracle.truffle.api.nodes.Node;
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

    private ElementDescriptor(){}

    public static ElementDescriptor createEmpty() { return new ElementDescriptor(); }

    public static ElementDescriptor createMethod(Object key, PropertyDescriptor descriptor, int placement, boolean isPrivate, Node originatingNode) {
        ElementDescriptor elem = new ElementDescriptor();
        elem.setKind(JSKind.getKindMethod());
        elem.setKey(key);
        elem.setDescriptor(descriptor);
        elem.setPlacement(placement);
        return elem;
    }

    public static ElementDescriptor createAccessor(Object key, PropertyDescriptor descriptor, int placement, boolean isPrivate, Node originatingNode) {
        ElementDescriptor elem = new ElementDescriptor();
        elem.setKind(JSKind.getKindAccessor());
        elem.setKey(key);
        elem.setDescriptor(descriptor);
        elem.setPlacement(placement);
        return elem;
    }

    public static ElementDescriptor createField(Object key, PropertyDescriptor descriptor, int placement, Object initialize, boolean isPrivate, Node originatingNode){
        ElementDescriptor elem = new ElementDescriptor();
        elem.setKind(JSKind.getKindField());
        elem.setKey(key);
        elem.setDescriptor(descriptor);
        elem.setPlacement(placement);
        elem.setInitialize(initialize);
        return elem;
    }

    public static ElementDescriptor createHook(int placement, Object start, Object replace, Object finish, Node originatingNode) {
        ElementDescriptor elem = new ElementDescriptor();
        elem.setKind(JSKind.getKindHook());
        elem.setPlacement(placement);
        if(start != Undefined.instance) {
            elem.setStart(start);
        }
        if(replace != Undefined.instance) {
            elem.setReplace(replace);
        }
        if(finish != Undefined.instance) {
            elem.setFinish(finish);
        }
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

    public PrivateName getPrivateKey() {
        if (hasPrivateKey()) {
            return (PrivateName) key;
        }
        return null;
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

    public boolean hasStart() { return start != null; }

    public Object getStart() {
        return start;
    }

    public void setStart(Object start) {
        this.start = start;
    }

    public boolean hasReplace() { return replace != null; }

    public Object getReplace() {
        return replace;
    }

    public void setReplace(Object replace) {
        this.replace = replace;
    }

    public boolean hasFinish() { return finish != null; }

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

    public boolean hasPrivateKey() {return key instanceof PrivateName; }

    public boolean hasInitialize() {return initialize != Undefined.instance; }

    public boolean hasDecorators() {return decorators != null && decorators.length != 0;}
}
