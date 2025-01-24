// Copyright 2023 the V8 project authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

#include "src/objects/dependent-code.h"

#include "src/deoptimizer/deoptimizer.h"
#include "src/objects/allocation-site-inl.h"
#include "src/objects/dependent-code-inl.h"
#include "src/objects/map.h"

namespace v8 {
namespace internal {

Tagged<DependentCode> DependentCode::GetDependentCode(
    Tagged<HeapObject> object) {
  if (IsMap(object)) {
    return Map::cast(object)->dependent_code();
  } else if (IsPropertyCell(object)) {
    return PropertyCell::cast(object)->dependent_code();
  } else if (IsAllocationSite(object)) {
    return AllocationSite::cast(object)->dependent_code();
  } else if (IsConstTrackingLetCell(object)) {
    return ConstTrackingLetCell::cast(object)->dependent_code();
  }
  UNREACHABLE();
}

void DependentCode::SetDependentCode(Handle<HeapObject> object,
                                     Handle<DependentCode> dep) {
  if (IsMap(*object)) {
    Handle<Map>::cast(object)->set_dependent_code(*dep);
  } else if (IsPropertyCell(*object)) {
    Handle<PropertyCell>::cast(object)->set_dependent_code(*dep);
  } else if (IsAllocationSite(*object)) {
    Handle<AllocationSite>::cast(object)->set_dependent_code(*dep);
  } else if (IsConstTrackingLetCell(*object)) {
    Handle<ConstTrackingLetCell>::cast(object)->set_dependent_code(*dep);
  } else {
    UNREACHABLE();
  }
}

namespace {

void PrintDependencyGroups(DependentCode::DependencyGroups groups) {
  while (groups != 0) {
    auto group = static_cast<DependentCode::DependencyGroup>(
        1 << base::bits::CountTrailingZeros(static_cast<uint32_t>(groups)));
    StdoutStream{} << DependentCode::DependencyGroupName(group);
    groups &= ~group;
    if (groups != 0) StdoutStream{} << ",";
  }
}

}  // namespace

void DependentCode::InstallDependency(Isolate* isolate, Handle<Code> code,
                                      Handle<HeapObject> object,
                                      DependencyGroups groups) {
  if (V8_UNLIKELY(v8_flags.trace_compilation_dependencies)) {
    StdoutStream{} << "Installing dependency of [" << code << "] on [" << object
                   << "] in groups [";
    PrintDependencyGroups(groups);
    StdoutStream{} << "]\n";
  }
  Handle<DependentCode> old_deps(DependentCode::GetDependentCode(*object),
                                 isolate);
  Handle<DependentCode> new_deps =
      InsertWeakCode(isolate, old_deps, groups, code);

  // Update the list head if necessary.
  if (!new_deps.is_identical_to(old_deps)) {
    DependentCode::SetDependentCode(object, new_deps);
  }
}

Handle<DependentCode> DependentCode::InsertWeakCode(
    Isolate* isolate, Handle<DependentCode> entries, DependencyGroups groups,
    Handle<Code> code) {
  if (entries->length() == entries->capacity()) {
    // We'd have to grow - try to compact first.
    entries->IterateAndCompact(
        isolate, [](Tagged<Code>, DependencyGroups) { return false; });
  }

  // As the Code object lives outside of the sandbox in trusted space, we need
  // to use its in-sandbox wrapper object here.
  MaybeObjectHandle code_slot(MakeWeak(code->wrapper()), isolate);
  entries = Handle<DependentCode>::cast(WeakArrayList::AddToEnd(
      isolate, entries, code_slot, Smi::FromInt(groups)));
  return entries;
}

template <typename Function>
void DependentCode::IterateAndCompact(IsolateForSandbox isolate,
                                      const Function& fn) {
  DisallowGarbageCollection no_gc;

  int len = length();
  if (len == 0) return;

  // We compact during traversal, thus use a somewhat custom loop construct:
  //
  // - Loop back-to-front s.t. trailing cleared entries can simply drop off
  //   the back of the list.
  // - Any cleared slots are filled from the back of the list.
  int i = len - kSlotsPerEntry;
  while (i >= 0) {
    Tagged<MaybeObject> obj = Get(i + kCodeSlotOffset);
    if (obj.IsCleared()) {
      len = FillEntryFromBack(i, len);
      i -= kSlotsPerEntry;
      continue;
    }

    if (fn(CodeWrapper::cast(obj.GetHeapObjectAssumeWeak())->code(isolate),
           static_cast<DependencyGroups>(
               Get(i + kGroupsSlotOffset).ToSmi().value()))) {
      len = FillEntryFromBack(i, len);
    }

    i -= kSlotsPerEntry;
  }

  set_length(len);
}

bool DependentCode::MarkCodeForDeoptimization(
    Isolate* isolate, DependentCode::DependencyGroups deopt_groups) {
  DisallowGarbageCollection no_gc;

  bool marked_something = false;
  IterateAndCompact(isolate, [&](Tagged<Code> code, DependencyGroups groups) {
    if ((groups & deopt_groups) == 0) return false;

    if (!code->marked_for_deoptimization()) {
      code->SetMarkedForDeoptimization(isolate, "code dependencies");
      marked_something = true;
    }

    return true;
  });

  return marked_something;
}

int DependentCode::FillEntryFromBack(int index, int length) {
  DCHECK_EQ(index % 2, 0);
  DCHECK_EQ(length % 2, 0);
  for (int i = length - kSlotsPerEntry; i > index; i -= kSlotsPerEntry) {
    Tagged<MaybeObject> obj = Get(i + kCodeSlotOffset);
    if (obj.IsCleared()) continue;

    Set(index + kCodeSlotOffset, obj);
    Set(index + kGroupsSlotOffset, Get(i + kGroupsSlotOffset),
        SKIP_WRITE_BARRIER);
    return i;
  }
  return index;  // No non-cleared entry found.
}

void DependentCode::DeoptimizeDependencyGroups(
    Isolate* isolate, DependentCode::DependencyGroups groups) {
  DisallowGarbageCollection no_gc_scope;
  bool marked_something = MarkCodeForDeoptimization(isolate, groups);
  if (marked_something) {
    DCHECK(AllowCodeDependencyChange::IsAllowed());
    Deoptimizer::DeoptimizeMarkedCode(isolate);
  }
}

// static
Tagged<DependentCode> DependentCode::empty_dependent_code(
    const ReadOnlyRoots& roots) {
  return DependentCode::cast(roots.empty_weak_array_list());
}

const char* DependentCode::DependencyGroupName(DependencyGroup group) {
  switch (group) {
    case kTransitionGroup:
      return "transition";
    case kPrototypeCheckGroup:
      return "prototype-check";
    case kPropertyCellChangedGroup:
      return "property-cell-changed";
    case kFieldConstGroup:
      return "field-const";
    case kFieldTypeGroup:
      return "field-type";
    case kFieldRepresentationGroup:
      return "field-representation";
    case kInitialMapChangedGroup:
      return "initial-map-changed";
    case kAllocationSiteTenuringChangedGroup:
      return "allocation-site-tenuring-changed";
    case kAllocationSiteTransitionChangedGroup:
      return "allocation-site-transition-changed";
    case kConstTrackingLetChangedGroup:
      return "const-tracking-let-changed";
  }
  UNREACHABLE();
}

}  // namespace internal
}  // namespace v8
