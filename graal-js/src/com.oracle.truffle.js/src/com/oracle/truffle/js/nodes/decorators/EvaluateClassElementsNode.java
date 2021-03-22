package com.oracle.truffle.js.nodes.decorators;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.runtime.Boundaries;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;

import java.util.Map;
import java.util.Set;

public class EvaluateClassElementsNode extends JavaScriptBaseNode {
    private final JSContext context;
    private final BranchProfile errorBranch = BranchProfile.create();

    @Children private final ClassElementNode[] classElementNodes;

    private EvaluateClassElementsNode(JSContext context, ClassElementNode[] classElementNodes) {
        this.context = context;
        this.classElementNodes = classElementNodes;
    }

    public static EvaluateClassElementsNode create(JSContext context, ClassElementNode[] classElementNodes) {
        return new EvaluateClassElementsNode(context, classElementNodes);
    }

    private EvaluateClassElementsNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
        return create(context, ClassElementNode.cloneUninitialized(classElementNodes, materializedTags));
    }

    public static EvaluateClassElementsNode cloneUninitialized(EvaluateClassElementsNode evaluateClassElementsNode, Set<Class<? extends Tag>> materializedTags) {
        return evaluateClassElementsNode.copyUninitialized(materializedTags);
    }

    @ExplodeLoop
    public ElementDescriptor[] execute(VirtualFrame frame, DynamicObject proto, DynamicObject constructor) {
        CompilerAsserts.partialEvaluationConstant(classElementNodes);
        ElementDescriptor[] elements = new ElementDescriptor[classElementNodes.length];
        Map<Object, ElementDescriptor> elementMap = Boundaries.hashMapCreate();
        for(int i = 0; i < classElementNodes.length; i++) {
            ClassElementNode classElement = classElementNodes[i];
            DynamicObject homeObject = classElement.isStatic() ? constructor: proto;
            ElementDescriptor element = classElement.execute(frame, homeObject, context);
            //CoalesceClassElements
            Object key = element.getKey();
            if(!Boundaries.mapContainsKey(elementMap, key)) {
                Boundaries.mapPut(elementMap, key, element);
            } else {
                if(element.isMethod() ||element.isAccessor()) {
                    ElementDescriptor other = Boundaries.mapGet(elementMap, key);
                    if(other.isMethod() || other.isAccessor() && element.getPlacement() == other.getPlacement()) {
                        if(element.getDescriptor().isDataDescriptor() || other.getDescriptor().isDataDescriptor()) {
                            assert !element.hasPrivateKey();
                            assert element.getDescriptor().getConfigurable() && other.getDescriptor().getConfigurable();
                            if(element.hasDecorators() || other.hasDecorators()) {
                                errorBranch.enter();
                                throw Errors.createTypeErrorMethodDecorators(this);
                            }
                            other.setDescriptor(element.getDescriptor());
                        } else {
                            if(element.hasDecorators()) {
                                if(other.hasDecorators()) {
                                    errorBranch.enter();
                                    throw Errors.createTypeErrorAccessorDecorators(this);
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
            elements[i] = element;
        }
        return  elements;
    }
}
