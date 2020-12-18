package com.oracle.truffle.js.nodes.decorators;

import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.builtins.JSOrdinary;
import com.oracle.truffle.js.runtime.objects.JSOrdinaryObject;
import com.oracle.truffle.js.runtime.objects.JSProperty;
import com.oracle.truffle.js.runtime.objects.PropertyDescriptor;
import com.sun.org.apache.bcel.internal.generic.JSR;

import java.util.Locale;

public class DescriptorUtil {
    private static final String ELEMENT_DESCRIPTOR_VALUE = "Descriptor";
    private static final String KIND = "kind";
    private static final String VALUE = "method";
    private static final String WRITABLE = "writable";
    private static final String GET = "get";
    private static final String SET = "set";
    private static final String ENUMERABLE = "enumerable";
    private static final String CONFIGURABLE = "configurable";
    private static final String KEY = "key";
    private static final String PLACEMENT = "placement";
    private static final String INITIALIZE = "initialize";
    private static final String START = "start";
    private static final String REPLACE = "replace";
    private static final String FINISH = "finish";
    private static final String ELEMENTS = "elements";

    public static Object fromElementDescriptor(ElementDescriptor element, JSContext context) {
        DynamicObject obj = JSOrdinary.create(context);
        PropertyDescriptor desc = PropertyDescriptor.createData(ELEMENT_DESCRIPTOR_VALUE,false, false, true);
        JSRuntime.definePropertyOrThrow(obj,ELEMENT_DESCRIPTOR_VALUE.toLowerCase(Locale.ROOT),desc);
        JSRuntime.createDataPropertyOrThrow(obj, KIND, element.getKindString());
        if(element.hasDescriptor()) {
            PropertyDescriptor descriptor = element.getDescriptor();
            if(descriptor.hasValue()) {
                JSRuntime.createDataProperty(obj, VALUE, descriptor.getValue());
            }
            if(descriptor.hasWritable()) {
                JSRuntime.createDataProperty(obj, WRITABLE, descriptor.getWritable());
            }
            if(descriptor.hasGet()) {
                JSRuntime.createDataProperty(obj, GET, descriptor.getGet());
            }
            if(descriptor.hasSet()) {
                JSRuntime.createDataProperty(obj, SET, descriptor.getSet());
            }
            if(descriptor.hasEnumerable()) {
                JSRuntime.createDataProperty(obj, ENUMERABLE, descriptor.getEnumerable());
            }
            if(descriptor.hasConfigurable()) {
                JSRuntime.createDataProperty(obj, CONFIGURABLE, descriptor.getConfigurable());
            }
        }
        if(element.isMethod() || element.isAccessor()|| element.isField()) {
            //TODO: get private name
            JSRuntime.createDataPropertyOrThrow(obj, KEY, element.getKey());
        }
        JSRuntime.createDataPropertyOrThrow(obj, PLACEMENT, element.getPlacementString());
        if(element.isField()) {
            JSRuntime.createDataPropertyOrThrow(obj, INITIALIZE,JSRuntime.nullToUndefined(element.getInitialize()));
        }
        if(element.isHook()) {
            Object start = element.getStart();
            assert start != null;
            JSRuntime.createDataPropertyOrThrow(obj, START, start);
        }
        return obj;
    }

