package com.oracle.truffle.js.nodes.decorators;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.JSWriteFrameSlotNode;
import com.oracle.truffle.js.nodes.cast.JSToPropertyKeyNode;

public abstract class ClassElementKeyNode extends JavaScriptBaseNode {
    public abstract Object executeKey(VirtualFrame frame);

    public static ClassElementKeyNode createComputedKeyNode(JavaScriptNode keyNode){
        return new ComputedKeyNode(keyNode);
    }

    public static ClassElementKeyNode createPrivateKeyNode(JavaScriptNode keyNode, JSWriteFrameSlotNode writeFrameSlotNode) {
        return new PrivateKeyNode(keyNode, writeFrameSlotNode);
    }

    public static ClassElementKeyNode createObjectKeyNode(Object key){
        return new ObjectKeyNode(key);
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
    }
}
