package com.oracle.truffle.trufflenode;

import com.oracle.truffle.js.runtime.JSTruffleOptions;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * A class responsible for the deallocation of the external memory segments associated with Java
 * objects.A direct {@code ByteBuffer} created by a JNI call is built on top of an existing memory
 * segment whose life-cycle may be associated with the created buffer.
 *
 * @author Jan Stola
 */
final class Deallocator {

    /**
     * Determines whether {@code sun.misc.Cleaner} can be used for deallocation.
     */
    private static final boolean USE_CLEANER = Boolean.parseBoolean(System.getProperty("truffle.node.js.deallocateUsingCleaner", "true"));
    /**
     * {@code java.nio.DirectByteBuffer} {@code Class} object.
     */
    private static final Class<?> DIRECT_BYTE_BUFFER_CLASS;
    /**
     * {@code cleaner} field of {@code DirectByteBuffer}.
     */
    private static final Field CLEANER_FIELD;

    static {
        Class<?> clazz;
        Field field;
        if (!JSTruffleOptions.SubstrateVM && USE_CLEANER) {
            try {
                clazz = Class.forName("java.nio.DirectByteBuffer");
                field = clazz.getDeclaredField("cleaner");
                field.setAccessible(true);
            } catch (ClassNotFoundException | NoSuchFieldException ex) {
                clazz = null;
                field = null;
            }
        } else {
            clazz = null;
            field = null;
        }
        DIRECT_BYTE_BUFFER_CLASS = clazz;
        CLEANER_FIELD = field;
    }

    /**
     * Queue associated with the {@code WeakReference}s that we use to determine whether the native
     * memory of the corresponding {@code ByteBuffer} should be deallocated.
     */
    private final ReferenceQueue<ByteBuffer> queue;
    /**
     * Collection holding all the weak references to the buffers whose memory has not been
     * deallocated yet.
     */
    private final Set<ReferenceWithPointer> enqueued = Collections.synchronizedSet(new HashSet<>());
    /**
     * Determines whether the cleanup thread has been started.
     */
    private boolean cleanupThreadStarted = false;

    Deallocator() {
        this.queue = new ReferenceQueue<>();
    }

    /**
     * Registers the given {@code buffer} for deallocation.
     *
     * @param buffer buffer whose memory should be deallocated once it is no longer used.
     * @param pointer pointer to the memory that should be deallocated.
     */
    void register(ByteBuffer buffer, long pointer) {
        if (buffer.getClass() == DIRECT_BYTE_BUFFER_CLASS) {
            try {
                CLEANER_FIELD.set(buffer, sun.misc.Cleaner.create(buffer, () -> {
                    NativeAccess.deallocate(pointer);
                }));
            } catch (IllegalAccessException | IllegalArgumentException ex) {
                ex.printStackTrace();
            }
        } else {
            if (!cleanupThreadStarted) {
                startCleanupThread();
            }
            enqueued.add(new ReferenceWithPointer(buffer, pointer));
        }
    }

    /**
     * Starts the cleanup thread (if it is not running already).
     */
    private synchronized void startCleanupThread() {
        if (cleanupThreadStarted) {
            return;
        }
        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    while (true) {
                        ReferenceWithPointer reference = (ReferenceWithPointer) queue.remove();
                        enqueued.remove(reference);
                        reference.deallocate();
                    }
                } catch (InterruptedException iex) {
                    iex.printStackTrace();
                }
            }
        };
        thread.setDaemon(true);
        thread.start();
        cleanupThreadStarted = true;
    }

    /**
     * Weak reference to {@code ByteBuffer} that keeps track of the memory that should be
     * deallocated once this reference is cleared.
     */
    final class ReferenceWithPointer extends WeakReference<ByteBuffer> {

        /**
         * Pointer to the memory that should be deallocated.
         */
        private final long pointer;

        ReferenceWithPointer(ByteBuffer object, long pointer) {
            super(object, queue);
            this.pointer = pointer;
        }

        void deallocate() {
            NativeAccess.deallocate(pointer);
        }
    }

}
