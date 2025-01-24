// Copyright 2019 the V8 project authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

#ifndef V8_HEAP_MARKING_VISITOR_H_
#define V8_HEAP_MARKING_VISITOR_H_

#include "src/common/globals.h"
#include "src/execution/isolate.h"
#include "src/heap/ephemeron-remembered-set.h"
#include "src/heap/marking-state.h"
#include "src/heap/marking-worklist.h"
#include "src/heap/objects-visiting.h"
#include "src/heap/pretenuring-handler.h"
#include "src/heap/spaces.h"
#include "src/heap/weak-object-worklists.h"
#include "src/objects/string.h"

namespace v8 {
namespace internal {

struct EphemeronMarking {
  std::vector<Tagged<HeapObject>> newly_discovered;
  bool newly_discovered_overflowed;
  size_t newly_discovered_limit;
};

// The base class for all marking visitors (main and concurrent marking) but
// also for e.g. the reference summarizer. It implements marking logic with
// support for bytecode flushing, embedder tracing and weak references.
//
// Derived classes are expected to provide the following methods:
// - CanUpdateValuesInHeap
// - AddStrongReferenceForReferenceSummarizer
// - AddWeakReferenceForReferenceSummarizer
// - TryMark
// - IsMarked
// - MarkPointerTableEntry
// - retaining_path_mode
// - RecordSlot
// - RecordRelocSlot
//
// These methods capture the difference between the different visitor
// implementations. For example, the concurrent visitor has to use the locking
// for string types that can be transitioned to other types on the main thread
// concurrently. On the other hand, the reference summarizer is not supposed to
// write into heap objects.
template <typename ConcreteVisitor>
class MarkingVisitorBase : public ConcurrentHeapVisitor<int, ConcreteVisitor> {
 public:
  MarkingVisitorBase(MarkingWorklists::Local* local_marking_worklists,
                     WeakObjects::Local* local_weak_objects, Heap* heap,
                     unsigned mark_compact_epoch,
                     base::EnumSet<CodeFlushMode> code_flush_mode,
                     bool trace_embedder_fields,
                     bool should_keep_ages_unchanged,
                     uint16_t code_flushing_increase)
      : ConcurrentHeapVisitor<int, ConcreteVisitor>(heap->isolate()),
        local_marking_worklists_(local_marking_worklists),
        local_weak_objects_(local_weak_objects),
        heap_(heap),
        mark_compact_epoch_(mark_compact_epoch),
        code_flush_mode_(code_flush_mode),
        trace_embedder_fields_(trace_embedder_fields),
        should_keep_ages_unchanged_(should_keep_ages_unchanged),
        should_mark_shared_heap_(heap->isolate()->is_shared_space_isolate()),
        code_flushing_increase_(code_flushing_increase),
        isolate_in_background_(heap->isolate()->IsIsolateInBackground())
#ifdef V8_ENABLE_SANDBOX
        ,
        external_pointer_table_(&heap->isolate()->external_pointer_table()),
        shared_external_pointer_table_(
            &heap->isolate()->shared_external_pointer_table()),
        shared_external_pointer_space_(
            heap->isolate()->shared_external_pointer_space()),
        trusted_pointer_table_(&heap->isolate()->trusted_pointer_table())
#endif  // V8_ENABLE_SANDBOX
  {
  }

  V8_INLINE int VisitBytecodeArray(Tagged<Map> map,
                                   Tagged<BytecodeArray> object);
  V8_INLINE int VisitDescriptorArrayStrongly(Tagged<Map> map,
                                             Tagged<DescriptorArray> object);
  V8_INLINE int VisitDescriptorArray(Tagged<Map> map,
                                     Tagged<DescriptorArray> object);
  V8_INLINE int VisitEphemeronHashTable(Tagged<Map> map,
                                        Tagged<EphemeronHashTable> object);
  V8_INLINE int VisitFixedArray(Tagged<Map> map, Tagged<FixedArray> object);
  V8_INLINE int VisitJSApiObject(Tagged<Map> map, Tagged<JSObject> object);
  V8_INLINE int VisitJSArrayBuffer(Tagged<Map> map,
                                   Tagged<JSArrayBuffer> object);
  V8_INLINE int VisitJSDataViewOrRabGsabDataView(
      Tagged<Map> map, Tagged<JSDataViewOrRabGsabDataView> object);
  V8_INLINE int VisitJSFunction(Tagged<Map> map, Tagged<JSFunction> object);
  V8_INLINE int VisitJSTypedArray(Tagged<Map> map, Tagged<JSTypedArray> object);
  V8_INLINE int VisitJSWeakRef(Tagged<Map> map, Tagged<JSWeakRef> object);
  V8_INLINE int VisitMap(Tagged<Map> map, Tagged<Map> object);
  V8_INLINE int VisitSharedFunctionInfo(Tagged<Map> map,
                                        Tagged<SharedFunctionInfo> object);
  V8_INLINE int VisitTransitionArray(Tagged<Map> map,
                                     Tagged<TransitionArray> object);
  V8_INLINE int VisitWeakCell(Tagged<Map> map, Tagged<WeakCell> object);

