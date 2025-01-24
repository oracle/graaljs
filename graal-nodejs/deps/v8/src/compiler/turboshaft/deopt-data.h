// Copyright 2022 the V8 project authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

#ifndef V8_COMPILER_TURBOSHAFT_DEOPT_DATA_H_
#define V8_COMPILER_TURBOSHAFT_DEOPT_DATA_H_

#include "src/base/small-vector.h"
#include "src/common/globals.h"
#include "src/compiler/frame-states.h"
#include "src/compiler/turboshaft/index.h"
#include "src/compiler/turboshaft/representations.h"

namespace v8::internal::compiler::turboshaft {

struct FrameStateData {
  // The data is encoded as a pre-traversal of a tree.
  enum class Instr : uint8_t {
    kInput,  // 1 Operand: input machine type
    kUnusedRegister,
    kDematerializedObject,           // 2 Operands: id, field_count
    kDematerializedObjectReference,  // 1 Operand: id
    kArgumentsElements,              // 1 Operand: type
    kArgumentsLength,
  };

  class Builder {
   public:
    void AddParentFrameState(OpIndex parent) {
      DCHECK(inputs_.empty());
      inlined_ = true;
      inputs_.push_back(parent);
    }
    void AddInput(MachineType type, OpIndex input) {
      instructions_.push_back(Instr::kInput);
      machine_types_.push_back(type);
      inputs_.push_back(input);
    }

    void AddUnusedRegister() {
      instructions_.push_back(Instr::kUnusedRegister);
    }

    void AddDematerializedObjectReference(uint32_t id) {
      instructions_.push_back(Instr::kDematerializedObjectReference);
      int_operands_.push_back(id);
    }

    void AddDematerializedObject(uint32_t id, uint32_t field_count) {
      instructions_.push_back(Instr::kDematerializedObject);
      int_operands_.push_back(id);
      int_operands_.push_back(field_count);
    }

    void AddArgumentsElements(CreateArgumentsType type) {
      instructions_.push_back(Instr::kArgumentsElements);
      int_operands_.push_back(static_cast<int>(type));
    }

    void AddArgumentsLength() {
      instructions_.push_back(Instr::kArgumentsLength);
    }

    const FrameStateData* AllocateFrameStateData(
        const FrameStateInfo& frame_state_info, Zone* zone) {
      return zone->New<FrameStateData>(FrameStateData{
          frame_state_info, zone->CloneVector(base::VectorOf(instructions_)),
          zone->CloneVector(base::VectorOf(machine_types_)),
          zone->CloneVector(base::VectorOf(int_operands_))});
    }

    base::Vector<const OpIndex> Inputs() { return base::VectorOf(inputs_); }
    bool inlined() const { return inlined_; }

   private:
    base::SmallVector<Instr, 32> instructions_;
    base::SmallVector<MachineType, 32> machine_types_;
    base::SmallVector<uint32_t, 16> int_operands_;
    base::SmallVector<OpIndex, 32> inputs_;

    bool inlined_ = false;
  };

  struct Iterator {
    base::Vector<const Instr> instructions;
    base::Vector<const MachineType> machine_types;
    base::Vector<const uint32_t> int_operands;
    base::Vector<const OpIndex> inputs;

    bool has_more() const {
      DCHECK_IMPLIES(instructions.empty(), machine_types.empty());
      DCHECK_IMPLIES(instructions.empty(), int_operands.empty());
      DCHECK_IMPLIES(instructions.empty(), inputs.empty());
      return !instructions.empty();
    }

    Instr current_instr() { return instructions[0]; }

    void ConsumeInput(MachineType* machine_type, OpIndex* input) {
      DCHECK_EQ(instructions[0], Instr::kInput);
      instructions += 1;
      *machine_type = machine_types[0];
      machine_types += 1;
      *input = inputs[0];
      inputs += 1;
    }
    void ConsumeUnusedRegister() {
      DCHECK_EQ(instructions[0], Instr::kUnusedRegister);
      instructions += 1;
    }
    void ConsumeDematerializedObject(uint32_t* id, uint32_t* field_count) {
      DCHECK_EQ(instructions[0], Instr::kDematerializedObject);
      instructions += 1;
      *id = int_operands[0];
      *field_count = int_operands[1];
      int_operands += 2;
    }
    void ConsumeDematerializedObjectReference(uint32_t* id) {
      DCHECK_EQ(instructions[0], Instr::kDematerializedObjectReference);
      instructions += 1;
      *id = int_operands[0];
      int_operands += 1;
    }
    void ConsumeArgumentsElements(CreateArgumentsType* type) {
      DCHECK_EQ(instructions[0], Instr::kArgumentsElements);
      instructions += 1;
      *type = static_cast<CreateArgumentsType>(int_operands[0]);
      int_operands += 1;
    }
    void ConsumeArgumentsLength() {
      DCHECK_EQ(instructions[0], Instr::kArgumentsLength);
      instructions += 1;
    }
  };

  Iterator iterator(base::Vector<const OpIndex> state_values) const {
    return Iterator{instructions, machine_types, int_operands, state_values};
  }

  const FrameStateInfo& frame_state_info;
  base::Vector<Instr> instructions;
  base::Vector<MachineType> machine_types;
  base::Vector<uint32_t> int_operands;
};

inline bool operator==(const FrameStateData& lhs, const FrameStateData& rhs) {
  return lhs.frame_state_info == rhs.frame_state_info &&
         lhs.instructions == rhs.instructions &&
         lhs.machine_types == rhs.machine_types &&
         lhs.int_operands == rhs.int_operands;
}

}  // namespace v8::internal::compiler::turboshaft

#endif  // V8_COMPILER_TURBOSHAFT_DEOPT_DATA_H_
