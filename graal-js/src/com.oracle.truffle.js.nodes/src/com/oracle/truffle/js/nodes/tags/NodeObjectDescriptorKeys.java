package com.oracle.truffle.js.nodes.tags;

import java.util.Map;

import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.TruffleObject;

public class NodeObjectDescriptorKeys implements TruffleObject {

    private final Object[] keys;

    public NodeObjectDescriptorKeys(Map<String, Object> from) {
        this.keys = from.keySet().toArray();
    }

    @Override
    public ForeignAccess getForeignAccess() {
        return NodeObjectDescriptorKeysFactoryForeign.ACCESS;
    }

    public Object getKeyAt(int pos) {
        return keys[pos];
    }

    public int size() {
        return keys.length;
    }

}
