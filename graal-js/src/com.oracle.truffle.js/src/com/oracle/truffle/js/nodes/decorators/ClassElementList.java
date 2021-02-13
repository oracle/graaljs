package com.oracle.truffle.js.nodes.decorators;

import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.js.runtime.Errors;

import java.util.HashMap;
import java.util.LinkedList;

public class ClassElementList {
    private LinkedList<ElementDescriptor> elements = new LinkedList<>();
    private int prototypeFieldCount = 0;
    private int staticFieldCount = 0;
    private int ownFieldCount = 0;
    private int ownHookStartCount = 0;

    private HashMap<Object, Integer> placementMap = new HashMap<>();

    public void enqueue(ElementDescriptor e) {
        enqueue(e, true, null);
    }

    public void enqueue(ElementDescriptor e, boolean isSilent, Node originatingNode) {
        if(e.isField()) {
            if(e.isStatic()) {
                staticFieldCount++;
            }
            if(e.isPrototype()){
                prototypeFieldCount++;
            }
            if(e.isOwn()) {
                ownFieldCount++;
            }
        }
        if(e.isHook() && e.isOwn() && e.hasStart()) {
            ownHookStartCount++;
        }
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
        elements.addLast(e);
    }

    public ElementDescriptor dequeue() {
        ElementDescriptor e = elements.removeFirst();
        if(e.isField()) {
            if(e.isStatic()) {
                staticFieldCount--;
            }
            if(e.isPrototype()){
                prototypeFieldCount--;
            }
            if(e.isOwn()) {
                ownFieldCount--;
            }
        }
        if(e.isHook() && e.isOwn() && e.hasStart()) {
            ownHookStartCount--;
        }
        //RemoveElementPlacement
        placementMap.remove(e.getKey());
        return e;
    }

    public LinkedList<ElementDescriptor> getList() {
        return elements;
    }

    public int getPrototypeFieldCount() { return prototypeFieldCount; }

    public int getStaticFieldCount() { return staticFieldCount; }

    public int getOwnFieldCount() { return ownFieldCount; }

    public int getOwnHookStartCount() { return ownHookStartCount; }

    public int size() { return elements.size(); }

    public ElementDescriptor[] toArray() {
        return elements.toArray(new ElementDescriptor[0]);
    }
}
