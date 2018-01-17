package com.oracle.truffle.js.nodes.tags;

import com.oracle.truffle.api.interop.CanResolve;
import com.oracle.truffle.api.interop.MessageResolution;
import com.oracle.truffle.api.interop.Resolve;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.Node;

@MessageResolution(receiverType = NodeObjectDescriptorKeys.class)
public class NodeObjectDescriptorKeysFactory {

    @Resolve(message = "READ")
    abstract static class Read extends Node {

        public Object access(NodeObjectDescriptorKeys target, Number key) {
            return target.getKeyAt(key.intValue());
        }
    }

    @Resolve(message = "HAS_SIZE")
    abstract static class HasSize extends Node {

        public boolean access(NodeObjectDescriptorKeys target) {
            return true;
        }
    }

    @Resolve(message = "GET_SIZE")
    abstract static class GetSize extends Node {

        public Object access(NodeObjectDescriptorKeys target) {
            return target.size();
        }
    }

    @CanResolve
    public abstract static class CanResolveNode extends Node {

        protected boolean test(TruffleObject receiver) {
            return receiver instanceof NodeObjectDescriptor;
        }
    }

}
