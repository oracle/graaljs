package com.oracle.truffle.js.runtime.builtins;

import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.js.runtime.objects.JSNonProxyObject;

public class JSModuleBlockObject extends JSNonProxyObject {

    protected JSModuleBlockObject(Shape shape) {
        super(shape);
    }
}
