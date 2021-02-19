package com.oracle.truffle.js.nodes.decorators;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.js.runtime.Boundaries;
import com.oracle.truffle.js.runtime.Errors;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ClassElementList {
    private final List<ElementDescriptor> ownElements;
    private List<ElementDescriptor> staticAndPrototypeElements;
    private int prototypeAndStaticFieldCount = 0;
    private int ownFieldCount = 0;
    private int ownHookStartCount = 0;
    private boolean methodOrAccessorWithPrivateKey = false;

    private final Map<Object, Integer> placementMap;

    private ClassElementList() {
        ownElements = new ArrayList<>();
        staticAndPrototypeElements = new ArrayList<>();
        placementMap = Boundaries.hashMapCreate();
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
            if(placementMap.containsKey(e.getKey())) {
                int placement = placementMap.get(e.getKey());
                if(e.getPlacement() == placement && !isSilent) {
                    throw Errors.createTypeError(String.format("Duplicate key %s.", e.getKey()), originatingNode);
                }
            } else {
                placementMap.put(e.getKey(), e.getPlacement());
            }
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