  // ObjectVisitor overrides.
  void VisitMapPointer(Tagged<HeapObject> host) final {
    Tagged<Map> map = host->map(ObjectVisitorWithCageBases::cage_base());
    ProcessStrongHeapObject(host, host->map_slot(), map);
  }
  V8_INLINE void VisitPointer(Tagged<HeapObject> host, ObjectSlot p) final {
    VisitPointersImpl(host, p, p + 1);
  }
  V8_INLINE void VisitPointer(Tagged<HeapObject> host,
                              MaybeObjectSlot p) final {
    VisitPointersImpl(host, p, p + 1);
  }
  V8_INLINE void VisitPointers(Tagged<HeapObject> host, ObjectSlot start,
                               ObjectSlot end) final {
    VisitPointersImpl(host, start, end);
  }
  V8_INLINE void VisitPointers(Tagged<HeapObject> host, MaybeObjectSlot start,
                               MaybeObjectSlot end) final {
    VisitPointersImpl(host, start, end);
  }
  V8_INLINE void VisitInstructionStreamPointer(
      Tagged<Code> host, InstructionStreamSlot slot) final {
    VisitStrongPointerImpl(host, slot);
  }
  V8_INLINE void VisitEmbeddedPointer(Tagged<InstructionStream> host,
                                      RelocInfo* rinfo) final;
  V8_INLINE void VisitCodeTarget(Tagged<InstructionStream> host,
                                 RelocInfo* rinfo) final;
  void VisitCustomWeakPointers(Tagged<HeapObject> host, ObjectSlot start,
                               ObjectSlot end) final {
    // Weak list pointers should be ignored during marking. The lists are
    // reconstructed after GC.
  }

  V8_INLINE void VisitExternalPointer(Tagged<HeapObject> host,
                                      ExternalPointerSlot slot) final;
  V8_INLINE void VisitIndirectPointer(Tagged<HeapObject> host,
                                      IndirectPointerSlot slot,
                                      IndirectPointerMode mode) final;

  void VisitTrustedPointerTableEntry(Tagged<HeapObject> host,
                                     IndirectPointerSlot slot) final;

  V8_INLINE void VisitProtectedPointer(Tagged<TrustedObject> host,
                                       ProtectedPointerSlot slot) final {
    VisitStrongPointerImpl(host, slot);
  }

  void SynchronizePageAccess(Tagged<HeapObject> heap_object) {
#ifdef THREAD_SANITIZER
    // This is needed because TSAN does not process the memory fence
    // emitted after page initialization.
    MemoryChunk::FromHeapObject(heap_object)->SynchronizedLoad();
#endif
  }

  bool ShouldMarkObject(Tagged<HeapObject> object) const {
    if (InReadOnlySpace(object)) return false;
    if (should_mark_shared_heap_) return true;
    return !InAnySharedSpace(object);
  }

  // Marks the object grey and pushes it on the marking work list.
  V8_INLINE void MarkObject(Tagged<HeapObject> host, Tagged<HeapObject> obj);

  V8_INLINE static constexpr bool ShouldVisitReadOnlyMapPointer() {
    return false;
  }

  // Convenience method.
  bool IsUnmarked(Tagged<HeapObject> obj) const {
    return !concrete_visitor()->IsMarked(obj);
  }

 protected:
  using ConcurrentHeapVisitor<int, ConcreteVisitor>::concrete_visitor;

  template <typename THeapObjectSlot>
  void ProcessStrongHeapObject(Tagged<HeapObject> host, THeapObjectSlot slot,
                               Tagged<HeapObject> heap_object);
  template <typename THeapObjectSlot>
  void ProcessWeakHeapObject(Tagged<HeapObject> host, THeapObjectSlot slot,
                             Tagged<HeapObject> heap_object);

