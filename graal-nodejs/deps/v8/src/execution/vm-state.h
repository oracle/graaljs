// Copyright 2010 the V8 project authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

#ifndef V8_EXECUTION_VM_STATE_H_
#define V8_EXECUTION_VM_STATE_H_

#include "include/v8-unwinder.h"
#include "src/common/globals.h"
#include "src/logging/counters-scopes.h"
#include "v8-internal.h"

namespace v8 {
namespace internal {

// Logging and profiling. A StateTag represents a possible state of the VM. The
// logger maintains a stack of these. Creating a VMState object enters a state
// by pushing on the stack, and destroying a VMState object leaves a state by
// popping the current state from the stack.
template <StateTag Tag>
class VMState {
 public:
  explicit inline VMState(Isolate* isolate);
  inline ~VMState();

  Isolate* isolate() { return isolate_; }

 private:
  Isolate* const isolate_;
  StateTag const previous_tag_;

  friend ExternalCallbackScope;
};

class V8_NODISCARD ExternalCallbackScope {
 public:
  inline ExternalCallbackScope(Isolate* isolate, Address callback);
  inline ~ExternalCallbackScope();
  Address callback() { return callback_; }
  Address* callback_entrypoint_address() {
    if (callback_ == kNullAddress) return nullptr;
#if USES_FUNCTION_DESCRIPTORS
    return FUNCTION_ENTRYPOINT_ADDRESS(callback_);
#else
    return const_cast<Address*>(&callback_);
#endif
  }
  ExternalCallbackScope* previous() { return previous_scope_; }
  inline Address scope_address();

 private:
  Address const callback_;
  ExternalCallbackScope* const previous_scope_;
  VMState<EXTERNAL> const vm_state_;
  PauseNestedTimedHistogramScope const pause_timed_histogram_scope_;
#ifdef USE_SIMULATOR
  Address scope_address_;
#endif
};

}  // namespace internal
}  // namespace v8

#endif  // V8_EXECUTION_VM_STATE_H_
