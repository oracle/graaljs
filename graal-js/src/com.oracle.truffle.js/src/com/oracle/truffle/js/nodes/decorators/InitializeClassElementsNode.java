package com.oracle.truffle.js.nodes.decorators;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.access.InitializeInstanceElementsNode;
import com.oracle.truffle.js.nodes.access.PropertySetNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.runtime.Errors;
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
    @Child private PropertySetNode setPrivateBrandNode;

    private InitializeClassElementsNode(JSContext context) {
        this.context = context;
        this.privateBrandAddNode = PropertySetNode.createSetHidden(JSFunction.PRIVATE_BRAND_ID, context);
        this.hookCallNode = JSFunctionCallNode.createCall();
        this.setPrivateBrandNode = PropertySetNode.createSetHidden(JSFunction.PRIVATE_BRAND_ID, context);
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
        ClassElementList fields = new ClassElementList();
        ClassElementList startHooks = new ClassElementList();
        ClassElementList otherHooks = new ClassElementList();
        int size = elements.size();
        boolean setStaticBrand = false;
        boolean setInstanceBrand = false;
        for(int i = 0; i < size; i++) {
            ElementDescriptor element = elements.dequeue();
            if(element.isStatic() && element.hasKey() && element.hasPrivateKey()) {
                //PrivateBrandAdd
                setStaticBrand = true;
            }
            if((element.isMethod() || element.isAccessor()) && element.hasKey() && element.hasPrivateKey()) {
                setInstanceBrand = true;
            }
            //If the class contains a private instance method or accessor, set F.[[PrivateBrand]].
            if(element.isStatic() || element.isPrototype()) {
                if ((element.isMethod() || element.isAccessor()) && !element.hasPrivateKey()) {
                    DynamicObject receiver = element.isStatic() ? constructor : proto;
                    JSRuntime.definePropertyOrThrow(receiver, element.getKey(), element.getDescriptor());
                }
                if (element.isField()) {
                    assert !element.getDescriptor().hasValue() && !element.getDescriptor().hasGet() && !element.getDescriptor().hasSet();
                    fields.enqueue(element);
                }
                if (element.isHook()) {
                    if (element.hasStart()) {
                        startHooks.enqueue(element);
                    }
                    if (element.hasReplace() || element.hasFinish()) {
                        otherHooks.enqueue(element);
                    }
                }
            } else {
                elements.enqueue(element);
            }
        }
        if(setStaticBrand) {
            //TODO: Perform PrivateBrandAdd
            privateBrandAddNode.setValue(constructor, constructor);
        }
        if(setInstanceBrand) {
            HiddenKey privateBrand = new HiddenKey("Brand");
            setPrivateBrandNode.setValue(constructor, privateBrand);
        }
        if(initializeInstanceElementsNode != null) {
            initializeInstanceElementsNode.executeFields(proto, constructor, fields);
        }
        while(startHooks.size() > 0) {
            ElementDescriptor element = startHooks.dequeue();
            DynamicObject receiver = element.isStatic() ? constructor : proto;
            Object res = hookCallNode.executeCall(JSArguments.createZeroArg(receiver, element.getStart()));
            if(res != Undefined.instance) {
                throw Errors.createTypeErrorHookReturnValue("Start",this);
                //TODO: test
            }
        }
        while(otherHooks.size() > 0) {
            ElementDescriptor element = otherHooks.dequeue();
            if(element.hasReplace()) {
                assert !element.hasFinish();
                assert element.isStatic();
                Object newConstructor = hookCallNode.executeCall(JSArguments.createOneArg(Undefined.instance, element.getReplace(), constructor));
                if(!JSRuntime.isConstructor(newConstructor)) {
                    throw Errors.createTypeErrorHookReplaceValue(this);
                    //TODO: test
                }
                constructor = (DynamicObject) newConstructor;
            } else {
                assert element.hasFinish();
                DynamicObject receiver = element.isStatic() ? constructor : proto;
                Object res = hookCallNode.executeCall(JSArguments.createZeroArg(receiver, element.getFinish()));
                if(res != Undefined.instance) {
                    throw Errors.createTypeErrorHookReturnValue("Finish",this);
                    //TODO: test
                }
            }
        }
        return constructor;
    }
}
