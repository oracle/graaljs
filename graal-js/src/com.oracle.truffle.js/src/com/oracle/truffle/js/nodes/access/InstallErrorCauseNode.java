package com.oracle.truffle.js.nodes.access;

import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.objects.JSObject;

public class InstallErrorCauseNode extends JavaScriptBaseNode {
    private static final String CAUSE = "cause";
    @Child
    private CreateDataPropertyNode createNonEnumerableDataPropertyNode;

    public InstallErrorCauseNode(JSContext context) {
        this.createNonEnumerableDataPropertyNode = CreateDataPropertyNode.createNonEnumerable(context, CAUSE);
    }

    public void executeVoid(DynamicObject error, DynamicObject options) {
        assert JSObject.isJSObject(error);
        if(JSObject.hasProperty(options, CAUSE)) {
            Object cause = JSObject.get(options, CAUSE);
            createNonEnumerableDataPropertyNode.executeVoid(error, cause);
        }
    }
}
