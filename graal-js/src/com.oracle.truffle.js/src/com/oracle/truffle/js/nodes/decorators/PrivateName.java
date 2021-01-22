package com.oracle.truffle.js.nodes.decorators;

import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.js.runtime.objects.PropertyDescriptor;

public class PrivateName {
    private final HiddenKey key;
    private int kind;
    private PropertyDescriptor descriptor;

    public PrivateName(HiddenKey key) {
        this.key = key;
    }

    public String getName() {
        return key.getName();
    }

    public HiddenKey getHiddenKey() {
        return key;
    }

    public boolean isField() {
        return JSKind.isField(kind);
    }

    public boolean isMethod() {
        return JSKind.isMethod(kind);
    }

    public boolean isAccessor() {
        return JSKind.isAccessor(kind);
    }

    public void setKind(int kind) {
        assert JSKind.isField(kind) || JSKind.isMethod(kind) || JSKind.isAccessor(kind);
        this.kind = kind;
    }

    public void setDescriptor(PropertyDescriptor descriptor) {
        this.descriptor = descriptor;
    }

    public PropertyDescriptor getDescriptor() {
        return this.descriptor;
    }

    @Override
    public String toString() {
        return key.getName();
    }
}
