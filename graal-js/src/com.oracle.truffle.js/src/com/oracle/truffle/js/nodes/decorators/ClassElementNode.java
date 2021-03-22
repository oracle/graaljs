package com.oracle.truffle.js.nodes.decorators;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.ObjectLiteralNode;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.objects.JSAttributes;
import com.oracle.truffle.js.runtime.objects.PropertyDescriptor;

import java.util.Set;

public abstract class ClassElementNode extends JavaScriptBaseNode {
    public static final ClassElementNode[] EMPTY = {};

    @Child protected ClassElementKeyNode key;
    @Children protected final JavaScriptNode[] decorators;

    private boolean isStatic;
    private boolean isPrivate;
    private boolean isAnonymousFunctionDefinition;
    protected int attributes;
    protected int placement;

    protected ClassElementNode(ClassElementKeyNode key, boolean isStatic, boolean isPrivate, boolean isAnonymousFunctionDefinition, JavaScriptNode[] decorators){
        this.key = key;
        this.isStatic = isStatic;
        this.isPrivate = isPrivate;
        this.isAnonymousFunctionDefinition = isAnonymousFunctionDefinition;
        this.decorators = decorators;
    }

    public boolean isStatic() {
        return isStatic;
    }

    protected boolean isPrivate() {
        return isPrivate;
    }

    @ExplodeLoop
    protected Object[] executeDecorators(VirtualFrame frame) {
        //DecoratorEvaluation
        CompilerAsserts.partialEvaluationConstant(decorators);
        if(decorators == null) {
            return new Object[0];
        }
        Object[] d = new Object[decorators.length];
        for(int i = 0; i < decorators.length; i++) {
            d[i] = decorators[i].execute(frame);
        }
        return d;
    }

    protected abstract ClassElementNode copyUninitialized(Set<Class<? extends Tag>> materializedTags);
    protected Object executeKey(VirtualFrame frame){
        return key.executeKey(frame);
    }
    public abstract ElementDescriptor execute(VirtualFrame frame, DynamicObject homeObject, JSContext context);
    public boolean isAnonymousFunctionDefinition() { return isAnonymousFunctionDefinition; }

    public static ClassElementNode createFieldClassElement(ClassElementKeyNode key, JavaScriptNode initialize, boolean isStatic, boolean isPrivate, boolean isAnonymousFunctionDefinition, JavaScriptNode[] decorators){
        return new FieldClassElementNode(key, initialize, isStatic, isPrivate, isAnonymousFunctionDefinition, decorators);
    }

    public static ClassElementNode createMethodClassElement(ClassElementKeyNode key, JavaScriptNode value, boolean isStatic, boolean isPrivate, JavaScriptNode[] decorators) {
        return new MethodClassElementNode(key, value, isStatic, isPrivate, decorators);
    }

    public static ClassElementNode createAccessorClassElement(ClassElementKeyNode key, JavaScriptNode getter, JavaScriptNode setter, boolean isStatic, boolean isPrivate, JavaScriptNode[] decorators){
        return new AccessorClassElementNode(key, getter, setter, isStatic, isPrivate, decorators);
    }

    public static ClassElementNode[] cloneUninitialized(ClassElementNode[] elements, Set<Class<? extends Tag>> materializedTags) {
        ClassElementNode[] copy = elements.clone();
        for(int i = 0; i < copy.length; i++) {
            copy[i] = copy[i].copyUninitialized(materializedTags);
        }
        return copy;
    }

    private abstract static class DataClassElementNode extends ClassElementNode {
        @Child protected JavaScriptNode valueNode;

        DataClassElementNode(ClassElementKeyNode key, JavaScriptNode valueNode, boolean isStatic, boolean isPrivate, boolean isAnonymousFunctionDefinition, JavaScriptNode[] decorators) {
            super(key, isStatic, isPrivate, isAnonymousFunctionDefinition, decorators);
            this.valueNode = valueNode;
        }

