package com.oracle.truffle.js.nodes.tags;

import java.util.HashMap;
import java.util.Map;

import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.TruffleObject;

public class NodeObjectDescriptor implements TruffleObject {

    private final Map<String, Object> data = new HashMap<>();

    @Override
    public ForeignAccess getForeignAccess() {
        return NodeObjectDescriptorFactoryForeign.ACCESS;
    }

    public void addProperty(String name, Object value) {
        data.put(name, value);
    }

    public int size() {
        return data.size();
    }

    public Object getProperty(String name) {
        assert data.containsKey(name);
        return data.get(name);
    }

    public TruffleObject getPropertyNames() {
        return new NodeObjectDescriptorKeys(data);
    }

}
