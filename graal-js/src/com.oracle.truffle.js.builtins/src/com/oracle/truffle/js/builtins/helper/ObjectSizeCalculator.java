/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The Universal Permissive License (UPL), Version 1.0
 *
 * Subject to the condition set forth below, permission is hereby granted to any
 * person obtaining a copy of this software, associated documentation and/or
 * data (collectively the "Software"), free of charge and under any and all
 * copyright rights in the Software, and any and all patent rights owned or
 * freely licensable by each licensor hereunder covering either (i) the
 * unmodified Software as contributed to or provided by such licensor, or (ii)
 * the Larger Works (as defined below), to deal in both
 *
 * (a) the Software, and
 *
 * (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 * one is included with the Software each a "Larger Work" to which the Software
 * is contributed by such licensors),
 *
 * without restriction, including without limitation the rights to copy, create
 * derivative works of, display, perform, and distribute the Software and make,
 * use, sell, offer for sale, import, export, have made, and have sold the
 * Software and the Larger Work(s), and to sublicense the foregoing rights on
 * either these or other terms.
 *
 * This license is subject to the following condition:
 *
 * The above copyright notice and either this complete permission notice or at a
 * minimum a reference to the UPL must be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.oracle.truffle.js.builtins.helper;

import java.lang.reflect.*;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import sun.misc.*;

/**
 * Original implementation from Nashorn.
 *
 * Contains utility methods for calculating the memory usage of objects. It only works on the
 * HotSpot JVM, and infers the actual memory layout (32 bit vs. 64 bit word size, compressed object
 * pointers vs. uncompressed) from best available indicators. It can reliably detect a 32 bit vs. 64
 * bit JVM. It can only make an educated guess at whether compressed OOPs are used, though;
 * specifically, it knows what the JVM's default choice of OOP compression would be based on HotSpot
 * version and maximum heap sizes, but if the choice is explicitly overridden with the
 * <tt>-XX:{+|-}UseCompressedOops</tt> command line switch, it can not detect this fact and will
 * report incorrect sizes, as it will presume the default JVM behavior.
 */
public class ObjectSizeCalculator {

    /**
     * Describes constant memory overheads for various constructs in a JVM implementation.
     */
    public interface MemoryLayoutSpecification {

        /**
         * Returns the fixed overhead of an array of any type or length in this JVM.
         *
         * @return the fixed overhead of an array.
         */
        int getArrayHeaderSize();

        /**
         * Returns the fixed overhead of for any {@link Object} subclass in this JVM.
         *
         * @return the fixed overhead of any object.
         */
        int getObjectHeaderSize();

        /**
         * Returns the quantum field size for a field owned by an object in this JVM.
         *
         * @return the quantum field size for an object.
         */
        int getObjectPadding();

        /**
         * Returns the fixed size of an object reference in this JVM.
         *
         * @return the size of all object references.
         */
        int getReferenceSize();

        /**
         * Returns the quantum field size for a field owned by one of an object's ancestor
         * superclasses in this JVM.
         *
         * @return the quantum field size for a superclass field.
         */
        int getSuperclassFieldPadding();
    }

    public static class CurrentLayout {
        public static final MemoryLayoutSpecification SPEC = getEffectiveMemoryLayoutSpecification();
    }

    /**
     * Given an object, returns the total allocated size, in bytes, of the object and all other
     * objects reachable from it. Attempts to to detect the current JVM memory layout, but may fail
     * with {@link UnsupportedOperationException};
     *
     * @param obj the object; can be null. Passing in a {@link java.lang.Class} object doesn't do
     *            anything special, it measures the size of all objects reachable through it (which
     *            will include its class loader, and by extension, all other Class objects loaded by
     *            the same loader, and all the parent class loaders). It doesn't provide the size of
     *            the static fields in the JVM class that the Class object represents.
     * @return the total allocated size of the object and all other objects it retains.
     * @throws UnsupportedOperationException if the current vm memory layout cannot be detected.
     */
    public static long getObjectSize(final Object obj) throws UnsupportedOperationException {
        return obj == null ? 0 : new ObjectSizeCalculator(CurrentLayout.SPEC).calculateObjectSize(obj);
    }

