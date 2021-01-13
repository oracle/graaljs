package com.oracle.truffle.js.nodes.decorators;

import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.js.runtime.objects.PropertyDescriptor;

public class PrivateName {
    private final HiddenKey key;
    private int kind = 0;
    private Object brand;
    private PropertyDescriptor descriptor;

    public PrivateName(HiddenKey key) {
        this.key = key;
    }

    public void setKind(int kind) {
        assert JSKind.isField(kind) || JSKind.isMethod(kind) || JSKind.isAccessor(kind);
        this.kind = kind;
    }

    public void setBrand(Object brand) {
        this.brand = brand;
    }

    public void setDescriptor(PropertyDescriptor descriptor) {
        this.descriptor = descriptor;
    }

    public HiddenKey getHiddenKey() {
        return key;
    }
}
