package com.oracle.truffle.js.nodes.decorators;

import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.builtins.JSOrdinary;
import com.oracle.truffle.js.runtime.objects.JSOrdinaryObject;
import com.oracle.truffle.js.runtime.objects.PropertyDescriptor;
import com.oracle.truffle.js.runtime.objects.Undefined;

import java.util.Arrays;

public class ElementDescriptorUtil {
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
    private static final String CLASS = "class";
    private static final String EXTRAS = "extras";

    public static Object fromElementDescriptor(ElementDescriptor element, JSContext context) {
        DynamicObject obj = JSOrdinary.create(context);
        PropertyDescriptor desc = PropertyDescriptor.createData(ELEMENT_DESCRIPTOR_VALUE,false, false, true);
        JSRuntime.definePropertyOrThrow(obj,Symbol.SYMBOL_TO_STRING_TAG,desc);
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

    public static ElementDescriptor toElementDescriptor(Object e, Node originatingNode) {
        assert JSRuntime.isObject(e);
        DynamicObject elementObject = (DynamicObject) e;
        int kind = JSKind.fromString(JSRuntime.toString(JSOrdinaryObject.get(elementObject, KIND)));
        if(!JSKind.isHook(kind) && !JSKind.isMethod(kind) && !JSKind.isAccessor(kind) && !JSKind.isField(kind)) {
            throw Errors.createTypeErrorElementDescriptorProperty("kind","'hook', 'method', 'accessor' or 'field'", originatingNode);
            //TODO: test
        }
        Object key = JSOrdinaryObject.get(elementObject, KEY);
        if(JSKind.isHook(kind) && key != Undefined.instance) {
            throw Errors.createTypeErrorElementDescriptorPropertyDescriptor("kind 'hook'", "must not have property key",originatingNode);
            //TODO: test
        }
        boolean hasPrivateKey = key instanceof PrivateName;
        if(!hasPrivateKey) {
            key = JSRuntime.toPropertyKey(key);
        }

        int placement = JSPlacement.fromString(JSRuntime.toString(JSOrdinaryObject.get(elementObject, PLACEMENT)));
        if(!JSPlacement.isStatic(placement) && !JSPlacement.isPrototype(placement) && !JSPlacement.isOwn(placement)) {
            throw Errors.createTypeErrorElementDescriptorProperty("placement", "'static', 'prototype' or 'own'", originatingNode);
            //TODO: test
        }
        PropertyDescriptor descriptor = toDecoratorPropertyDescriptor(elementObject);
        if(hasPrivateKey) {
            if(descriptor.hasEnumerable() && descriptor.getEnumerable()) {
                throw Errors.createTypeErrorElementDescriptorRestriction("private key", "must not be enumerable", originatingNode);
                //TODO: test
            }
            if(descriptor.hasConfigurable() && descriptor.getConfigurable()) {
                throw Errors.createTypeErrorElementDescriptorRestriction("private key" , "must not be configurable", originatingNode);
                //TODO: test
            }
            if(JSPlacement.isPrototype(placement)) {
                throw Errors.createTypeErrorElementDescriptorRestriction("private key", "must not have placement 'prototype'", originatingNode);
                //TODO: test
            }
        }
        if((JSKind.isAccessor(kind) || JSKind.isHook(kind)) && descriptor.isDataDescriptor()) {
            throw Errors.createTypeErrorElementDescriptorPropertyDescriptor("kind 'accessor' or 'hook'", "must not be a data descriptor", originatingNode);
            //TODO: test
        }
        if((JSKind.isField(kind) || JSKind.isMethod(kind) ||JSKind.isHook(kind)) && descriptor.isAccessorDescriptor()) {
            throw Errors.createTypeErrorElementDescriptorPropertyDescriptor("kind 'field', 'method' or 'hook'", "must not be an accessor descriptor.", originatingNode);
            //TODO: test
        }
        if(JSKind.isField(kind) && descriptor.hasValue()) {
            throw Errors.createTypeErrorElementDescriptorPropertyDescriptor("kind 'field'","must not have a value property", originatingNode);
            //TODO: test
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
        //isPrivate is false, since all checks are already performed before the call.
        if(JSKind.isMethod(kind)) {
            return ElementDescriptor.createMethod(key,descriptor,placement,false, originatingNode);
        }
        if(JSKind.isField(kind)) {
            return ElementDescriptor.createField(key, descriptor, placement,initialize, false, originatingNode);
        }
        if(JSKind.isAccessor(kind)) {
            return ElementDescriptor.createAccessor(key, descriptor,placement, false, originatingNode);
        }
        if(JSKind.isHook(kind)) {
            return ElementDescriptor.createHook(placement,null,null,null, originatingNode);
        }
        return null;
    }

    public static PropertyDescriptor toDecoratorPropertyDescriptor(Object elementObject){
        PropertyDescriptor desc = PropertyDescriptor.createEmpty();
        DynamicObject obj = (DynamicObject) elementObject;
        if(JSOrdinaryObject.hasProperty(obj, ENUMERABLE)) {
            desc.setEnumerable(JSRuntime.toBoolean(JSOrdinaryObject.get(obj, ENUMERABLE)));
        }
        if(JSOrdinaryObject.hasProperty(obj, CONFIGURABLE)) {
            desc.setConfigurable(JSRuntime.toBoolean(JSOrdinaryObject.get(obj, CONFIGURABLE)));
        }
        if(JSOrdinaryObject.hasProperty(obj, VALUE)) {
            Object value = JSOrdinaryObject.get(obj, VALUE);
            if(!JSRuntime.isCallable(value)) {
                throw Errors.createTypeError("Property method of property descriptor must be callable.");
            }
            desc.setValue(value);
        }
        if(JSOrdinaryObject.hasProperty(obj, WRITABLE)) {
            desc.setWritable(JSRuntime.toBoolean(JSOrdinaryObject.get(obj, WRITABLE)));
        }
        if(JSOrdinaryObject.hasProperty(obj, GET)) {
            Object getter = JSOrdinaryObject.get(obj, GET);
            if(!JSRuntime.isCallable(getter) && !JSRuntime.isNullOrUndefined(getter)) {
                throw Errors.createTypeError("Property get of property descriptor must be callable.");
            }
            desc.setGet((DynamicObject) getter);
        }
        if(JSOrdinaryObject.hasProperty(obj, SET)) {
            Object setter = JSOrdinaryObject.get(obj, SET);
            if(!JSRuntime.isCallable(setter) && !JSRuntime.isNullOrUndefined(setter)) {
                throw Errors.createTypeError("Property set of property descriptor must be callable.");
            }
            desc.setSet((DynamicObject) setter);
        }
        if(desc.isDataDescriptor() && desc.isAccessorDescriptor()) {
            throw Errors.createTypeError("Property descriptor can not be both accessor and data descriptor.");
        }
        return desc;
    }

    public static Object fromClassDescriptor(ClassElementList elements, JSContext context) {
        Object[] elementObjects = new Object[elements.size()];
        for (int i = 0; i < elements.size(); i++){
            elementObjects[i] = fromElementDescriptor(elements.pop(),context);
        }
        DynamicObject obj = JSOrdinary.create(context);
        PropertyDescriptor desc = PropertyDescriptor.createData(ELEMENT_DESCRIPTOR_VALUE,false,false,true);
        JSRuntime.definePropertyOrThrow(obj, Symbol.SYMBOL_TO_STRING_TAG, desc);
        JSRuntime.createDataPropertyOrThrow(obj, KIND, CLASS);
        JSRuntime.createDataPropertyOrThrow(obj, ELEMENTS, JSRuntime.createArrayFromList(context, Arrays.asList(elementObjects.clone())));
        return obj;
    }

    public static void checkClassDescriptor(Object classDescriptor) {
        DynamicObject obj = (DynamicObject) classDescriptor;
        int kind = JSKind.fromString(JSRuntime.toString(JSOrdinaryObject.get(obj,KIND)));
        if(!JSKind.isClass(kind)) {
            throw Errors.createTypeError("Class descriptor must have kind 'class'.");
        }
        Object key = JSOrdinaryObject.get(obj, KEY);
        if(!JSRuntime.isNullOrUndefined(key)) {
            throw Errors.createTypeError("Class descriptor must not have property key.");
        }
        Object placement = JSOrdinaryObject.get(obj, PLACEMENT);
        if(!JSRuntime.isNullOrUndefined(placement)) {
            throw Errors.createTypeError("Class descriptor must not have property placement.");
        }
        PropertyDescriptor descriptor = JSRuntime.toPropertyDescriptor(obj);
        if(descriptor.hasConfigurable() || descriptor.hasGet() || descriptor.hasSet() || descriptor.hasValue() ||descriptor.hasWritable() ||descriptor.hasEnumerable())
        {
            throw Errors.createTypeError("Property descriptor of class descriptor must either be empty or undefined.");
        }
        Object initialize = JSOrdinaryObject.get(obj, INITIALIZE);
        if(!JSRuntime.isNullOrUndefined(initialize)) {
            throw Errors.createTypeError("Class descriptor must not have property initialize.");
        }
        Object start = JSOrdinaryObject.get(obj, START);
        if(!JSRuntime.isNullOrUndefined(start)) {
            throw Errors.createTypeError("Class descriptor must not have property start.");
        }
        Object extras = JSOrdinaryObject.get(obj, EXTRAS);
        if(!JSRuntime.isNullOrUndefined(extras)) {
            throw Errors.createTypeError("Class descriptor must not have property extras.");
        }
        Object finish = JSOrdinaryObject.get(obj, FINISH);
        if(!JSRuntime.isNullOrUndefined(finish)) {
            throw Errors.createTypeError("Class descriptor must not have property finish.");
        }
        Object replace = JSOrdinaryObject.get(obj, REPLACE);
        if(!JSRuntime.isNullOrUndefined(replace)) {
            throw Errors.createTypeError("Class descriptor must not have property replace.");
        }
    }
}
