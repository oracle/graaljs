// Copyright 2020 the V8 project authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

#include "src/execution/arguments-inl.h"
#include "src/objects/js-weak-refs-inl.h"
#include "src/runtime/runtime-utils.h"

namespace v8 {
namespace internal {

RUNTIME_FUNCTION(Runtime_ShrinkFinalizationRegistryUnregisterTokenMap) {
  HandleScope scope(isolate);
  DCHECK_EQ(1, args.length());
  Handle<JSFinalizationRegistry> finalization_registry =
      args.at<JSFinalizationRegistry>(0);

  if (!finalization_registry->key_map().IsUndefined(isolate)) {
    Handle<SimpleNumberDictionary> key_map =
        handle(SimpleNumberDictionary::cast(finalization_registry->key_map()),
               isolate);
    key_map = SimpleNumberDictionary::Shrink(isolate, key_map);
    finalization_registry->set_key_map(*key_map);
  }

  return ReadOnlyRoots(isolate).undefined_value();
}

RUNTIME_FUNCTION(
    Runtime_JSFinalizationRegistryRegisterWeakCellWithUnregisterToken) {
  HandleScope scope(isolate);
  DCHECK_EQ(2, args.length());
  Handle<JSFinalizationRegistry> finalization_registry =
      args.at<JSFinalizationRegistry>(0);
  Handle<WeakCell> weak_cell = args.at<WeakCell>(1);

  JSFinalizationRegistry::RegisterWeakCellWithUnregisterToken(
      finalization_registry, weak_cell, isolate);

  return ReadOnlyRoots(isolate).undefined_value();
}

RUNTIME_FUNCTION(Runtime_JSWeakRefAddToKeptObjects) {
  HandleScope scope(isolate);
  DCHECK_EQ(1, args.length());
  Handle<HeapObject> object = args.at<HeapObject>(0);
  DCHECK(object->CanBeHeldWeakly());

  isolate->heap()->KeepDuringJob(object);

  return ReadOnlyRoots(isolate).undefined_value();
}

}  // namespace internal
}  // namespace v8
