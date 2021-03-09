package com.oracle.truffle.js.runtime.builtins;

import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.js.runtime.objects.JSNonProxyObject;

public class JSTemporalCalendarObject extends JSNonProxyObject {

    private final String id;

    protected JSTemporalCalendarObject(Shape shape) {
        super(shape);
        this.id = "iso8601";
    }

    protected JSTemporalCalendarObject(Shape shape, String id) {
        super(shape);
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