    // Fixed object header size for arrays.
    private final int arrayHeaderSize;
    // Fixed object header size for non-array objects.
    private final int objectHeaderSize;
    // Padding for the object size - if the object size is not an exact multiple
    // of this, it is padded to the next multiple.
    private final int objectPadding;
    // Size of reference (pointer) fields.
    private final int referenceSize;

    private final Map<Class<?>, ClassSizeInfo> classSizeInfos = new IdentityHashMap<>();

    private final Map<Object, Object> alreadyVisited = new IdentityHashMap<>();
    private final Map<Class<?>, ClassHistogramElement> histogram = new IdentityHashMap<>();

    private final Deque<Object> pending = new ArrayDeque<>(16 * 1024);
    private long size;

    private final Predicate<Object> predicate;

    /**
     * Creates an object size calculator that can calculate object sizes for a given
     * {@code memoryLayoutSpecification}.
     *
     * @param memoryLayoutSpecification a description of the JVM memory layout.
     */
    public ObjectSizeCalculator(final MemoryLayoutSpecification memoryLayoutSpecification) {
        this(memoryLayoutSpecification, all -> true);
    }

    public ObjectSizeCalculator(final MemoryLayoutSpecification memoryLayoutSpecification, Predicate<Object> predicate) {
        this.arrayHeaderSize = memoryLayoutSpecification.getArrayHeaderSize();
        this.objectHeaderSize = memoryLayoutSpecification.getObjectHeaderSize();
        this.objectPadding = memoryLayoutSpecification.getObjectPadding();
        this.referenceSize = memoryLayoutSpecification.getReferenceSize();
        this.predicate = predicate;
    }

    /**
     * Given an object, returns the total allocated size, in bytes, of the object and all other
     * objects reachable from it.
     *
     * @param obj the object; can be null. Passing in a {@link java.lang.Class} object doesn't do
     *            anything special, it measures the size of all objects reachable through it (which
     *            will include its class loader, and by extension, all other Class objects loaded by
     *            the same loader, and all the parent class loaders). It doesn't provide the size of
     *            the static fields in the JVM class that the Class object represents.
     * @return the total allocated size of the object and all other objects it retains.
     */
    public synchronized long calculateObjectSize(final Object obj) {
        // Breadth-first traversal instead of naive depth-first with recursive
        // implementation, so we don't blow the stack traversing long linked lists.
        histogram.clear();
        try {
            Object o = obj;
            for (;;) {
                visit(o);
                if (pending.isEmpty()) {
                    return size;
                }
                o = pending.removeFirst();
            }
        } finally {
            alreadyVisited.clear();
            pending.clear();
            size = 0;
        }
    }

    /**
     * Get the class histogram.
     *
     * @return class histogram element list
     */
    public List<ClassHistogramElement> getClassHistogram() {
        return new ArrayList<>(histogram.values());
    }

    private ClassSizeInfo getClassSizeInfo(final Class<?> clazz) {
        ClassSizeInfo csi = classSizeInfos.get(clazz);
        if (csi == null) {
            csi = new ClassSizeInfo(clazz);
            classSizeInfos.put(clazz, csi);
        }
        return csi;
    }

    private void visit(final Object obj) {
        if (alreadyVisited.containsKey(obj)) {
            return;
        }
        final Class<?> clazz = obj.getClass();
        if (clazz == ArrayElementsVisitor.class) {
            ((ArrayElementsVisitor) obj).visit(this);
        } else {
            alreadyVisited.put(obj, obj);
            if (clazz.isArray()) {
                visitArray(obj);
            } else {
                getClassSizeInfo(clazz).visit(obj, this);
            }
        }
    }

    private void visitArray(final Object array) {
        final Class<?> arrayClass = array.getClass();
        final Class<?> componentType = arrayClass.getComponentType();
        final int length = Array.getLength(array);
        if (componentType.isPrimitive()) {
            increaseByArraySize(arrayClass, length, getPrimitiveFieldSize(componentType));
        } else {
            increaseByArraySize(arrayClass, length, referenceSize);
            // If we didn't use an ArrayElementsVisitor, we would be enqueueing every
            // element of the array here instead. For large arrays, it would
            // tremendously enlarge the queue. In essence, we're compressing it into
            // a small command object instead. This is different than immediately
            // visiting the elements, as their visiting is scheduled for the end of
            // the current queue.
            switch (length) {
                case 0: {
                    break;
                }
                case 1: {
                    enqueue(Array.get(array, 0));
                    break;
                }
                default: {
                    enqueue(new ArrayElementsVisitor((Object[]) array));
                }
            }
        }
    }

