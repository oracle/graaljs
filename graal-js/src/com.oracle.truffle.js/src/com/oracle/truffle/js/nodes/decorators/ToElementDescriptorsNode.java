package com.oracle.truffle.js.nodes.decorators;

import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.access.GetIteratorNode;
import com.oracle.truffle.js.nodes.access.IteratorCloseNode;
import com.oracle.truffle.js.nodes.access.IteratorStepNode;
import com.oracle.truffle.js.nodes.access.IteratorValueNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.objects.IteratorRecord;
import com.oracle.truffle.js.runtime.objects.JSOrdinaryObject;

import java.util.ArrayList;
import java.util.List;

public class ToElementDescriptorsNode extends JavaScriptBaseNode {
    protected static final String EXTRAS = "extras";
    @Child
    private GetIteratorNode getIteratorNode;
    @Child
    private IteratorStepNode iteratorStepNode;
    @Child
    private IteratorValueNode iteratorValueNode;
    @Child
    private IteratorCloseNode iteratorCloseNode;

    private ToElementDescriptorsNode(JSContext context) {
        this.getIteratorNode = GetIteratorNode.create(context);
        this.iteratorStepNode = IteratorStepNode.create(context);
        this.iteratorValueNode = IteratorValueNode.create(context);
        this.iteratorCloseNode = IteratorCloseNode.create(context);
    }

    public static ToElementDescriptorsNode create(JSContext context) {
        return new ToElementDescriptorsNode(context);
    }

    public List<ElementDescriptor> toElementDescriptors(Object elementObjects){
        if(JSRuntime.isNullOrUndefined(elementObjects)) {
            return null;
        }
        List<ElementDescriptor> elements = new ArrayList<>();
        IteratorRecord iterator = getIteratorNode.execute(elementObjects);
        Object extra;
        while ((extra = getNext(iterator)) != null) {
            if (!JSRuntime.isNullOrUndefined(JSOrdinaryObject.get((DynamicObject) extra, EXTRAS))) {
                throw Errors.createTypeError("Property extras of element descriptor must not have nested extras.");
            }
            elements.add(DescriptorUtil.toElementDescriptor(extra));
        }
        return elements;
    }


    protected Object getNext(IteratorRecord iteratorRecord) {
        try {
            Object next = iteratorStepNode.execute(iteratorRecord);
            if (next == Boolean.FALSE) {
                return null;
            }
            return iteratorValueNode.execute((DynamicObject) next);
        } catch (Exception ex) {
            iteratorCloseNode.executeAbrupt(iteratorRecord.getIterator());
            throw ex;
        }
    }
}
