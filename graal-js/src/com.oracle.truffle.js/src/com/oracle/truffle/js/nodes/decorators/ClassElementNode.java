package com.oracle.truffle.js.nodes.decorators;


import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.ObjectLiteralNode;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.objects.Accessor;
import com.oracle.truffle.js.runtime.objects.JSAttributes;
import com.oracle.truffle.js.runtime.objects.PropertyDescriptor;

public abstract class ClassElementNode extends JavaScriptBaseNode {
    private static final int PLACEMENT_STATIC = 1 << 0;
    private static final int PLACEMENT_PROTOTYPE = 1 << 1;
    private static final int PLACEMENT_OWN = 1 << 2;

    public static final ClassElementNode[] EMPTY = {};

    @Child private ClassElementKeyNode key;
    private int placement;
    protected int attributes;
    private boolean isField;
    private boolean isAnonymousFunctionDefinition;

    protected ClassElementNode(ClassElementKeyNode key, int placement, boolean configurable, boolean enumerable, boolean writable, boolean isField, boolean isAnonymousFunctionDefinition){
        this.key = key;
        this.placement = placement;
        this.attributes = JSAttributes.fromConfigurableEnumerableWritable(configurable, enumerable, writable);
        this.isField = isField;
        this.isAnonymousFunctionDefinition = isAnonymousFunctionDefinition;
    }

    public void prepareKey(VirtualFrame frame) { key.executeVoid(frame); }
    public Object executeKey(VirtualFrame frame){
        return key.executeKey(frame);
    }
    public abstract void executeVoid(VirtualFrame frame, DynamicObject homeObject, JSContext context);
    public abstract Object executeValue(VirtualFrame frame, DynamicObject homeObject);

    public static ClassElementNode createDataClassElement(ClassElementKeyNode key, JavaScriptNode value, int placement, boolean configurable, boolean enumerable, boolean writable, boolean isField, boolean isAnonymousFunctionDefinition){
        return new DataClassElementNode(key, value, placement, configurable, enumerable, writable, isField, isAnonymousFunctionDefinition);
    }

    public static ClassElementNode createAccessorClassElement(ClassElementKeyNode key, JavaScriptNode getter, JavaScriptNode setter, int placement, boolean configurable, boolean enumerable, boolean writable){
        return new AccessorClassElementNode(key, getter, setter, placement, configurable, enumerable, writable);
    }

    public boolean isStatic() {
        return (placement & PLACEMENT_STATIC) != 0;
    }

    public boolean isPrototype(){
        return (placement & PLACEMENT_PROTOTYPE) != 0;
    }

    public boolean isOwn() {
        return (placement & PLACEMENT_OWN) != 0;
    }

    public boolean isField() { return isField; }

    public boolean isAnonymousFunctionDefinition() { return isAnonymousFunctionDefinition; }

    private static class DataClassElementNode extends ClassElementNode {
        @Child JavaScriptNode valueNode;

        protected DataClassElementNode(ClassElementKeyNode key, JavaScriptNode valueNode, int placement, boolean configurable, boolean enumerable, boolean writable, boolean isField, boolean isAnonymousFunctionDefinition) {
            super(key, placement, configurable, enumerable, writable, isField, isAnonymousFunctionDefinition);
            this.valueNode = valueNode;
        }

        @Override
        public void executeVoid(VirtualFrame frame, DynamicObject homeObject, JSContext context) {
            prepareKey(frame);
            Object key = executeKey(frame);
            Object value = null;
            if (valueNode instanceof ObjectLiteralNode.MakeMethodNode) {
                 value = ((ObjectLiteralNode.MakeMethodNode) valueNode).executeWithObject(frame, homeObject);
            } else {
                value = valueNode.execute(frame);
            }
            PropertyDescriptor propDesc = PropertyDescriptor.createData(value, attributes);
            JSRuntime.definePropertyOrThrow(homeObject, key, propDesc);
        }

        @Override
        public Object executeValue(VirtualFrame frame, DynamicObject homeObject) {
            if (valueNode instanceof ObjectLiteralNode.MakeMethodNode) {
                return ((ObjectLiteralNode.MakeMethodNode) valueNode).executeWithObject(frame, homeObject);
            } else {
                return valueNode.execute(frame);
            }
        }
    }


    private static class AccessorClassElementNode extends ClassElementNode {
        @Child private JavaScriptNode getterNode;
        @Child private JavaScriptNode setterNode;

        protected AccessorClassElementNode(ClassElementKeyNode key, JavaScriptNode getterNode, JavaScriptNode setterNode, int placement, boolean configurable, boolean enumerable, boolean writable) {
            super(key, placement, configurable, enumerable, writable, false, false);
            this.getterNode = getterNode;
            this.setterNode = setterNode;
        }

        @Override
        public void executeVoid(VirtualFrame frame, DynamicObject homeObject, JSContext context) {
            Object key = executeKey(frame);
            Object getter = null;
            Object setter = null;
            if(getterNode != null) {
                getter = getterNode.execute(frame);
            }
            if(setterNode != null) {
                setter = setterNode.execute(frame);
            }
            PropertyDescriptor propDesc = PropertyDescriptor.createAccessor((DynamicObject) getter,(DynamicObject) setter, attributes);
            JSRuntime.definePropertyOrThrow(homeObject, key, propDesc);
        }

        @Override
        public Object executeValue(VirtualFrame frame, DynamicObject homeObject) {
            Object getter = null;
            Object setter = null;
            if(getterNode != null) {
                getter = getterNode.execute(frame);
            }
            if(setterNode != null) {
                setter = setterNode.execute(frame);
            }
            return new Accessor((DynamicObject) getter, (DynamicObject) setter);
        }
    }
}