  template <typename TSlot>
  V8_INLINE void VisitPointersImpl(Tagged<HeapObject> host, TSlot start,
                                   TSlot end);

  template <typename TSlot>
  V8_INLINE void VisitStrongPointerImpl(Tagged<HeapObject> host, TSlot slot);

  V8_INLINE void VisitDescriptorsForMap(Tagged<Map> map);

  template <typename T>
  int VisitEmbedderTracingSubclass(Tagged<Map> map, Tagged<T> object);
  template <typename T>
  int VisitEmbedderTracingSubClassWithEmbedderTracing(Tagged<Map> map,
                                                      Tagged<T> object);
  template <typename T>
  int VisitEmbedderTracingSubClassNoEmbedderTracing(Tagged<Map> map,
                                                    Tagged<T> object);

  V8_INLINE int VisitFixedArrayWithProgressBar(Tagged<Map> map,
                                               Tagged<FixedArray> object,
                                               ProgressBar& progress_bar);
  V8_INLINE int VisitFixedArrayRegularly(Tagged<Map> map,
                                         Tagged<FixedArray> object);

  // Methods needed for supporting code flushing.
  bool ShouldFlushCode(Tagged<SharedFunctionInfo> sfi) const;
  bool ShouldFlushBaselineCode(Tagged<JSFunction> js_function) const;

  bool HasBytecodeArrayForFlushing(Tagged<SharedFunctionInfo> sfi) const;
  bool IsOld(Tagged<SharedFunctionInfo> sfi) const;
  void MakeOlder(Tagged<SharedFunctionInfo> sfi) const;

  MarkingWorklists::Local* const local_marking_worklists_;
  WeakObjects::Local* const local_weak_objects_;
  Heap* const heap_;
  const unsigned mark_compact_epoch_;
  const base::EnumSet<CodeFlushMode> code_flush_mode_;
  const bool trace_embedder_fields_;
  const bool should_keep_ages_unchanged_;
  const bool should_mark_shared_heap_;
  const uint16_t code_flushing_increase_;
  const bool isolate_in_background_;
#ifdef V8_ENABLE_SANDBOX
  ExternalPointerTable* const external_pointer_table_;
  ExternalPointerTable* const shared_external_pointer_table_;
  ExternalPointerTable::Space* const shared_external_pointer_space_;
  TrustedPointerTable* const trusted_pointer_table_;
#endif  // V8_ENABLE_SANDBOX
};

// This is the common base class for main and concurrent full marking visitors.
// Derived class are expected to provide the same methods as for
// MarkingVisitorBase except for those defined in this class.
template <typename ConcreteVisitor>
class FullMarkingVisitorBase : public MarkingVisitorBase<ConcreteVisitor> {
 public:
  FullMarkingVisitorBase(MarkingWorklists::Local* local_marking_worklists,
                         WeakObjects::Local* local_weak_objects, Heap* heap,
                         unsigned mark_compact_epoch,
                         base::EnumSet<CodeFlushMode> code_flush_mode,
                         bool trace_embedder_fields,
                         bool should_keep_ages_unchanged,
                         uint16_t code_flushing_increase)
      : MarkingVisitorBase<ConcreteVisitor>(
            local_marking_worklists, local_weak_objects, heap,
            mark_compact_epoch, code_flush_mode, trace_embedder_fields,
            should_keep_ages_unchanged, code_flushing_increase) {}

  V8_INLINE void AddStrongReferenceForReferenceSummarizer(
      Tagged<HeapObject> host, Tagged<HeapObject> obj) {}

  V8_INLINE void AddWeakReferenceForReferenceSummarizer(
      Tagged<HeapObject> host, Tagged<HeapObject> obj) {}

  constexpr bool CanUpdateValuesInHeap() { return true; }

  bool TryMark(Tagged<HeapObject> obj) {
    return MarkBit::From(obj).Set<AccessMode::ATOMIC>();
  }
  bool IsMarked(Tagged<HeapObject> obj) const {
    return MarkBit::From(obj).Get<AccessMode::ATOMIC>();
  }

  void MarkPointerTableEntry(Tagged<HeapObject> obj, IndirectPointerSlot slot);
};

}  // namespace internal
}  // namespace v8

#endif  // V8_HEAP_MARKING_VISITOR_H_
