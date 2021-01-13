package com.oracle.truffle.js.nodes.decorators;

import java.util.LinkedList;

public class ClassElementList {
    private LinkedList<ElementDescriptor> elements = new LinkedList<>();
    private int prototypeFieldCount = 0;
    private int staticFieldCount = 0;

    public void push(ElementDescriptor e) {
        if(e.isField()) {
            if(e.isStatic()) {
                staticFieldCount++;
            }
            if(e.isPrototype()){
                prototypeFieldCount++;
            }
        }
        elements.addLast(e);
    }

    public ElementDescriptor peek() {
        return elements.getFirst();
    }

    public ElementDescriptor pop() {
        ElementDescriptor e = elements.removeFirst();
        if(e.isField()) {
            if(e.isStatic()) {
                staticFieldCount--;
            }
            if(e.isPrototype()){
                prototypeFieldCount--;
            }
        }
        return e;
    }

    public LinkedList<ElementDescriptor> getList() {
        return elements;
    }

    public int getPrototypeFieldCount() { return prototypeFieldCount; }

    public int getStaticFieldCount() { return staticFieldCount; }

    public int size() { return elements.size(); };

    public ElementDescriptor[] toArray() {
        return elements.toArray(new ElementDescriptor[elements.size()]);
    }
}
