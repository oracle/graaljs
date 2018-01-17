package com.oracle.truffle.js.nodes.tags;

import java.util.function.Function;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.interop.CanResolve;
import com.oracle.truffle.api.interop.KeyInfo;
import com.oracle.truffle.api.interop.MessageResolution;
import com.oracle.truffle.api.interop.Resolve;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.Node.Child;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.cast.JSToPropertyKeyNode;
import com.oracle.truffle.js.nodes.interop.JSForeignToJSTypeNode;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.PropertyDescriptor;

@MessageResolution(receiverType = NodeObjectDescriptor.class)
public class NodeObjectDescriptorFactory {

    @Resolve(message = "READ")
    abstract static class Read extends Node {

        public Object access(NodeObjectDescriptor target, String key) {
            Object p = target.getProperty(key);
            if (p instanceof Function) {
                // TODO call
            }
            return p;
        }
    }

    @Resolve(message = "GET_SIZE")
    abstract static class GetSize extends Node {

        public Object access(NodeObjectDescriptor target) {
            return target.size();
        }
    }

    @Resolve(message = "HAS_SIZE")
    abstract static class HasSize extends Node {

        public Object access(NodeObjectDescriptor target) {
            return false;
        }
    }

    @Resolve(message = "HAS_KEYS")
    abstract static class HasKeys extends Node {

        public Object access(@SuppressWarnings("unused") Object target) {
            return true;
        }
    }

    @Resolve(message = "KEYS")
    abstract static class Keys extends Node {

        public Object access(NodeObjectDescriptor target) {
            return target.getPropertyNames();
        }
    }

    @Resolve(message = "KEY_INFO")
    abstract static class KeyInfoMR extends Node {

        public Object access(NodeObjectDescriptor target, Object key) {
            return KeyInfo.newBuilder().setInternal(false).setInvocable(false).setWritable(false).setReadable(true).build();
        }
    }

    @CanResolve
    public abstract static class CanResolveNode extends Node {

        protected boolean test(TruffleObject receiver) {
            return receiver instanceof NodeObjectDescriptor;
        }
    }

}
