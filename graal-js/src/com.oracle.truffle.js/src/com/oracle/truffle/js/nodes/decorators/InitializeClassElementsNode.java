package com.oracle.truffle.js.nodes.decorators;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.access.InitializeInstanceElementsNode;
import com.oracle.truffle.js.nodes.access.PropertySetNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.objects.Undefined;

public class InitializeClassElementsNode extends JavaScriptBaseNode {
    private final JSContext context;

    @Child private PropertySetNode privateBrandAddNode;
    @Child private JSFunctionCallNode hookCallNode;
    @Child private InitializeInstanceElementsNode initializeInstanceElementsNode;

    private InitializeClassElementsNode(JSContext context) {
        this.context = context;
        this.privateBrandAddNode = PropertySetNode.createSetHidden(JSFunction.PRIVATE_BRAND_ID, context);
        this.hookCallNode = JSFunctionCallNode.createCall();
    }

    public static InitializeClassElementsNode create(JSContext context) {
        return new InitializeClassElementsNode(context);
    }

    @ExplodeLoop
    public DynamicObject execute(DynamicObject proto, DynamicObject constructor, ClassElementList elements) {
        if(elements.getPrototypeFieldCount() != 0 || elements.getStaticFieldCount() != 0) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            initializeInstanceElementsNode = insert(InitializeInstanceElementsNode.create(context));
        }
        ElementDescriptor[] fields = new ElementDescriptor[elements.getPrototypeFieldCount() + elements.getStaticFieldCount()];
        int fieldIndex = 0;
        ClassElementList startHooks = new ClassElementList();
        ClassElementList otherHooks = new ClassElementList();
        int size = elements.size();
        for(int i = 0; i < size; i++) {
            ElementDescriptor element = elements.pop();
            if(element.isStatic() && element.hasKey() && element.hasPrivateKey()) {
                //PrivateBrandAdd
                //TODO: Perform PrivateBrandAdd
                privateBrandAddNode.setValue(constructor, constructor);
            }
            if(element.isStatic() || element.isPrototype()) {
                if ((element.isMethod() || element.isAccessor()) && !element.hasPrivateKey()) {
                    DynamicObject receiver = element.isStatic() ? constructor : proto;
                    JSRuntime.definePropertyOrThrow(receiver, element.getKey(), element.getDescriptor());
                }
                if (element.isField()) {
                    assert !element.getDescriptor().hasValue() && !element.getDescriptor().hasGet() && !element.getDescriptor().hasSet();
                    fields[fieldIndex++] = element;
                }
                if (element.isHook()) {
                    if (element.hasStart()) {
                        startHooks.push(element);
                    }
                    if (element.hasReplace() || element.hasFinish()) {
                        otherHooks.push(element);
                    }
                }
            } else {
                elements.push(element);
            }
        }
        if(initializeInstanceElementsNode != null) {
            initializeInstanceElementsNode.executeFields(proto, constructor, fields);
        }
        while(startHooks.size() > 0) {
            ElementDescriptor element = startHooks.pop();
            DynamicObject receiver = element.isStatic() ? constructor : proto;
            Object res = hookCallNode.executeCall(JSArguments.createZeroArg(receiver, element.getStart()));
            if(res != Undefined.instance) {
                //TODO: throw Error
            }
        }
        while(otherHooks.size() > 0) {
            ElementDescriptor element = otherHooks.pop();
            if(element.hasReplace()) {
                assert !element.hasFinish();
                assert element.isStatic();
                Object newConstructor = hookCallNode.executeCall(JSArguments.createOneArg(Undefined.instance, element.getReplace(), constructor));
                if(!JSRuntime.isConstructor(newConstructor)) {
                    //TODO: throw Error
                }
                constructor = (DynamicObject) newConstructor;
            } else {
                assert element.hasFinish();
                DynamicObject receiver = element.isStatic() ? constructor : proto;
                Object res = hookCallNode.executeCall(JSArguments.createZeroArg(receiver, element.getFinish()));
                if(!JSRuntime.isNullOrUndefined(res)) {
                    //TODO: throw Error
                }
            }
        }
        return constructor;
    }
}
