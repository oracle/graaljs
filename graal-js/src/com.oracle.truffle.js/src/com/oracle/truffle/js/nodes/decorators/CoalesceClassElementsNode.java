package com.oracle.truffle.js.nodes.decorators;

import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.runtime.Errors;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class CoalesceClassElementsNode extends JavaScriptBaseNode {
    private CoalesceClassElementsNode() {}

    public static CoalesceClassElementsNode create() {
        return new CoalesceClassElementsNode();
    }

    public List<ElementDescriptor> executeCoalition(List<ElementDescriptor> elements) {
        List<ElementDescriptor> newElements = new ArrayList<>();
        HashMap<Object, Integer> indexMap = new HashMap<>();
        for(int i = 0; i < elements.size(); i++) {
            ElementDescriptor element = elements.get(i);
            if(!indexMap.containsKey(element.getKey())) {
                indexMap.put(element.getKey(),i);
            } else {
                if(element.isMethod() ||element.isAccessor()) {
                    Integer index = indexMap.get(element.getKey());
                    ElementDescriptor other = elements.get(index);
                    if(other.isMethod() || element.isAccessor() && element.getPlacement() == other.getPlacement()) {
                        if(element.getDescriptor().isDataDescriptor() || other.getDescriptor().isDataDescriptor()) {
                            assert !element.hasPrivateKey();
                            assert element.getDescriptor().getConfigurable() && other.getDescriptor().getConfigurable();
                            if(element.hasDecorators() || other.hasDecorators()) {
                                throw Errors.createTypeError("Overwritten and overwriting methods can not be decorated.", this);
                            }
                            other.setDescriptor(element.getDescriptor());
                        } else {
                            if(element.hasDecorators()) {
                                if(other.hasDecorators()) {
                                    throw Errors.createTypeError("Either getter or setter can be decorated, not both.", this);
                                }
                                other.setDecorators(element.getDecorators());
                            }
                            //CoalesceGetterSetter
                            assert other.getDescriptor().isAccessorDescriptor() && element.getDescriptor().isAccessorDescriptor();
                            if(element.getDescriptor().hasGet()) {
                                other.getDescriptor().setGet((DynamicObject) element.getDescriptor().getGet());
                            } else {
                                assert element.getDescriptor().hasSet();
                                other.getDescriptor().setSet((DynamicObject) element.getDescriptor().getSet());
                            }
                        }
                        continue;
                    }
                }
            }
            newElements.add(element);
        }
        return newElements;
    }
}
