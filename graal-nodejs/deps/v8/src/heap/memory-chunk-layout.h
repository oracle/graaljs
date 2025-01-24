// Copyright 2020 the V8 project authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

#ifndef V8_HEAP_MEMORY_CHUNK_LAYOUT_H_
#define V8_HEAP_MEMORY_CHUNK_LAYOUT_H_

#include "src/base/platform/mutex.h"
#include "src/common/globals.h"
#include "src/heap/base/active-system-pages.h"
#include "src/heap/list.h"
#include "src/heap/marking.h"
#include "src/heap/progress-bar.h"
#include "src/heap/slot-set.h"

namespace v8 {
namespace internal {

class MarkingBitmap;
class FreeListCategory;
class Heap;
class TypedSlotsSet;
class SlotSet;
class MemoryChunkMetadata;

enum RememberedSetType {
  OLD_TO_NEW,
  OLD_TO_NEW_BACKGROUND,
  OLD_TO_OLD,
  OLD_TO_SHARED,
  OLD_TO_CODE,
  TRUSTED_TO_TRUSTED,
  NUMBER_OF_REMEMBERED_SET_TYPES
};

using ActiveSystemPages = ::heap::base::ActiveSystemPages;

class V8_EXPORT_PRIVATE MemoryChunkLayout {
 public:
  static constexpr int kNumSets = NUMBER_OF_REMEMBERED_SET_TYPES;
  static constexpr int kNumTypes =
      static_cast<int>(ExternalBackingStoreType::kNumValues);
  static constexpr int kMemoryChunkAlignment = sizeof(size_t);
#define FIELD(Type, Name) \
  k##Name##Offset, k##Name##End = k##Name##Offset + sizeof(Type) - 1
  enum Header {
    // MemoryChunk fields:
    FIELD(uintptr_t, Flags),
    FIELD(MemoryChunkMetadata*, Metadata),
    // MemoryChunkMetadata fields:
    FIELD(size_t, Size),
    FIELD(Heap*, Heap),
    FIELD(Address, AreaStart),
    FIELD(Address, AreaEnd),
    FIELD(size_t, AllocatedBytes),
    FIELD(size_t, WastedMemory),
    FIELD(std::atomic<intptr_t>, HighWaterMark),
    FIELD(Address, Owner),
    FIELD(VirtualMemory, Reservation),
    // MutablePageMetadata fields:
    FIELD(SlotSet* [kNumSets], SlotSet),
    FIELD(TypedSlotsSet* [kNumSets], TypedSlotSet),
    FIELD(ProgressBar, ProgressBar),
    FIELD(std::atomic<intptr_t>, LiveByteCount),
    FIELD(base::Mutex*, Mutex),
    FIELD(base::SharedMutex*, SharedMutex),
    FIELD(base::Mutex*, PageProtectionChangeMutex),
    FIELD(std::atomic<intptr_t>, ConcurrentSweeping),
    FIELD(std::atomic<size_t>[kNumTypes], ExternalBackingStoreBytes),
    FIELD(heap::ListNode<MutablePageMetadata>, ListNode),
    FIELD(FreeListCategory**, Categories),
    FIELD(PossiblyEmptyBuckets, PossiblyEmptyBuckets),
    FIELD(ActiveSystemPages*, ActiveSystemPages),
    FIELD(size_t, AllocatedLabSize),
    FIELD(size_t, AgeInNewSpace),
    FIELD(MarkingBitmap, MarkingBitmap),
    kEndOfMarkingBitmap,
    kMemoryChunkHeaderSize =
        kEndOfMarkingBitmap +
        ((kEndOfMarkingBitmap % kMemoryChunkAlignment) == 0
             ? 0
             : kMemoryChunkAlignment -
                   (kEndOfMarkingBitmap % kMemoryChunkAlignment)),
    kMemoryChunkHeaderStart = kSlotSetOffset,
    kBasicMemoryChunkHeaderSize = kMemoryChunkHeaderStart,
    kBasicMemoryChunkHeaderStart = 0,
  };
#undef FIELD

  static size_t CodePageGuardStartOffset();
  static size_t CodePageGuardSize();
  // Code pages have padding on the first page for code alignment, so the
  // ObjectStartOffset will not be page aligned.
  static intptr_t ObjectPageOffsetInCodePage();
  static intptr_t ObjectStartOffsetInCodePage();
  static intptr_t ObjectEndOffsetInCodePage();
  static size_t AllocatableMemoryInCodePage();
  static size_t ObjectStartOffsetInDataPage();
  static size_t AllocatableMemoryInDataPage();
  static intptr_t ObjectStartOffsetInReadOnlyPage();
  static size_t AllocatableMemoryInReadOnlyPage();
  static size_t ObjectStartOffsetInMemoryChunk(AllocationSpace space);
  static size_t AllocatableMemoryInMemoryChunk(AllocationSpace space);

  static int MaxRegularCodeObjectSize();

  static_assert(kMemoryChunkHeaderSize % alignof(size_t) == 0);
};

}  // namespace internal
}  // namespace v8

#endif  // V8_HEAP_MEMORY_CHUNK_LAYOUT_H_
