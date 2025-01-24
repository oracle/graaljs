// Copyright 2018 the V8 project authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

#ifndef V8_HEAP_HEAP_WRITE_BARRIER_INL_H_
#define V8_HEAP_HEAP_WRITE_BARRIER_INL_H_

// Clients of this interface shouldn't depend on lots of heap internals.
// Do not include anything from src/heap here!

#include "src/heap/heap-write-barrier.h"
#include "src/heap/marking-barrier.h"
#include "src/heap/memory-chunk.h"
#include "src/objects/compressed-slots-inl.h"
#include "src/objects/maybe-object-inl.h"

namespace v8 {
namespace internal {

// Defined in heap.cc.
V8_EXPORT_PRIVATE bool Heap_PageFlagsAreConsistent(Tagged<HeapObject> object);
V8_EXPORT_PRIVATE void Heap_CombinedGenerationalAndSharedBarrierSlow(
    Tagged<HeapObject> object, Address slot, Tagged<HeapObject> value);
V8_EXPORT_PRIVATE void Heap_CombinedGenerationalAndSharedEphemeronBarrierSlow(
    Tagged<EphemeronHashTable> table, Address slot, Tagged<HeapObject> value);

V8_EXPORT_PRIVATE void Heap_GenerationalBarrierForCodeSlow(
    Tagged<InstructionStream> host, RelocInfo* rinfo,
    Tagged<HeapObject> object);

V8_EXPORT_PRIVATE void Heap_GenerationalEphemeronKeyBarrierSlow(
    Heap* heap, Tagged<HeapObject> table, Address slot);

inline bool IsCodeSpaceObject(Tagged<HeapObject> object);
inline bool IsTrustedSpaceObject(Tagged<HeapObject> object);

// Do not use these internal details anywhere outside of this file. These
// internals are only intended to shortcut write barrier checks.
namespace heap_internals {

inline void CombinedWriteBarrierInternal(Tagged<HeapObject> host,
                                         HeapObjectSlot slot,
                                         Tagged<HeapObject> value,
                                         WriteBarrierMode mode) {
  DCHECK_EQ(mode, UPDATE_WRITE_BARRIER);

  MemoryChunk* host_chunk = MemoryChunk::FromHeapObject(host);

  MemoryChunk* value_chunk = MemoryChunk::FromHeapObject(value);

  const bool pointers_from_here_are_interesting =
      !host_chunk->IsYoungOrSharedChunk();
  const bool is_marking = host_chunk->IsMarking();

  if (pointers_from_here_are_interesting &&
      value_chunk->IsYoungOrSharedChunk()) {
    // Generational or shared heap write barrier (old-to-new or old-to-shared).
    Heap_CombinedGenerationalAndSharedBarrierSlow(host, slot.address(), value);
  }

  // Marking barrier: mark value & record slots when marking is on.
  if (V8_UNLIKELY(is_marking)) {
    // CodePageHeaderModificationScope is not required because the only case
    // when a InstructionStream value is stored somewhere is during creation of
    // a new InstructionStream object which is then stored to
    // Code's code field and in this case the code space is already unlocked.
    WriteBarrier::MarkingSlow(host, HeapObjectSlot(slot), value);
  }
}

}  // namespace heap_internals

inline void WriteBarrierForCode(Tagged<InstructionStream> host,
                                RelocInfo* rinfo, Tagged<Object> value,
                                WriteBarrierMode mode) {
  DCHECK(!HasWeakHeapObjectTag(value));
  if (!value.IsHeapObject()) return;
  WriteBarrierForCode(host, rinfo, HeapObject::cast(value), mode);
}

inline void WriteBarrierForCode(Tagged<InstructionStream> host,
                                RelocInfo* rinfo, Tagged<HeapObject> value,
                                WriteBarrierMode mode) {
  if (mode == SKIP_WRITE_BARRIER) {
    SLOW_DCHECK(!WriteBarrier::IsRequired(host, value));
    return;
  }

  // Used during InstructionStream initialization where we update the write
  // barriers together separate from the field writes.
  if (mode == UNSAFE_SKIP_WRITE_BARRIER) {
    DCHECK(!DisallowGarbageCollection::IsAllowed());
    return;
  }

  DCHECK_EQ(mode, UPDATE_WRITE_BARRIER);
  GenerationalBarrierForCode(host, rinfo, value);
  WriteBarrier::Shared(host, rinfo, value);
  WriteBarrier::Marking(host, rinfo, value);
}

inline void CombinedWriteBarrier(Tagged<HeapObject> host, ObjectSlot slot,
                                 Tagged<Object> value, WriteBarrierMode mode) {
  if (mode == SKIP_WRITE_BARRIER) {
    SLOW_DCHECK(!WriteBarrier::IsRequired(host, value));
    return;
  }

  if (!value.IsHeapObject()) return;
  heap_internals::CombinedWriteBarrierInternal(host, HeapObjectSlot(slot),
                                               HeapObject::cast(value), mode);
}

inline void CombinedWriteBarrier(Tagged<HeapObject> host, MaybeObjectSlot slot,
                                 Tagged<MaybeObject> value,
                                 WriteBarrierMode mode) {
  if (mode == SKIP_WRITE_BARRIER) {
    SLOW_DCHECK(!WriteBarrier::IsRequired(host, value));
    return;
  }

  Tagged<HeapObject> value_object;
  if (!value.GetHeapObject(&value_object)) return;
  heap_internals::CombinedWriteBarrierInternal(host, HeapObjectSlot(slot),
                                               value_object, mode);
}

inline void CombinedWriteBarrier(HeapObjectLayout* host,
                                 TaggedMemberBase* member, Tagged<Object> value,
                                 WriteBarrierMode mode) {
  if (mode == SKIP_WRITE_BARRIER) {
    SLOW_DCHECK(!WriteBarrier::IsRequired(host, value));
    return;
  }

  if (!value.IsHeapObject()) return;
  heap_internals::CombinedWriteBarrierInternal(
      Tagged(host), HeapObjectSlot(ObjectSlot(member)), HeapObject::cast(value),
      mode);
}

inline void CombinedEphemeronWriteBarrier(Tagged<EphemeronHashTable> host,
                                          ObjectSlot slot, Tagged<Object> value,
                                          WriteBarrierMode mode) {
  if (mode == SKIP_WRITE_BARRIER) {
    SLOW_DCHECK(!WriteBarrier::IsRequired(host, value));
    return;
  }

  DCHECK_EQ(mode, UPDATE_WRITE_BARRIER);
  if (!value.IsHeapObject()) return;

  MemoryChunk* host_chunk = MemoryChunk::FromHeapObject(host);

  Tagged<HeapObject> heap_object_value = HeapObject::cast(value);
  MemoryChunk* value_chunk = MemoryChunk::FromHeapObject(heap_object_value);

  const bool pointers_from_here_are_interesting =
      !host_chunk->IsYoungOrSharedChunk();
  const bool is_marking = host_chunk->IsMarking();

  if (pointers_from_here_are_interesting &&
      value_chunk->IsYoungOrSharedChunk()) {
    Heap_CombinedGenerationalAndSharedEphemeronBarrierSlow(host, slot.address(),
                                                           heap_object_value);
  }

  // Marking barrier: mark value & record slots when marking is on.
  if (is_marking) {
    // Currently InstructionStream values are never stored in EphemeronTables.
    // If this ever changes then the CodePageHeaderModificationScope might be
    // required here.
    DCHECK(!IsCodeSpaceObject(heap_object_value));
    WriteBarrier::MarkingSlow(host, HeapObjectSlot(slot), heap_object_value);
  }
}

inline void IndirectPointerWriteBarrier(Tagged<HeapObject> host,
                                        IndirectPointerSlot slot,
                                        Tagged<HeapObject> value,
                                        WriteBarrierMode mode) {
  // Indirect pointers are only used when the sandbox is enabled.
  DCHECK(V8_ENABLE_SANDBOX_BOOL);

  if (mode == SKIP_WRITE_BARRIER) {
    SLOW_DCHECK(!WriteBarrier::IsRequired(host, value));
    return;
  }

  // Objects referenced via indirect pointers are currently never allocated in
  // the young generation or the shared heap. If they ever are, then some of
  // these write barriers need to be adjusted.
  DCHECK(!MemoryChunk::FromHeapObject(value)->IsYoungOrSharedChunk());

  WriteBarrier::Marking(host, slot);
}

inline void ProtectedPointerWriteBarrier(Tagged<TrustedObject> host,
                                         ProtectedPointerSlot slot,
                                         Tagged<TrustedObject> value,
                                         WriteBarrierMode mode) {
  if (mode == SKIP_WRITE_BARRIER) {
    SLOW_DCHECK(!WriteBarrier::IsRequired(host, value));
    return;
  }

  // Protected pointers are only used within trusted space.
  DCHECK(!MemoryChunk::FromHeapObject(value)->IsYoungOrSharedChunk());

  WriteBarrier::Marking(host, slot, value);
}

inline void GenerationalBarrierForCode(Tagged<InstructionStream> host,
                                       RelocInfo* rinfo,
                                       Tagged<HeapObject> object) {
  if (V8_ENABLE_THIRD_PARTY_HEAP_BOOL) return;
  MemoryChunk* object_chunk = MemoryChunk::FromHeapObject(object);
  if (!object_chunk->InYoungGeneration()) return;
  Heap_GenerationalBarrierForCodeSlow(host, rinfo, object);
}

inline WriteBarrierMode GetWriteBarrierModeForObject(
    Tagged<HeapObject> object, const DisallowGarbageCollection* promise) {
  if (v8_flags.disable_write_barriers) return SKIP_WRITE_BARRIER;
  DCHECK(Heap_PageFlagsAreConsistent(object));
  MemoryChunk* chunk = MemoryChunk::FromHeapObject(object);
  if (chunk->IsMarking()) return UPDATE_WRITE_BARRIER;
  if (chunk->InYoungGeneration()) return SKIP_WRITE_BARRIER;
  return UPDATE_WRITE_BARRIER;
}

inline bool ObjectInYoungGeneration(Tagged<Object> object) {
  // TODO(rong): Fix caller of this function when we deploy
  // v8_use_third_party_heap.
  if (v8_flags.single_generation) return false;
  if (object.IsSmi()) return false;
  return MemoryChunk::FromHeapObject(HeapObject::cast(object))
      ->InYoungGeneration();
}

inline bool IsReadOnlyHeapObject(Tagged<HeapObject> object) {
  if (V8_ENABLE_THIRD_PARTY_HEAP_BOOL) return ReadOnlyHeap::Contains(object);
  MemoryChunk* chunk = MemoryChunk::FromHeapObject(object);
  return chunk->InReadOnlySpace();
}

inline bool IsCodeSpaceObject(Tagged<HeapObject> object) {
  MemoryChunk* chunk = MemoryChunk::FromHeapObject(object);
  return chunk->InCodeSpace();
}

inline bool IsTrustedSpaceObject(Tagged<HeapObject> object) {
  MemoryChunk* chunk = MemoryChunk::FromHeapObject(object);
  return chunk->InTrustedSpace();
}

bool WriteBarrier::IsMarking(Tagged<HeapObject> object) {
  if (V8_ENABLE_THIRD_PARTY_HEAP_BOOL) return false;
  MemoryChunk* chunk = MemoryChunk::FromHeapObject(object);
  return chunk->IsMarking();
}

void WriteBarrier::Marking(Tagged<HeapObject> host, ObjectSlot slot,
                           Tagged<Object> value) {
  DCHECK(!HasWeakHeapObjectTag(value));
  if (!value.IsHeapObject()) return;
  Tagged<HeapObject> value_heap_object = HeapObject::cast(value);
  // Currently this marking barrier is never used for InstructionStream values.
  // If this ever changes then the CodePageHeaderModificationScope might be
  // required here.
  DCHECK(!IsCodeSpaceObject(value_heap_object));
  Marking(host, HeapObjectSlot(slot), value_heap_object);
}

void WriteBarrier::Marking(Tagged<HeapObject> host, MaybeObjectSlot slot,
                           Tagged<MaybeObject> value) {
  Tagged<HeapObject> value_heap_object;
  if (!value.GetHeapObject(&value_heap_object)) return;
  // This barrier is called from generated code and from C++ code.
  // There must be no stores of InstructionStream values from generated code and
  // all stores of InstructionStream values in C++ must be handled by
  // CombinedWriteBarrierInternal().
  DCHECK(!IsCodeSpaceObject(value_heap_object));
  Marking(host, HeapObjectSlot(slot), value_heap_object);
}

void WriteBarrier::Marking(Tagged<HeapObject> host, HeapObjectSlot slot,
                           Tagged<HeapObject> value) {
  if (!IsMarking(host)) return;
  MarkingSlow(host, slot, value);
}

void WriteBarrier::Marking(Tagged<InstructionStream> host,
                           RelocInfo* reloc_info, Tagged<HeapObject> value) {
  if (!IsMarking(host)) return;
  MarkingSlow(host, reloc_info, value);
}

void WriteBarrier::Shared(Tagged<InstructionStream> host, RelocInfo* reloc_info,
                          Tagged<HeapObject> value) {
  if (V8_ENABLE_THIRD_PARTY_HEAP_BOOL) return;

  MemoryChunk* value_chunk = MemoryChunk::FromHeapObject(value);
  if (!value_chunk->InWritableSharedSpace()) return;

  SharedSlow(host, reloc_info, value);
}

void WriteBarrier::Marking(Tagged<JSArrayBuffer> host,
                           ArrayBufferExtension* extension) {
  if (!extension || !IsMarking(host)) return;
  MarkingSlow(host, extension);
}

void WriteBarrier::Marking(Tagged<DescriptorArray> descriptor_array,
                           int number_of_own_descriptors) {
  if (!IsMarking(descriptor_array)) return;
  MarkingSlow(descriptor_array, number_of_own_descriptors);
}

void WriteBarrier::Marking(Tagged<HeapObject> host, IndirectPointerSlot slot) {
  if (!IsMarking(host)) return;
  MarkingSlow(host, slot);
}

void WriteBarrier::Marking(Tagged<TrustedObject> host,
                           ProtectedPointerSlot slot,
                           Tagged<TrustedObject> value) {
  if (!IsMarking(host)) return;
  MarkingSlow(host, slot, value);
}

// static
void WriteBarrier::MarkingFromGlobalHandle(Tagged<Object> value) {
  if (V8_ENABLE_THIRD_PARTY_HEAP_BOOL) return;
  if (!value.IsHeapObject()) return;
  MarkingSlowFromGlobalHandle(HeapObject::cast(value));
}

// static
void WriteBarrier::CombinedBarrierFromInternalFields(Tagged<JSObject> host,
                                                     void* value) {
  CombinedBarrierFromInternalFields(host, 1, &value);
}

// static
void WriteBarrier::CombinedBarrierFromInternalFields(Tagged<JSObject> host,
                                                     size_t argc,
                                                     void** values) {
  if (V8_ENABLE_THIRD_PARTY_HEAP_BOOL) return;
  if (V8_LIKELY(!IsMarking(host))) {
    GenerationalBarrierFromInternalFields(host, argc, values);
    return;
  }
  MarkingBarrier* marking_barrier = CurrentMarkingBarrier(host);
  if (marking_barrier->is_minor()) {
    // TODO(v8:13012): We do not currently mark Oilpan objects while MinorMS is
    // active. Once Oilpan uses a generational GC with incremental marking and
    // unified heap, this barrier will be needed again.
    return;
  }
  MarkingSlowFromInternalFields(marking_barrier->heap(), host);
}

// static
void WriteBarrier::GenerationalBarrierFromInternalFields(Tagged<JSObject> host,
                                                         void* value) {
  GenerationalBarrierFromInternalFields(host, 1, &value);
}

// static
void WriteBarrier::GenerationalBarrierFromInternalFields(Tagged<JSObject> host,
                                                         size_t argc,
                                                         void** values) {
  auto* memory_chunk = MemoryChunk::FromHeapObject(host);
  if (V8_LIKELY(memory_chunk->InYoungGeneration())) return;
  auto* cpp_heap = memory_chunk->GetHeap()->cpp_heap();
  if (!cpp_heap) return;
  for (size_t i = 0; i < argc; ++i) {
    if (!values[i]) continue;
    v8::internal::CppHeap::From(cpp_heap)->RememberCrossHeapReferenceIfNeeded(
        host, values[i]);
  }
}

#ifdef ENABLE_SLOW_DCHECKS
// static
template <typename T>
bool WriteBarrier::IsRequired(Tagged<HeapObject> host, T value) {
  if (MemoryChunk::FromHeapObject(host)->InYoungGeneration()) return false;
  if (IsSmi(value)) return false;
  if (value.IsCleared()) return false;
  Tagged<HeapObject> target = value.GetHeapObject();
  if (ReadOnlyHeap::Contains(target)) return false;
  return !IsImmortalImmovableHeapObject(target);
}
// static
template <typename T>
bool WriteBarrier::IsRequired(const HeapObjectLayout* host, T value) {
  if (MemoryChunk::FromAddress(host->address())->InYoungGeneration())
    return false;
  if (IsSmi(value)) return false;
  if (value.IsCleared()) return false;
  Tagged<HeapObject> target = value.GetHeapObject();
  if (ReadOnlyHeap::Contains(target)) return false;
  return !IsImmortalImmovableHeapObject(target);
}
#endif

}  // namespace internal
}  // namespace v8

#endif  // V8_HEAP_HEAP_WRITE_BARRIER_INL_H_
