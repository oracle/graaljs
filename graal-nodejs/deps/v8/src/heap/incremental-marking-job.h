// Copyright 2012 the V8 project authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

#ifndef V8_HEAP_INCREMENTAL_MARKING_JOB_H_
#define V8_HEAP_INCREMENTAL_MARKING_JOB_H_

#include "include/v8-platform.h"
#include "src/base/optional.h"
#include "src/base/platform/mutex.h"
#include "src/base/platform/time.h"

namespace v8::internal {

class Heap;
class Isolate;

// The incremental marking job uses platform tasks to perform incremental
// marking actions (start, step, finalize). The job posts regular foreground
// tasks or delayed foreground tasks if marking progress allows.
class IncrementalMarkingJob final {
 public:
  enum class TaskType {
    kNormal,
    kPending,
  };

  explicit IncrementalMarkingJob(Heap* heap);

  IncrementalMarkingJob(const IncrementalMarkingJob&) = delete;
  IncrementalMarkingJob& operator=(const IncrementalMarkingJob&) = delete;

  // Schedules a task with a given `task_type`. Safe to be called from any
  // thread.
  void ScheduleTask(TaskType task_type = TaskType::kNormal);

  // Returns a weighted average of time to task. For delayed tasks the time to
  // task is only recorded after the initial delay. In case a task is currently
  // running, it is added to the average.
  base::Optional<v8::base::TimeDelta> AverageTimeToTask() const;

  base::Optional<v8::base::TimeDelta> CurrentTimeToTask() const;

 private:
  class Task;

  Heap* const heap_;
  const std::shared_ptr<v8::TaskRunner> foreground_task_runner_;
  mutable base::Mutex mutex_;
  v8::base::TimeTicks scheduled_time_;
  base::Optional<TaskType> pending_task_;
};

constexpr const char* ToString(IncrementalMarkingJob::TaskType task_type) {
  switch (task_type) {
    case IncrementalMarkingJob::TaskType::kNormal:
      return "normal";
    case IncrementalMarkingJob::TaskType::kPending:
      return "pending";
  }
}

}  // namespace v8::internal

#endif  // V8_HEAP_INCREMENTAL_MARKING_JOB_H_