    private void increaseByArraySize(final Class<?> clazz, final int length, final long elementSize) {
        increaseSize(clazz, roundTo(arrayHeaderSize + length * elementSize, objectPadding));
    }

    private static class ArrayElementsVisitor {
        private final Object[] array;

        ArrayElementsVisitor(final Object[] array) {
            this.array = array;
        }

        public void visit(final ObjectSizeCalculator calc) {
            for (final Object elem : array) {
                if (elem != null) {
                    calc.visit(elem);
                }
            }
        }
    }

    void enqueue(final Object obj) {
        if (obj != null && predicate.test(obj)) {
            pending.addLast(obj);
        }
    }

    void increaseSize(final Class<?> clazz, final long objectSize) {
        ClassHistogramElement he = histogram.get(clazz);
        if (he == null) {
            he = new ClassHistogramElement(clazz);
            histogram.put(clazz, he);
        }
        he.addInstance(objectSize);
        size += objectSize;
    }

    static long roundTo(final long x, final int multiple) {
        return ((x + multiple - 1) / multiple) * multiple;
    }

    private class ClassSizeInfo {
        // Padded fields + header size
        private final long objectSize;
        private final Field[] referenceFields;

        ClassSizeInfo(final Class<?> clazz) {
            final List<Field> newReferenceFields = new LinkedList<>();
            Stream<Field> fields = Stream.empty();
            for (Class<?> c = clazz; c != null; c = c.getSuperclass()) {
                fields = Stream.concat(fields, Arrays.stream(c.getDeclaredFields()));
            }
            long usedSize = fields.filter(f -> !Modifier.isStatic(f.getModifiers())).mapToLong(f -> {
                if (!f.getType().isPrimitive()) {
                    f.setAccessible(true);
                    newReferenceFields.add(f);
                }
                return UNSAFE.objectFieldOffset(f) + (f.getType().isPrimitive() ? getPrimitiveFieldSize(f.getType()) : referenceSize);
            }).max().orElse(objectHeaderSize);

            this.objectSize = roundTo(usedSize, objectPadding);
            this.referenceFields = newReferenceFields.toArray(new Field[newReferenceFields.size()]);
        }

        void visit(final Object obj, final ObjectSizeCalculator calc) {
            calc.increaseSize(obj.getClass(), objectSize);
            enqueueReferencedObjects(obj, calc);
        }

        public void enqueueReferencedObjects(final Object obj, final ObjectSizeCalculator calc) {
            for (Field f : referenceFields) {
                try {
                    calc.enqueue(f.get(obj));
                } catch (IllegalAccessException e) {
                    final AssertionError ae = new AssertionError("Unexpected denial of access to " + f);
                    ae.initCause(e);
                    throw ae;
                }
            }
        }
    }

    private static long getPrimitiveFieldSize(final Class<?> type) {
        if (type == boolean.class || type == byte.class) {
            return 1;
        }
        if (type == char.class || type == short.class) {
            return 2;
        }
        if (type == int.class || type == float.class) {
            return 4;
        }
        if (type == long.class || type == double.class) {
            return 8;
        }
        throw new AssertionError("Encountered unexpected primitive type " + type.getName());
    }

    // ALERT: java.lang.management is not available in compact 1. We need
    // to use reflection to soft link test memory statistics.

    static Class<?> managementFactory = null;
    static Class<?> memoryPoolMXBean = null;
    static Class<?> memoryUsage = null;
    static Method getMemoryPoolMXBeans = null;
    static Method getUsage = null;
    static Method getMax = null;
    static {
        try {
            managementFactory = Class.forName("java.lang.management.ManagementFactory");
            memoryPoolMXBean = Class.forName("java.lang.management.MemoryPoolMXBean");
            memoryUsage = Class.forName("java.lang.management.MemoryUsage");

            getMemoryPoolMXBeans = managementFactory.getMethod("getMemoryPoolMXBeans");
            getUsage = memoryPoolMXBean.getMethod("getUsage");
            getMax = memoryUsage.getMethod("getMax");
        } catch (ClassNotFoundException | NoSuchMethodException | SecurityException ex) {
            // Pass thru, asserts when attempting to use.
        }
    }

