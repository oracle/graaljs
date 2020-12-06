package com.oracle.truffle.js.nodes.decorators;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node.Child;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Property;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.builtins.JSFunctionObject;
import com.oracle.truffle.js.runtime.builtins.JSOrdinary;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSOrdinaryObject;
import com.oracle.truffle.js.runtime.objects.PropertyDescriptor;

public class ElementDecoratorNode extends JavaScriptBaseNode {
    @Child
    JavaScriptNode expressionNode;
    @Child
    JSFunctionCallNode functionNode;

    public ElementDecoratorNode(JavaScriptNode expressionNode) {
        this.expressionNode = expressionNode;
        this.functionNode = JSFunctionCallNode.createCall();
    }

    public ElementDescriptor[] executeDecorator(VirtualFrame frame, ElementDescriptor element, JSContext context) {
        if(element.isHook()) {
            //TODO: throw TypeError
        }
        Object elementObject = fromElementDescriptor(element, context);
        JSFunctionObject function = (JSFunctionObject) expressionNode.execute(frame);
        Object elementExtrasObject = functionNode.executeCall(JSArguments.createOneArg(null, function, elementObject));
        if(JSRuntime.isNullOrUndefined(elementExtrasObject)) {
            return new ElementDescriptor[]{ element };
        }
        return new ElementDescriptor[] { toElementDescriptor(elementExtrasObject) };
    }

    private Object fromElementDescriptor(ElementDescriptor element, JSContext context) {
        DynamicObject obj = JSOrdinary.create(context);
        PropertyDescriptor desc = PropertyDescriptor.createData("Descriptor",false, false, true);
        JSRuntime.definePropertyOrThrow(obj,"desc",desc);
        JSRuntime.createDataProperty(obj, "kind", element.getKindString());
        if(element.getDescriptor() != null) {
            PropertyDescriptor descriptor = element.getDescriptor();
            if(descriptor.hasValue()) {
                JSRuntime.createDataProperty(obj, "method", descriptor.getValue());
            }
            if(descriptor.hasWritable()) {
                JSRuntime.createDataProperty(obj, "writable", descriptor.getWritable());
            }
            if(descriptor.hasGet()) {
                JSRuntime.createDataProperty(obj, "get", descriptor.getGet());
            }
            if(descriptor.hasSet()) {
                JSRuntime.createDataProperty(obj, "set", descriptor.getSet());
            }
            if(descriptor.hasEnumerable()) {
                JSRuntime.createDataProperty(obj, "enumerable", descriptor.getEnumerable());
            }
            if(descriptor.hasConfigurable()) {
                JSRuntime.createDataProperty(obj, "configurable", descriptor.getConfigurable());
            }
        }
        if(element.isMethod() || element.isAccessor()|| element.isField()) {
            JSRuntime.createDataProperty(obj, "key", element.getKey());
        }
        JSRuntime.createDataProperty(obj, "placement", element.getPlacementString());
        if(element.isField()) {
            JSRuntime.createDataProperty(obj, "initialize",JSRuntime.nullToUndefined(element.getInitialize()));
        }
        if(element.isHook()) {
            Object start = element.getStart();
            assert start != null;
            JSRuntime.createDataProperty(obj, "start", start);
        }
        return obj;
    }

    private ElementDescriptor toElementDescriptor(Object e) {
        assert JSRuntime.isObject(e);
        DynamicObject elementObject = (DynamicObject) e;
        int kind = JSKind.fromString(JSRuntime.toString(JSOrdinaryObject.get(elementObject, "kind")));
        if(!JSKind.isHook(kind) && !JSKind.isMethod(kind) && !JSKind.isAccessor(kind) && !JSKind.isField(kind)) {
            //TODO: throw TypeError
        }
        Object key = JSOrdinaryObject.get(elementObject, "key");
        if(JSKind.isHook(kind)) {
            if(!JSRuntime.isNullOrUndefined(key)) {
                //TODO: throw TypeError
            }
        }
        //TODO: check if key is private
        key = JSRuntime.toPropertyKey(key);
        int placement = JSPlacement.fromString(JSRuntime.toString(JSOrdinaryObject.get(elementObject, "placement")));
        if(!JSPlacement.isStatic(placement) && !JSPlacement.isPrototype(placement) && !JSPlacement.isOwn(placement)) {
            //TODO: throw TypeError
        }
        PropertyDescriptor descriptor = toDecoratorPropertyDescriptor(elementObject);
        //TODO: 11. - 14.
        Object initialize = JSOrdinaryObject.get(elementObject, "initialize");
        if(!JSRuntime.isNullOrUndefined(initialize) && !JSRuntime.isCallable(initialize)) {
            //TODO: throw TypeError
        }
        if(JSKind.isField(kind) && !JSRuntime.isNullOrUndefined(initialize)){
                //TODO: throw TypeError
        }
        //TODO: 18. - 28.
        //TODO: isPrivate
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

    private PropertyDescriptor toDecoratorPropertyDescriptor(Object elementObject){
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
        if((desc.hasGet() || desc.hasSet()) && (desc.hasValue() || desc.hasWritable())) {
            //TODO: throw TypeError
        }
        //TODO: CompletePropertyDescriptor
        return desc;
    }
}
