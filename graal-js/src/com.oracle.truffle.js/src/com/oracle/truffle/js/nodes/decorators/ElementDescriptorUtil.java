package com.oracle.truffle.js.nodes.decorators;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
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

    @TruffleBoundary
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

    @TruffleBoundary
    public static ElementDescriptor toElementDescriptor(Object e, Node originatingNode) {
        assert JSRuntime.isObject(e);
        DynamicObject elementObject = (DynamicObject) e;
        int kind = JSKind.fromString(JSRuntime.toString(JSOrdinaryObject.get(elementObject, KIND)));
        if(!JSKind.isHook(kind) && !JSKind.isMethod(kind) && !JSKind.isAccessor(kind) && !JSKind.isField(kind)) {
            throw Errors.createTypeErrorElementDescriptorProperty("kind","must be one of 'hook', 'method', 'accessor' or 'field'", originatingNode);
        }
        Object key = JSOrdinaryObject.get(elementObject, KEY);
        if(JSKind.isHook(kind) && key != Undefined.instance) {
            throw Errors.createTypeErrorElementDescriptorPropertyDescriptor("kind 'hook'", "must not have property key",originatingNode);
        }
        boolean hasPrivateKey = key instanceof PrivateName;
        if(!hasPrivateKey) {
            key = JSRuntime.toPropertyKey(key);
        }

        int placement = JSPlacement.fromString(JSRuntime.toString(JSOrdinaryObject.get(elementObject, PLACEMENT)));
        if(!JSPlacement.isStatic(placement) && !JSPlacement.isPrototype(placement) && !JSPlacement.isOwn(placement)) {
            throw Errors.createTypeErrorElementDescriptorProperty("placement", "must be one of 'static', 'prototype' or 'own'", originatingNode);
        }
        PropertyDescriptor descriptor = toDecoratorPropertyDescriptor(elementObject, kind, originatingNode);
        if(hasPrivateKey) {
            if(descriptor.hasEnumerable() && descriptor.getEnumerable()) {
                throw Errors.createTypeErrorElementDescriptorRestriction("private key", "must not be enumerable", originatingNode);
            }
            if(descriptor.hasConfigurable() && descriptor.getConfigurable()) {
                throw Errors.createTypeErrorElementDescriptorRestriction("private key" , "must not be configurable", originatingNode);
            }
            if(JSPlacement.isPrototype(placement)) {
                throw Errors.createTypeErrorElementDescriptorRestriction("private key", "must not have placement 'prototype'", originatingNode);
            }
        }
        if((JSKind.isAccessor(kind) || JSKind.isHook(kind)) && descriptor.isDataDescriptor()) {
            throw Errors.createTypeErrorElementDescriptorPropertyDescriptor("kind 'accessor' or 'hook'", "must not be a data descriptor", originatingNode);
        }
        if((JSKind.isField(kind) || JSKind.isMethod(kind) || JSKind.isHook(kind)) && descriptor.isAccessorDescriptor()) {
            throw Errors.createTypeErrorElementDescriptorPropertyDescriptor("kind 'field', 'method' or 'hook'", "must not be an accessor descriptor", originatingNode);
        }
        if(JSKind.isField(kind) && descriptor.hasValue()) {
            throw Errors.createTypeErrorElementDescriptorPropertyDescriptor("kind 'field'","must not have a value property", originatingNode);
        }
        Object initialize = JSOrdinaryObject.get(elementObject, INITIALIZE);
        if(initialize != Undefined.instance) {
            if(!JSRuntime.isCallable(initialize)) {
                throw Errors.createTypeErrorElementDescriptorPropertyRestriction("initialize","other than undefined","must be callable", originatingNode);
            }
            if(!JSKind.isField(kind)) {
                throw Errors.createTypeErrorElementDescriptorPropertyRestriction("initialize", "other than undefined", "requires element descriptor kind 'field'", originatingNode);
            }
        }
        Object start = JSOrdinaryObject.get(elementObject, START);
        if(start != Undefined.instance) {
            if(!JSRuntime.isCallable(start)) {
                throw Errors.createTypeErrorElementDescriptorPropertyRestriction("start","other than undefined","must be callable", originatingNode);
            }
            if(!JSKind.isHook(kind)) {
                throw Errors.createTypeErrorElementDescriptorPropertyRestriction("start", "other than undefined", "requires element descriptor kind 'hook'", originatingNode);
            }
        }
        Object replace = JSOrdinaryObject.get(elementObject, REPLACE);
        if(replace != Undefined.instance) {
            if(!JSRuntime.isCallable(replace)) {
                throw Errors.createTypeErrorElementDescriptorPropertyRestriction("replace","other than undefined","must be callable", originatingNode);
            }
            if(!JSKind.isHook(kind)) {
                throw Errors.createTypeErrorElementDescriptorPropertyRestriction("replace", "other than undefined", "requires element descriptor kind 'hook'", originatingNode);
            }
        }
        Object finish = JSOrdinaryObject.get(elementObject, FINISH);
        if(finish != Undefined.instance) {
            if(!JSRuntime.isCallable(finish)) {
                throw Errors.createTypeErrorElementDescriptorPropertyRestriction("finish","other than undefined","must be callable", originatingNode);

            }
            if(!JSKind.isHook(kind)) {
                throw Errors.createTypeErrorElementDescriptorPropertyRestriction("finish", "other than undefined", "requires element descriptor kind 'hook'", originatingNode);

            }
        }

        if(JSKind.isHook(kind)) {
            if(start == Undefined.instance && replace == Undefined.instance && finish == Undefined.instance) {
                throw Errors.createTypeErrorElementDescriptorProperty("kind 'hook'","must have at least one of start, replace or finish", originatingNode);

            }
            if(replace != Undefined.instance && finish != Undefined.instance) {
                throw Errors.createTypeError("Properties replace and finish cannot both be present on element descriptor.", originatingNode);

            }
            if(JSPlacement.isOwn(placement) && (replace != Undefined.instance || finish != Undefined.instance)) {
                throw Errors.createTypeErrorElementDescriptorProperty("kind 'hook' and placement 'own'", "must not have properties replace and finish", originatingNode);

            }
            if(JSPlacement.isPrototype(placement) && replace != Undefined.instance) {
                throw Errors.createTypeErrorElementDescriptorProperty("kind 'hook' and placement 'prototype'", "must not have property replace", originatingNode);

            }
        }
        Object elements = JSOrdinaryObject.get(elementObject, ELEMENTS);
        if(elements != Undefined.instance) {
            throw Errors.createTypeError("Element descriptor must not have property elements.");

        }
        // isPrivate is false, since all checks are already performed before the call.
        if(JSKind.isMethod(kind)) {
            return ElementDescriptor.createMethod(key, descriptor, placement,false, originatingNode);
        }
        if(JSKind.isField(kind)) {
            return ElementDescriptor.createField(key, descriptor, placement, initialize, false, originatingNode);
        }
        if(JSKind.isAccessor(kind)) {
            return ElementDescriptor.createAccessor(key, descriptor, placement, false, originatingNode);
        }
        if(JSKind.isHook(kind)) {
            return ElementDescriptor.createHook(placement, start, replace, finish, originatingNode);
        }
        throw Errors.shouldNotReachHere();
    }

    @TruffleBoundary
    public static PropertyDescriptor toDecoratorPropertyDescriptor(Object elementObject, int kind, Node originatingNode){
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
                throw Errors.createTypeErrorPropertyDescriptor("method", "must be callable", originatingNode);
            }
            desc.setValue(value);
        }
        if(JSOrdinaryObject.hasProperty(obj, WRITABLE)) {
            desc.setWritable(JSRuntime.toBoolean(JSOrdinaryObject.get(obj, WRITABLE)));
        }
        if(JSOrdinaryObject.hasProperty(obj, GET)) {
            Object getter = JSOrdinaryObject.get(obj, GET);
            if(!JSRuntime.isCallable(getter) && getter != Undefined.instance) {
                throw Errors.createTypeErrorPropertyDescriptor("get", "must be callable", originatingNode);
            }
            desc.setGet((DynamicObject) getter);
        }
        if(JSOrdinaryObject.hasProperty(obj, SET)) {
            Object setter = JSOrdinaryObject.get(obj, SET);
            if(!JSRuntime.isCallable(setter) && setter != Undefined.instance) {
                throw Errors.createTypeErrorPropertyDescriptor("set", "must be callable", originatingNode);
            }
            desc.setSet((DynamicObject) setter);
        }
        if(desc.isDataDescriptor() && desc.isAccessorDescriptor()) {
            throw Errors.createTypeError("Property descriptor can not be both accessor and data descriptor.", originatingNode);
        }
        completePropertyDescriptor(desc, kind);
        return desc;
    }

    @TruffleBoundary
    private static void completePropertyDescriptor(PropertyDescriptor desc, int kind) {
        if(JSKind.isField(kind) || JSKind.isMethod(kind)) {
            if(!desc.hasWritable()) {
                desc.setWritable(false);
            }
        }
        if(JSKind.isMethod(kind)) {
            if(!desc.hasValue()) {
                desc.setValue(Undefined.instance);
            }
        }
        if(JSKind.isAccessor(kind)){
            if(!desc.hasGet()) {
                desc.setGet(Undefined.instance);
            }
            if(!desc.hasSet()) {
                desc.setSet(Undefined.instance);
            }
        }
        if(!desc.hasEnumerable()) {
            desc.setEnumerable(false);
        }
        if(!desc.hasConfigurable()) {
            desc.setConfigurable(false);
        }
    }

    @TruffleBoundary
    public static Object fromClassDescriptor(ClassElementList elements, JSContext context) {
        Object[] elementObjects = new Object[elements.size()];
        int i = 0;
        for (ElementDescriptor element: elements.getOwnElements()){
            elementObjects[i++] = fromElementDescriptor(element,context);
        }
        for(ElementDescriptor element: elements.getStaticAndPrototypeElements()) {
            elementObjects[i++] = fromElementDescriptor(element,context);
        }
        DynamicObject obj = JSOrdinary.create(context);
        PropertyDescriptor desc = PropertyDescriptor.createData(ELEMENT_DESCRIPTOR_VALUE,false,false,true);
        JSRuntime.definePropertyOrThrow(obj, Symbol.SYMBOL_TO_STRING_TAG, desc);
        JSRuntime.createDataPropertyOrThrow(obj, KIND, CLASS);
        JSRuntime.createDataPropertyOrThrow(obj, ELEMENTS, JSRuntime.createArrayFromList(context, Arrays.asList(elementObjects.clone())));
        return obj;
    }

    @TruffleBoundary
    public static void checkClassDescriptor(Object classDescriptor, Node originatingNode) {
        DynamicObject obj = (DynamicObject) classDescriptor;
        int kind = JSKind.fromString(JSRuntime.toString(JSOrdinaryObject.get(obj,KIND)));
        if(!JSKind.isClass(kind)) {
            throw Errors.createTypeError("Class descriptor must have kind 'class'.", originatingNode);
        }
        Object key = JSOrdinaryObject.get(obj, KEY);
        if(key != Undefined.instance) {
            throw Errors.createTypeErrorClassDescriptor(KEY, originatingNode);
        }
        Object placement = JSOrdinaryObject.get(obj, PLACEMENT);
        if(placement != Undefined.instance) {
            throw Errors.createTypeErrorClassDescriptor(PLACEMENT, originatingNode);
        }
        PropertyDescriptor descriptor = JSRuntime.toPropertyDescriptor(obj);
        if(descriptor.hasConfigurable() || descriptor.hasGet() || descriptor.hasSet() || descriptor.hasValue() || descriptor.hasWritable() || descriptor.hasEnumerable())
        {
            //Can not be tested
            throw Errors.createTypeError("Property descriptor of class descriptor must either be empty or undefined.", originatingNode);
        }
        Object initialize = JSOrdinaryObject.get(obj, INITIALIZE);
        if(initialize != Undefined.instance) {
            throw Errors.createTypeErrorClassDescriptor(INITIALIZE, originatingNode);
        }
        Object start = JSOrdinaryObject.get(obj, START);
        if(start != Undefined.instance) {
            throw Errors.createTypeErrorClassDescriptor(START, originatingNode);
        }
        Object extras = JSOrdinaryObject.get(obj, EXTRAS);
        if(extras != Undefined.instance) {
            throw Errors.createTypeErrorClassDescriptor(EXTRAS, originatingNode);
        }
        Object finish = JSOrdinaryObject.get(obj, FINISH);
        if(finish != Undefined.instance) {
            throw Errors.createTypeErrorClassDescriptor(FINISH, originatingNode);
        }
        Object replace = JSOrdinaryObject.get(obj, REPLACE);
        if(replace != Undefined.instance) {
            throw Errors.createTypeErrorClassDescriptor(REPLACE, originatingNode);
        }
    }
}