    public static ElementDescriptor toElementDescriptor(Object e) {
        assert JSRuntime.isObject(e);
        DynamicObject elementObject = (DynamicObject) e;
        int kind = JSKind.fromString(JSRuntime.toString(JSOrdinaryObject.get(elementObject, KIND)));
        if(!JSKind.isHook(kind) && !JSKind.isMethod(kind) && !JSKind.isAccessor(kind) && !JSKind.isField(kind)) {
            throw Errors.createTypeError("Property kind of element descriptor must be one of 'hook', 'method', 'accessor' or 'field'.");
        }
        Object key = JSOrdinaryObject.get(elementObject, KEY);
        if(JSKind.isHook(kind)) {
            if(!JSRuntime.isNullOrUndefined(key)) {
                throw Errors.createTypeError("Element descriptor with kind 'hook' must not have property key.");
            }
        }
        //TODO: check if key is private
        //TODO: isPrivate
        key = JSRuntime.toPropertyKey(key);

        int placement = JSPlacement.fromString(JSRuntime.toString(JSOrdinaryObject.get(elementObject, PLACEMENT)));
        if(!JSPlacement.isStatic(placement) && !JSPlacement.isPrototype(placement) && !JSPlacement.isOwn(placement)) {
            throw Errors.createTypeError("Property placement of element descriptor must be one of 'static', 'prototype' or 'own'.");
        }
        PropertyDescriptor descriptor = toDecoratorPropertyDescriptor(elementObject);
        //TODO: 11.
        if((JSKind.isAccessor(kind) || JSKind.isHook(kind)) && descriptor.isDataDescriptor()) {
            throw Errors.createTypeError("Property descriptor of element descriptor must not be a data descriptor of property kind is 'accessor' or 'hook'.");
        }
        if((JSKind.isField(kind) || JSKind.isMethod(kind) ||JSKind.isHook(kind)) && descriptor.isAccessorDescriptor()) {
            throw Errors.createTypeError("Property descriptor of element descriptor must not be an accessor descriptor of property kind is 'field', 'method' or 'hook'.");
        }
        if(JSKind.isField(kind) && descriptor.hasValue()) {
            throw Errors.createTypeError("Data descriptor of element descriptor must not have a value property if property kind is 'field'.");
        }
        Object initialize = JSOrdinaryObject.get(elementObject, INITIALIZE);
        if(!JSRuntime.isNullOrUndefined(initialize)) {
            if(!JSRuntime.isCallable(initialize)) {
                throw Errors.createTypeError("Property initialize of element descriptor must be callable.");
            }
            if(!JSKind.isField(kind)) {
                throw Errors.createTypeError("Element descriptor without kind 'field' must not have property initialize.");
            }
        }
        Object start = JSOrdinaryObject.get(elementObject, START);
        if(!JSRuntime.isNullOrUndefined(start)) {
            if(!JSRuntime.isCallable(start)) {
                throw Errors.createTypeError("Property start of element descriptor must be callable.");
            }
            if(!JSKind.isHook(kind)) {
                throw Errors.createTypeError("Element descriptor without kind 'hook' must not have property start.");
            }
        }
        Object replace = JSOrdinaryObject.get(elementObject, REPLACE);
        if(!JSRuntime.isNullOrUndefined(replace)) {
            if(!JSRuntime.isCallable(replace)) {
                throw Errors.createTypeError("Property replace of element descriptor must be callable.");
            }
            if(!JSKind.isHook(kind)) {
                throw Errors.createTypeError("Element descriptor without kind 'hook' must not have property replace.");
            }
        }
        Object finish = JSOrdinaryObject.get(elementObject, FINISH);
        if(!JSRuntime.isNullOrUndefined(finish)) {
            if(!JSRuntime.isCallable(finish)) {
                throw Errors.createTypeError("Property finish of element descriptor must be callable.");
            }
            if(!JSKind.isHook(kind)) {
                throw Errors.createTypeError("Element descriptor without kind 'hook' must not have property finish.");
            }
        }

        if(JSKind.isHook(kind)) {
            if(JSRuntime.isNullOrUndefined(start) && JSRuntime.isNullOrUndefined(replace) && JSRuntime.isNullOrUndefined(finish)) {
                throw Errors.createTypeError("Element descriptor with kind 'hook' must have at least one of property start, replace or finish.");
            }
            if(!JSRuntime.isNullOrUndefined(replace) && !JSRuntime.isNullOrUndefined(finish)) {
                throw Errors.createTypeError("Properties replace and finish cannot both be present.");
            }
            if(JSPlacement.isOwn(placement) && (!JSRuntime.isNullOrUndefined(replace) || !JSRuntime.isNullOrUndefined(finish))) {
                throw Errors.createTypeError("Element descriptor with kind 'hook' and placement 'own' must not have properties replace and finish.");
            }
            if(JSPlacement.isPrototype(placement) && !JSRuntime.isNullOrUndefined(replace)) {
                throw Errors.createTypeError("Element descriptor with kind 'hook' and placement 'own' must not have property replace.");
            }
        }
        Object elements = JSOrdinaryObject.get(elementObject, ELEMENTS);
        if(!JSRuntime.isNullOrUndefined(elements)) {
            throw Errors.createTypeError("Element descriptor must not have property elements.");
        }
        if(JSKind.isMethod(kind)) {
            return ElementDescriptor.createMethod(key,descriptor,placement,false);
        }
        if(JSKind.isField(kind)) {
            return ElementDescriptor.createField(key, descriptor, placement,initialize, false);
        }
        if(JSKind.isAccessor(kind)) {
            return ElementDescriptor.createAccessor(key, descriptor,placement, false);
        }
        if(JSKind.isHook(kind)) {
            return ElementDescriptor.createHook(placement,null,null,null);
        }
        return null;
    }

    public static PropertyDescriptor toDecoratorPropertyDescriptor(Object elementObject){
        PropertyDescriptor desc = PropertyDescriptor.createEmpty();
        DynamicObject obj = (DynamicObject) elementObject;
        if(JSOrdinaryObject.hasProperty(obj, "enumerable")) {
            desc.setEnumerable(JSRuntime.toBoolean(JSOrdinaryObject.get(obj, "enumerable")));
        }
        if(JSOrdinaryObject.hasProperty(obj, "configurable")) {
            desc.setConfigurable(JSRuntime.toBoolean(JSOrdinaryObject.get(obj, "configurable")));
        }
        if(JSOrdinaryObject.hasProperty(obj, "method")) {
            Object value = JSOrdinaryObject.get(obj, "method");
            if(!JSRuntime.isCallable(value)) {
                //TODO: throw TypeError
            }
            desc.setValue(value);
        }
        if(JSOrdinaryObject.hasProperty(obj, "writable")) {
            desc.setWritable(JSRuntime.toBoolean(JSOrdinaryObject.get(obj, "writable")));
        }
        if(JSOrdinaryObject.hasProperty(obj, "get")) {
            Object getter = JSOrdinaryObject.get(obj, "get");
            if(!JSRuntime.isCallable(getter) && !JSRuntime.isNullOrUndefined(getter)) {
                //TODO: throw TypeError
            }
            desc.setGet((DynamicObject) getter);
        }
        if(JSOrdinaryObject.hasProperty(obj, "set")) {
            Object setter = JSOrdinaryObject.get(obj, "set");
            if(!JSRuntime.isCallable(setter) && !JSRuntime.isNullOrUndefined(setter)) {
                //TODO: throw TypeError
            }
            desc.setSet((DynamicObject) setter);
        }
        if(desc.isDataDescriptor() && desc.isAccessorDescriptor()) {
            //TODO: throw TypeError
        }
        //TODO: CompletePropertyDescriptor
        return desc;
    }
}
