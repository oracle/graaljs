package com.oracle.truffle.js.nodes.decorators;

import java.util.LinkedList;

public class ClassElementList {
    private LinkedList<ElementDescriptor> elements = new LinkedList<>();
    private int instanceFieldCount = 0;
    private int staticFieldCount = 0;

    public void push(ElementDescriptor e) {
        if(e.isField()) {
            if(e.isStatic()) {
                staticFieldCount++;
            }
            if(e.isPrototype()) {
                instanceFieldCount++;
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
            if(e.isPrototype()) {
                instanceFieldCount--;
            }
        }
        return e;
    }

    public LinkedList<ElementDescriptor> getList() {
        return elements;
    }

    public int getInstanceFieldCount() { return instanceFieldCount; }

    public int getStaticFieldCount() { return staticFieldCount; }

    public int size() { return elements.size(); };
}
