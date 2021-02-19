package com.oracle.truffle.js.nodes.decorators;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.access.InitializeInstanceElementsNode;
import com.oracle.truffle.js.nodes.access.PropertySetNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.runtime.Boundaries;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.objects.Undefined;

import java.util.ArrayList;
import java.util.List;

public class InitializeClassElementsNode extends JavaScriptBaseNode {
    private final JSContext context;
    private final BranchProfile errorBranch = BranchProfile.create();

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

    public DynamicObject execute(DynamicObject proto, DynamicObject constructor, ClassElementList elements) {
        if(elements.getPrototypeAndStaticFieldCount() != 0) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            initializeInstanceElementsNode = insert(InitializeInstanceElementsNode.create(context));
        }
        ElementDescriptor[] fields = new ElementDescriptor[elements.getPrototypeAndStaticFieldCount()];
        int fieldIndex = 0;
        List<ElementDescriptor> startHooks = createList();
        List<ElementDescriptor> otherHooks = createList();
        boolean setStaticBrand = false;
        for(ElementDescriptor element: elements.getStaticAndPrototypeElements()) {
            if(element.isStatic() && element.hasKey() && element.hasPrivateKey()) {
                //PrivateBrandAdd
                setStaticBrand = true;
            }
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
                    startHooks.add(element);
                }
                if (element.hasReplace() || element.hasFinish()) {
                    otherHooks.add(element);
                }
            }
        }

        if(setStaticBrand) {
            //If the class contains a private instance method or accessor, set F.[[PrivateBrand]].
            privateBrandAddNode.setValue(constructor, constructor);
        }
        if(elements.setInstanceBand()) {
            HiddenKey privateBrand = new HiddenKey("Brand");
            setPrivateBrandNode.setValue(constructor, privateBrand);
        }
        if(initializeInstanceElementsNode != null) {
            initializeInstanceElementsNode.executeFields(proto, constructor, fields);
        }
        if(Boundaries.listSize(startHooks) != 0) {
            executeStartHooks(startHooks, constructor, proto);
        }
        if(Boundaries.listSize(otherHooks) != 0) {
            executeOtherHooks(otherHooks, constructor, proto);
        }

        return constructor;
    }

    private void executeStartHooks(List<ElementDescriptor> startHooks, DynamicObject constructor, DynamicObject proto) {
        for(ElementDescriptor element: startHooks) {
            DynamicObject receiver = element.isStatic() ? constructor : proto;
            Object res = hookCallNode.executeCall(JSArguments.createZeroArg(receiver, element.getStart()));
            if(res != Undefined.instance) {
                errorBranch.enter();
                throw Errors.createTypeErrorHookReturnValue("Start",this);
            }
        }
    }

    private void executeOtherHooks(List<ElementDescriptor> otherHooks, DynamicObject constructor, DynamicObject proto) {
        for(ElementDescriptor element: otherHooks) {
            if(element.hasReplace()) {
                assert !element.hasFinish();
                assert element.isStatic();
                Object newConstructor = hookCallNode.executeCall(JSArguments.createOneArg(Undefined.instance, element.getReplace(), constructor));
                if(!JSRuntime.isConstructor(newConstructor)) {
                    errorBranch.enter();
                    throw Errors.createTypeErrorHookReplaceValue(this);
                }
                constructor = (DynamicObject) newConstructor;
            } else {
                assert element.hasFinish();
                DynamicObject receiver = element.isStatic() ? constructor : proto;
                Object res = hookCallNode.executeCall(JSArguments.createZeroArg(receiver, element.getFinish()));
                if(res != Undefined.instance) {
                    errorBranch.enter();
                    throw Errors.createTypeErrorHookReturnValue("Finish",this);
                }
            }
        }
    }

    private List<ElementDescriptor> createList() {
        return new ArrayList<>();
    }
}
