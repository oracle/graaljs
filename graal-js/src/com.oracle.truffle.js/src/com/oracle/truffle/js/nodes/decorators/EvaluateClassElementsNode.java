package com.oracle.truffle.js.nodes.decorators;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;

import java.util.HashMap;

public class EvaluateClassElementsNode extends JavaScriptBaseNode {
    private final JSContext context;

    @Children private final ClassElementNode[] classElementNodes;

    private EvaluateClassElementsNode(JSContext context, ClassElementNode[] classElementNodes) {
        this.context = context;
        this.classElementNodes = classElementNodes;
    }

    public static EvaluateClassElementsNode create(JSContext context, ClassElementNode[] classElementNodes) {
        return new EvaluateClassElementsNode(context, classElementNodes);
    }

    public ClassElementList execute(VirtualFrame frame, DynamicObject proto, DynamicObject constructor) {
        ClassElementList elements = new ClassElementList();
        HashMap<Object, ElementDescriptor> elementMap = new HashMap<>();
        for(ClassElementNode classElement: classElementNodes) {
            DynamicObject homeObject = classElement.isStatic() ? constructor: proto;
            ElementDescriptor element = classElement.execute(frame, homeObject, context);
            //CoalesceClassElements
            if(!elementMap.containsKey(element.getKey())) {
                elementMap.put(element.getKey(), element);
            } else {
                if(element.isMethod() ||element.isAccessor()) {
                    ElementDescriptor other = elementMap.get(element.getKey());
                    if(other.isMethod() || other.isAccessor() && element.getPlacement() == other.getPlacement()) {
                        if(element.getDescriptor().isDataDescriptor() || other.getDescriptor().isDataDescriptor()) {
                            assert !element.hasPrivateKey();
                            assert element.getDescriptor().getConfigurable() && other.getDescriptor().getConfigurable();
                            if(element.hasDecorators() || other.hasDecorators()) {
                                throw Errors.createTypeErrorMethodDecorators(this);
                                //TODO: test
                            }
                            other.setDescriptor(element.getDescriptor());
                        } else {
                            if(element.hasDecorators()) {
                                if(other.hasDecorators()) {
                                    throw Errors.createTypeErrorAccessorDecorators(this);
                                    //TODO: test
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
            elements.push(element);
        }
        return  elements;
    }
}
