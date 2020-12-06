package com.oracle.truffle.js.nodes.decorators;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node.Child;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.ObjectLiteralNode;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.objects.JSAttributes;
import com.oracle.truffle.js.runtime.objects.PropertyDescriptor;

public abstract class ClassElementValueNode extends ClassElementNode {
    @Child
    private ClassElementKeyNode key;

    private boolean isStatic;
    private boolean isPrivate;
    private boolean isAnonymousFunctionDefinition;

    protected ClassElementValueNode(ClassElementKeyNode key, boolean isStatic, boolean isPrivate, boolean isAnonymousFunctionDefinition){
        this.key = key;
        this.isStatic = isStatic;
        this.isPrivate = isPrivate;
        this.isAnonymousFunctionDefinition = isAnonymousFunctionDefinition;
    }

    public boolean isStatic() {
        return isStatic;
    }

    protected boolean isPrivate() {
        return isPrivate;
    }

    protected Object executeKey(VirtualFrame frame){
        return key.executeKey(frame);
    }
    public abstract ElementDescriptor executeElementDescriptor(VirtualFrame frame, DynamicObject homeObject, JSContext context);

    public static ClassElementNode createFieldClassElement(ClassElementKeyNode key, JavaScriptNode initialize, boolean isStatic, boolean isPrivate, boolean isAnonymousFunctionDefinition){
        return new FieldClassElementNode(key, initialize, isStatic, isPrivate, isAnonymousFunctionDefinition);
    }

    public static ClassElementNode createMethodClassElement(ClassElementKeyNode key, JavaScriptNode value, boolean isStatic, boolean isPrivate) {
        return new MethodClassElementNode(key, value, isStatic, isPrivate);
    }

    public static ClassElementNode createAccessorClassElement(ClassElementKeyNode key, JavaScriptNode getter, JavaScriptNode setter, boolean isStatic, boolean isPrivate){
        return new AccessorClassElementNode(key, getter, setter, isStatic, isPrivate);
    }

    public boolean isAnonymousFunctionDefinition() { return isAnonymousFunctionDefinition; }

    private abstract static class DataClassElementNode extends ClassElementValueNode {
        @Child JavaScriptNode valueNode;

        DataClassElementNode(ClassElementKeyNode key, JavaScriptNode valueNode, boolean isStatic, boolean isPrivate, boolean isAnonymousFunctionDefinition) {
            super(key, isStatic, isPrivate, isAnonymousFunctionDefinition);
            this.valueNode = valueNode;
        }

        @Override
        public abstract ElementDescriptor executeElementDescriptor(VirtualFrame frame, DynamicObject homeObject, JSContext context);

        protected Object executeValue(VirtualFrame frame, DynamicObject homeObject) {
            if(valueNode == null) {
                return null;
            }
            if (valueNode instanceof ObjectLiteralNode.MakeMethodNode) {
                return ((ObjectLiteralNode.MakeMethodNode) valueNode).executeWithObject(frame, homeObject);
            } else {
                return valueNode.execute(frame);
            }
        }
    }

    private static class FieldClassElementNode extends DataClassElementNode {
        private int attributes;
        private int placement;

        protected FieldClassElementNode(ClassElementKeyNode key, JavaScriptNode initialize, boolean isStatic, boolean isPrivate, boolean isAnonymousFunctionDefinition){
            super(key, initialize, isStatic, isPrivate, isAnonymousFunctionDefinition);
            //ClassFieldDefinitionEvaluation
            if(isPrivate) {
                attributes = JSAttributes.fromConfigurableEnumerableWritable(false,false, false);
            } else {
                attributes = JSAttributes.fromConfigurableEnumerableWritable(true, true, true);
            }
            if(isStatic) {
                placement = JSPlacement.getStatic();
            } else {
                placement = JSPlacement.getOwn();
            }
        }

        @Override
        public ElementDescriptor executeElementDescriptor(VirtualFrame frame, DynamicObject homeObject, JSContext context) {
            Object key = executeKey(frame);
            Object value = executeValue(frame, homeObject);

            PropertyDescriptor propDesc = PropertyDescriptor.createData(value, attributes);
            return ElementDescriptor.createField(key, propDesc, placement, value, isPrivate());
            //JSRuntime.definePropertyOrThrow(homeObject, key, propDesc);
        }
    }

    private static class MethodClassElementNode extends DataClassElementNode {
        private int attributes;
        private int placement;

        protected MethodClassElementNode(ClassElementKeyNode key, JavaScriptNode value, boolean isStatic, boolean isPrivate) {
            super(key, value, isStatic, isPrivate, false);
            //DefaultMethodDescriptor
            if(isPrivate) {
                attributes = JSAttributes.fromConfigurableEnumerableWritable(false, false, false);
            } else {
                attributes = JSAttributes.fromConfigurableEnumerableWritable(true, true, true);
            }
            if(isStatic) {
                placement = JSPlacement.getStatic();
            } else {
                if (isPrivate) {
                    placement = JSPlacement.getOwn();
                } else {
                    placement = JSPlacement.getPrototype();
                }
            }
        }

        @Override
        public ElementDescriptor executeElementDescriptor(VirtualFrame frame, DynamicObject homeObject, JSContext context) {
            Object key = executeKey(frame);
            Object value = executeValue(frame, homeObject);

            PropertyDescriptor propDesc = PropertyDescriptor.createData(value, attributes);
            return ElementDescriptor.createMethod(key, propDesc, placement, isPrivate());
        }
    }


    private static class AccessorClassElementNode extends ClassElementValueNode {
        @Child private JavaScriptNode getterNode;
        @Child private JavaScriptNode setterNode;

        private int attributes;
        private int placement;

        protected AccessorClassElementNode(ClassElementKeyNode key, JavaScriptNode getterNode, JavaScriptNode setterNode, boolean isStatic, boolean isPrivate) {
            super(key, isStatic, isPrivate, false);
            this.getterNode = getterNode;
            this.setterNode = setterNode;
            if(isPrivate) {
                attributes = JSAttributes.fromConfigurableEnumerable(false, false);
            } else {
                attributes = JSAttributes.fromConfigurableEnumerable(true, true);
            }
            if(isStatic) {
                placement = JSPlacement.getStatic();
            } else {
                if (isPrivate) {
                    placement = JSPlacement.getOwn();
                } else {
                    placement = JSPlacement.getPrototype();
                }
            }
        }

        @Override
        public ElementDescriptor executeElementDescriptor(VirtualFrame frame, DynamicObject homeObject, JSContext context) {
            Object key = executeKey(frame);
            Object getter = null;
            Object setter = null;
            if(getterNode != null) {
                getter = getterNode.execute(frame);
            }
            if(setterNode != null) {
                setter = setterNode.execute(frame);
            }
            PropertyDescriptor propDesc = PropertyDescriptor.createAccessor((DynamicObject) getter, (DynamicObject) setter, attributes);
            return ElementDescriptor.createAccessor(key, propDesc, placement, isPrivate());
        }
    }
}
