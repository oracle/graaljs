package com.oracle.truffle.js.nodes.decorators;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.JSWriteFrameSlotNode;
import com.oracle.truffle.js.nodes.cast.JSToPropertyKeyNode;

import java.util.Set;

public abstract class ClassElementKeyNode extends JavaScriptBaseNode {

    public abstract Object executeKey(VirtualFrame frame);

    protected abstract ClassElementKeyNode copyUninitialized(Set<Class<? extends Tag>> materializedTags);

    public static ClassElementKeyNode createComputedKeyNode(JavaScriptNode keyNode){
        return new ComputedKeyNode(keyNode);
    }

    public static ClassElementKeyNode createPrivateKeyNode(JavaScriptNode keyNode, JSWriteFrameSlotNode writeFrameSlotNode) {
        return new PrivateKeyNode(keyNode, writeFrameSlotNode);
    }

    public static ClassElementKeyNode createObjectKeyNode(Object key){
        return new ObjectKeyNode(key);
    }

    public static ClassElementKeyNode cloneUninitialized(ClassElementKeyNode key, Set<Class<? extends Tag>> materializedTags){
        return key.copyUninitialized(materializedTags);
    }

    private static class ComputedKeyNode extends ClassElementKeyNode {
        @Child private JavaScriptNode keyNode;
        @Child private JSToPropertyKeyNode toPropertyKeyNode;

        ComputedKeyNode(JavaScriptNode keyNode){
            this.keyNode = keyNode;
            this.toPropertyKeyNode = JSToPropertyKeyNode.create();
        }

        @Override
        public Object executeKey(VirtualFrame frame) {
            Object key = keyNode.execute(frame);
            return toPropertyKeyNode.execute(key);
        }

        @Override
        protected ClassElementKeyNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
            return new ComputedKeyNode(JavaScriptNode.cloneUninitialized(keyNode,materializedTags));
        }
    }

    private static class PrivateKeyNode extends ClassElementKeyNode {
         @Child private JavaScriptNode keyNode;
         @Child private JSWriteFrameSlotNode writeFrameSlotNode;

         PrivateKeyNode(JavaScriptNode keyNode, JSWriteFrameSlotNode writeFrameSlotNode)
         {
             this.keyNode = keyNode;
             this.writeFrameSlotNode = writeFrameSlotNode;
         }

         @Override
         public Object executeKey(VirtualFrame frame){
             writeFrameSlotNode.execute(frame);
             return keyNode.execute(frame);
         }

        @Override
        protected ClassElementKeyNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
            return new PrivateKeyNode(JavaScriptNode.cloneUninitialized(keyNode, materializedTags), JSWriteFrameSlotNode.cloneUninitialized(writeFrameSlotNode, materializedTags
            ));
        }
    }

    private static class ObjectKeyNode extends ClassElementKeyNode {
        private Object key;

        ObjectKeyNode(Object key){
            this.key = key;
        }

        @Override
        public Object executeKey(VirtualFrame frame) {
            return key;
        }

        @Override
        protected ClassElementKeyNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
            return new ObjectKeyNode(key);
        }
    }
}
