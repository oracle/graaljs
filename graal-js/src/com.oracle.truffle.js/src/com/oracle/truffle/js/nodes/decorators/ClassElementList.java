package com.oracle.truffle.js.nodes.decorators;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.js.runtime.Errors;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ClassElementList {
    private final List<ElementDescriptor> ownElements;
    private List<ElementDescriptor> staticAndPrototypeElements;
    private int prototypeAndStaticFieldCount = 0;
    private int ownFieldCount = 0;
    private int ownHookStartCount = 0;
    private boolean methodOrAccessorWithPrivateKey = false;

    private final Set<Object> staticKeys;
    private final Set<Object> ownKeys;
    private final Set<Object> prototypeKeys;

    private ClassElementList() {
        ownElements = new ArrayList<>();
        staticAndPrototypeElements = new ArrayList<>();
        staticKeys = new HashSet<>();
        ownKeys = new HashSet<>();
        prototypeKeys = new HashSet<>();
    }

    @TruffleBoundary
    public static ClassElementList create() {
        return new ClassElementList();
    }

    @TruffleBoundary
    public void enqueue(ElementDescriptor e) {
        enqueue(e, true, null, true);
    }

    @TruffleBoundary
    public void enqueue(ElementDescriptor e, boolean isSilent, Node originatingNode, boolean check) {
        if(check) {
            addElementPlacement(e, isSilent, originatingNode);
        }
        if(methodOrAccessorWithPrivateKey || (e.isMethod() || e.isAccessor()) && e.hasKey() && e.hasPrivateKey()) {
            methodOrAccessorWithPrivateKey = true;
        }
        if(e.isOwn()) {
            if(e.isField()) {
                ownFieldCount++;
            }
            if(e.isHook() && e.hasStart()) {
                ownHookStartCount++;
            }
            ownElements.add(e);
        } else {
            if(e.isField()) {
                prototypeAndStaticFieldCount++;
            }
            staticAndPrototypeElements.add(e);
        }
    }

    private void addElementPlacement(ElementDescriptor e, boolean isSilent, Node originatingNode) {
        //AddElementPlacement
        if(e.hasKey()) {
            Object key = e.getKey();
            if(e.isStatic()) {
                checkKey(staticKeys, key, isSilent, originatingNode);
            }
            else if(e.isOwn()) {
                checkKey(ownKeys, key, isSilent, originatingNode);
            } else {
                assert e.isPrototype();
                checkKey(prototypeKeys, key, isSilent, originatingNode);
            }
        }
    }

    private void checkKey(Set<Object> collection, Object key, boolean isSilent, Node originatingNode) {
        if(collection.contains(key)) {
            error(key, isSilent, originatingNode);
        } else {
            collection.add(key);
        }
    }

    private void error(Object key, boolean isSilent, Node originatingNode) {
        if(!isSilent) {
            throw Errors.createTypeError(String.format("Duplicate key %s.", key), originatingNode);
        }
    }

    public int getPrototypeAndStaticFieldCount() { return prototypeAndStaticFieldCount; }

    public int getOwnFieldCount() { return ownFieldCount; }

    public int getOwnHookStartCount() { return ownHookStartCount; }

    public List<ElementDescriptor> getOwnElements() {
        return ownElements;
    }

    public boolean setInstanceBand() {
        return methodOrAccessorWithPrivateKey;
    }

    public List<ElementDescriptor> getStaticAndPrototypeElements() {
        return staticAndPrototypeElements;
    }

    public ElementDescriptor[] getOwnElementsArray() {
        return ownElements.toArray(new ElementDescriptor[0]);
    }

    @TruffleBoundary
    public int size() {
        return ownElements.size() + staticAndPrototypeElements.size();
    }

    @TruffleBoundary
    public void removeStaticAndPrototypeElements() {
        staticAndPrototypeElements = null;
    }
}
