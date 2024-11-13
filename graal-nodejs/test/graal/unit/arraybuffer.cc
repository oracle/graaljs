/*
 * Copyright (c) 2020, 2024, Oracle and/or its affiliates. All rights reserved.
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

#define SUITE ArrayBuffer

// ArrayBuffer::Detach

EXPORT_TO_JS(Detach) {
    args[0].As<ArrayBuffer>()->Detach();
}

// ArrayBuffer::GetBackingStore

EXPORT_TO_JS(GetBackingStoreDataPointerIsNull) {
    void* data = args[0].As<ArrayBuffer>()->GetBackingStore()->Data();
    args.GetReturnValue().Set(data == nullptr);
}

EXPORT_TO_JS(GetBackingStoreSum) {
    std::shared_ptr<BackingStore> backing_store = args[0].As<ArrayBuffer>()->GetBackingStore();
    size_t length = backing_store->ByteLength();
    uint8_t* data = reinterpret_cast<uint8_t*> (backing_store->Data());
    int32_t sum = 0;
    for (size_t i = 0; i<length; i++) {
        sum += data[i];
    }
    args.GetReturnValue().Set(sum);
}

#define ArrayBufferViewNewTest(view_class, bytes_per_element) \
EXPORT_TO_JS(New ## view_class) { \
    Local<ArrayBuffer> buffer = args[0].As<ArrayBuffer>(); \
    size_t byte_length = buffer->ByteLength(); \
    Local<view_class> array = view_class::New(buffer, 0, byte_length/bytes_per_element); \
    args.GetReturnValue().Set(array); \
}

ArrayBufferViewNewTest(Uint8Array, 1)
ArrayBufferViewNewTest(Uint8ClampedArray, 1)
ArrayBufferViewNewTest(Int8Array, 1)
ArrayBufferViewNewTest(Uint16Array, 2)
ArrayBufferViewNewTest(Int16Array, 2)
ArrayBufferViewNewTest(Uint32Array, 4)
ArrayBufferViewNewTest(Int32Array, 4)
ArrayBufferViewNewTest(Float32Array, 4)
ArrayBufferViewNewTest(Float64Array, 8)
ArrayBufferViewNewTest(BigInt64Array, 8)
ArrayBufferViewNewTest(BigUint64Array, 8)
ArrayBufferViewNewTest(DataView, 1)

// Extracted from a test of sodium npm package
EXPORT_TO_JS(NewBackingStoreSodium) {
    Isolate* isolate = args.GetIsolate();
    // Check that we do not crash
    ArrayBuffer::NewBackingStore(isolate, 4294967173u); // -123 interpreted as unsigned
}

static char* foo = "foo";

EXPORT_TO_JS(NewBackingStoreEmptyDeleter) {
    std::unique_ptr<BackingStore> backing_store = ArrayBuffer::NewBackingStore(foo, 3, BackingStore::EmptyDeleter, nullptr);
    Local<ArrayBuffer> array_buffer = ArrayBuffer::New(args.GetIsolate(), std::move(backing_store));
    args.GetReturnValue().Set(array_buffer);
}

EXPORT_TO_JS(ViewByteOffset) {
    size_t result = args[0].As<ArrayBufferView>()->ByteOffset();
    args.GetReturnValue().Set((uint32_t)result);
}

EXPORT_TO_JS(ViewByteLength) {
    size_t result = args[0].As<ArrayBufferView>()->ByteLength();
    args.GetReturnValue().Set((uint32_t)result);
}

EXPORT_TO_JS(TypedArrayLength) {
    size_t result = args[0].As<TypedArray>()->Length();
    args.GetReturnValue().Set((uint32_t)result);
}

#undef SUITE