    /**
     * Return the current memory usage.
     *
     * @return current memory usage derived from system configuration
     */
    public static MemoryLayoutSpecification getEffectiveMemoryLayoutSpecification() {
        final String dataModel = System.getProperty("sun.arch.data.model");
        if ("32".equals(dataModel)) {
            // Running with 32-bit data model
            return new MemoryLayoutSpecification() {
                @Override
                public int getArrayHeaderSize() {
                    return 12;
                }

                @Override
                public int getObjectHeaderSize() {
                    return 8;
                }

                @Override
                public int getObjectPadding() {
                    return 8;
                }

                @Override
                public int getReferenceSize() {
                    return 4;
                }

                @Override
                public int getSuperclassFieldPadding() {
                    return 4;
                }
            };
        } else if (!"64".equals(dataModel)) {
            throw new UnsupportedOperationException("Unrecognized value '" + dataModel + "' of sun.arch.data.model system property");
        }

        final String strVmVersion = System.getProperty("java.vm.version");
        final int vmVersion = Integer.parseInt(strVmVersion.substring(0, strVmVersion.indexOf('.')));
        if (vmVersion >= 17) {
            long maxMemory = 0;

            /*
             * See ALERT above. The reflection code below duplicates the following sequence, and
             * avoids hard coding of java.lang.management.
             * 
             * for (MemoryPoolMXBean mp : ManagementFactory.getMemoryPoolMXBeans()) { maxMemory +=
             * mp.getUsage().getMax(); }
             */

            if (getMemoryPoolMXBeans == null) {
                throw new AssertionError("java.lang.management not available in compact 1");
            }

            try {
                final List<?> memoryPoolMXBeans = (List<?>) getMemoryPoolMXBeans.invoke(managementFactory);
                for (final Object mp : memoryPoolMXBeans) {
                    final Object usage = getUsage.invoke(mp);
                    final Object max = getMax.invoke(usage);
                    maxMemory += ((Long) max).longValue();
                }
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                throw new AssertionError("java.lang.management not available in compact 1");
            }

            if (maxMemory < 30L * 1024 * 1024 * 1024) {
                // HotSpot 17.0 and above use compressed OOPs below 30GB of RAM total
                // for all memory pools (yes, including code cache).
                return new MemoryLayoutSpecification() {
                    @Override
                    public int getArrayHeaderSize() {
                        return 16;
                    }

                    @Override
                    public int getObjectHeaderSize() {
                        return 12;
                    }

                    @Override
                    public int getObjectPadding() {
                        return 8;
                    }

                    @Override
                    public int getReferenceSize() {
                        return 4;
                    }

                    @Override
                    public int getSuperclassFieldPadding() {
                        return 4;
                    }
                };
            }
        }

        // In other cases, it's a 64-bit uncompressed OOPs object model
        return new MemoryLayoutSpecification() {
            @Override
            public int getArrayHeaderSize() {
                return 24;
            }

            @Override
            public int getObjectHeaderSize() {
                return 16;
            }

            @Override
            public int getObjectPadding() {
                return 8;
            }

            @Override
            public int getReferenceSize() {
                return 8;
            }

            @Override
            public int getSuperclassFieldPadding() {
                return 8;
            }
        };
    }

    private static final Unsafe UNSAFE = getUnsafe();

    private static Unsafe getUnsafe() {
        try {
            return Unsafe.getUnsafe();
        } catch (SecurityException e) {
        }
        try {
            Field theUnsafeInstance = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafeInstance.setAccessible(true);
            return (Unsafe) theUnsafeInstance.get(Unsafe.class);
        } catch (Exception e) {
            throw new RuntimeException("exception while trying to get Unsafe.theUnsafe via reflection:", e);
        }
    }
}
