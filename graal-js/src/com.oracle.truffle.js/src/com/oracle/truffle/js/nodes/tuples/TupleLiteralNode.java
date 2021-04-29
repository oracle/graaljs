package com.oracle.truffle.js.nodes.tuples;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.GetIteratorNode;
import com.oracle.truffle.js.nodes.access.IteratorGetNextValueNode;
import com.oracle.truffle.js.nodes.access.JSConstantNode;
import com.oracle.truffle.js.nodes.instrumentation.JSTags;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.LiteralTag;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Tuple;
import com.oracle.truffle.js.runtime.objects.IteratorRecord;
import com.oracle.truffle.js.runtime.util.SimpleArrayList;

import java.util.Set;

public abstract class TupleLiteralNode extends JavaScriptNode {

    protected final JSContext context;

    public TupleLiteralNode(TupleLiteralNode copy) {
        this.context = copy.context;
    }

    protected TupleLiteralNode(JSContext context) {
        this.context = context;
    }

    @Override
    public abstract Object execute(VirtualFrame frame);

    @Override
    public boolean hasTag(Class<? extends Tag> tag) {
        if (tag == LiteralTag.class) {
            return true;
        } else {
            return super.hasTag(tag);
        }
    }

    @Override
    public Object getNodeObject() {
        return JSTags.createNodeObjectDescriptor(LiteralTag.TYPE, LiteralTag.Type.TupleLiteral.name());
    }

    public static JavaScriptNode create(JSContext context, JavaScriptNode[] elements) {
        if (elements == null || elements.length == 0) {
            return createEmptyTuple();
        }

        Object[] constantValues = resolveConstants(elements);
        if (constantValues != null) {
            return createConstantTuple(constantValues);
        }

        return new DefaultTupleLiteralNode(context, elements);
    }

    public static TupleLiteralNode createWithSpread(JSContext context, JavaScriptNode[] elements) {
        return new DefaultTupleLiteralWithSpreadNode(context, elements);
    }

    private static JSConstantNode createEmptyTuple() {
        return JSConstantNode.create(Tuple.create());
    }

    private static JSConstantNode createConstantTuple(Object[] array) {
        Tuple tuple = createTuple(array);
        return JSConstantNode.create(tuple);
    }

    private static Object[] resolveConstants(JavaScriptNode[] nodes) {
        Object[] values = new Object[nodes.length];
        for (int i = 0; i < values.length; i++) {
            JavaScriptNode node = nodes[i];
            if (node instanceof JSConstantNode) {
                values[i] = ((JSConstantNode) node).getValue();
            } else {
                return null;
            }
        }
        return values;
    }

    private static Tuple createTuple(Object[] array) {
        for (Object element : array) {
            if (!JSRuntime.isJSPrimitive(element)) {
                throw Errors.createTypeError("Tuples cannot contain non-primitive values");
            }
        }
        return Tuple.create(array);
    }

    private static class DefaultTupleLiteralNode extends TupleLiteralNode {

        @Children protected final JavaScriptNode[] elements;

        DefaultTupleLiteralNode(JSContext context, JavaScriptNode[] elements) {
            super(context);
            this.elements = elements;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            Object[] values = new Object[elements.length];
            for (int i = 0; i < elements.length; i++) {
                values[i] = elements[i].execute(frame);
            }
            return createTuple(values);
        }

        @Override
        protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
            return new DefaultTupleLiteralNode(context, cloneUninitialized(elements, materializedTags));
        }
    }

    private static final class DefaultTupleLiteralWithSpreadNode extends DefaultTupleLiteralNode {

        private final BranchProfile growProfile = BranchProfile.create();

        DefaultTupleLiteralWithSpreadNode(JSContext context, JavaScriptNode[] elements) {
            super(context, elements);
            assert elements.length > 0;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            SimpleArrayList<Object> evaluatedElements = new SimpleArrayList<>(elements.length + JSConfig.SpreadArgumentPlaceholderCount);
            for (int i = 0; i < elements.length; i++) {
                Node node = elements[i];
                if (elements[i] instanceof WrapperNode) {
                    node = ((WrapperNode) elements[i]).getDelegateNode();
                }
                if (node instanceof SpreadTupleNode) {
                    ((SpreadTupleNode) node).executeToList(frame, evaluatedElements, growProfile);
                } else {
                    evaluatedElements.add(elements[i].execute(frame), growProfile);
                }
            }
            return createTuple(evaluatedElements.toArray());
        }

        @Override
        protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
            return new DefaultTupleLiteralWithSpreadNode(context, cloneUninitialized(elements, materializedTags));
        }
    }

    public static final class SpreadTupleNode extends JavaScriptNode {
        @Child private GetIteratorNode getIteratorNode;
        @Child private IteratorGetNextValueNode iteratorStepNode;

        private SpreadTupleNode(JSContext context, JavaScriptNode arg) {
            this.getIteratorNode = GetIteratorNode.create(context, arg);
            this.iteratorStepNode = IteratorGetNextValueNode.create(context, null, JSConstantNode.create(null), false);
        }

        public static SpreadTupleNode create(JSContext context, JavaScriptNode arg) {
            return new SpreadTupleNode(context, arg);
        }

        public void executeToList(VirtualFrame frame, SimpleArrayList<Object> toList, BranchProfile growProfile) {
            IteratorRecord iteratorRecord = getIteratorNode.execute(frame);
            for (;;) {
                Object nextArg = iteratorStepNode.execute(frame, iteratorRecord);
                if (nextArg == null) {
                    break;
                }
                toList.add(nextArg, growProfile);
            }
        }

        @Override
        public Object execute(VirtualFrame frame) {
            throw Errors.shouldNotReachHere("Cannot execute SpreadTupleNode");
        }

        @Override
        protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
            SpreadTupleNode copy = (SpreadTupleNode) copy();
            copy.getIteratorNode = cloneUninitialized(getIteratorNode, materializedTags);
            copy.iteratorStepNode = cloneUninitialized(iteratorStepNode, materializedTags);
            return copy;
        }
    }

    @Override
    public boolean isResultAlwaysOfType(Class<?> clazz) {
        return clazz == DynamicObject.class;
    }

    public enum TupleContentType {
        Byte,
        Integer,
        Double,
        Object
    }
}