        @Override
        public abstract ElementDescriptor execute(VirtualFrame frame, DynamicObject homeObject, JSContext context);

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
        protected FieldClassElementNode(ClassElementKeyNode key, JavaScriptNode initialize, boolean isStatic, boolean isPrivate, boolean isAnonymousFunctionDefinition, JavaScriptNode[] decorators){
            super(key, initialize, isStatic, isPrivate, isAnonymousFunctionDefinition, decorators);
            //ClassFieldDefinitionEvaluation
            if(isPrivate) {
                attributes = JSAttributes.fromConfigurableEnumerableWritable(false, false, true);
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
        public ElementDescriptor execute(VirtualFrame frame, DynamicObject homeObject, JSContext context) {
            Object key = executeKey(frame);
            Object value = executeValue(frame, homeObject);
            Object[] decorators = executeDecorators(frame);

            PropertyDescriptor propDesc = PropertyDescriptor.createEmpty();
            propDesc.setEnumerable(JSAttributes.isEnumerable(attributes));
            propDesc.setWritable(JSAttributes.isWritable(attributes));
            propDesc.setConfigurable(JSAttributes.isConfigurable(attributes));
            ElementDescriptor elemDesc = ElementDescriptor.createField(key, propDesc, placement, value, isPrivate(), this);
            elemDesc.setDecorators(decorators);
            return elemDesc;
        }

        @Override
        protected ClassElementNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
            return new FieldClassElementNode(ClassElementKeyNode.cloneUninitialized(key, materializedTags), JavaScriptNode.cloneUninitialized(valueNode,materializedTags),isStatic(),isPrivate(),isAnonymousFunctionDefinition(),JavaScriptNode.cloneUninitialized(decorators,materializedTags));
        }
    }

    private static class MethodClassElementNode extends DataClassElementNode {
        protected MethodClassElementNode(ClassElementKeyNode key, JavaScriptNode value, boolean isStatic, boolean isPrivate, JavaScriptNode[] decorators) {
            super(key, value, isStatic, isPrivate, false, decorators);
            //DefaultMethodDescriptor
            if(isPrivate) {
                attributes = JSAttributes.fromConfigurableEnumerableWritable(false, false, false);
            } else {
                attributes = JSAttributes.fromConfigurableEnumerableWritable(true, false, true);
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
        public ElementDescriptor execute(VirtualFrame frame, DynamicObject homeObject, JSContext context) {
            Object key = executeKey(frame);
            Object value = executeValue(frame, homeObject);
            Object[] decorators = executeDecorators(frame);

            PropertyDescriptor propDesc = PropertyDescriptor.createData(value, attributes);
            ElementDescriptor elemDesc = ElementDescriptor.createMethod(key, propDesc, placement, isPrivate(), this);
            elemDesc.setDecorators(decorators);
            return elemDesc;
        }

        @Override
        protected ClassElementNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
            return new MethodClassElementNode(ClassElementKeyNode.cloneUninitialized(key, materializedTags), JavaScriptNode.cloneUninitialized(valueNode,materializedTags),isStatic(), isPrivate(), JavaScriptNode.cloneUninitialized(decorators, materializedTags));
        }
    }


    private static class AccessorClassElementNode extends ClassElementNode {
        @Child private JavaScriptNode getterNode;
        @Child private JavaScriptNode setterNode;

        protected AccessorClassElementNode(ClassElementKeyNode key, JavaScriptNode getterNode, JavaScriptNode setterNode, boolean isStatic, boolean isPrivate, JavaScriptNode[] decorators) {
            super(key, isStatic, isPrivate, false, decorators);
            this.getterNode = getterNode;
            this.setterNode = setterNode;
            if(isPrivate) {
                attributes = JSAttributes.fromConfigurableEnumerable(false, false);
            } else {
                attributes = JSAttributes.fromConfigurableEnumerable(true, false);
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
        public ElementDescriptor execute(VirtualFrame frame, DynamicObject homeObject, JSContext context) {
            Object key = executeKey(frame);
            Object[] decorators = executeDecorators(frame);
            Object getter = null;
            Object setter = null;
            if(getterNode != null) {
                if (getterNode instanceof ObjectLiteralNode.MakeMethodNode) {
                    getter = ((ObjectLiteralNode.MakeMethodNode) getterNode).executeWithObject(frame, homeObject);
                } else {
                    getter = getterNode.execute(frame);
                }
            }
            if(setterNode != null) {
                if (setterNode instanceof ObjectLiteralNode.MakeMethodNode) {
                    setter = ((ObjectLiteralNode.MakeMethodNode) setterNode).executeWithObject(frame, homeObject);
                } else {
                    setter = setterNode.execute(frame);
                }
            }

            PropertyDescriptor propDesc = PropertyDescriptor.createAccessor((DynamicObject) getter, (DynamicObject) setter, attributes);
            ElementDescriptor elemDesc = ElementDescriptor.createAccessor(key, propDesc, placement, isPrivate(), this);
            elemDesc.setDecorators(decorators);
            return elemDesc;
        }

        @Override
        protected ClassElementNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
            return new AccessorClassElementNode(ClassElementKeyNode.cloneUninitialized(key, materializedTags),JavaScriptNode.cloneUninitialized(getterNode,materializedTags),JavaScriptNode.cloneUninitialized(setterNode, materializedTags), isStatic(), isPrivate(), JavaScriptNode.cloneUninitialized(decorators, materializedTags));
        }
    }
}
