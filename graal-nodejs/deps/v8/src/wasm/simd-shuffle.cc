// Copyright 2020 the V8 project authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

#include "src/wasm/simd-shuffle.h"

#include <algorithm>

#include "src/common/globals.h"

namespace v8 {
namespace internal {
namespace wasm {

bool SimdShuffle::TryMatchIdentity(const uint8_t* shuffle) {
  for (int i = 0; i < kSimd128Size; ++i) {
    if (shuffle[i] != i) return false;
  }
  return true;
}

bool SimdShuffle::TryMatch32x4Rotate(const uint8_t* shuffle,
                                     uint8_t* shuffle32x4, bool is_swizzle) {
  uint8_t offset;
  bool is_concat = TryMatchConcat(shuffle, &offset);
  DCHECK_NE(offset, 0);  // 0 is identity, it should not be matched.
  // Since we already have a concat shuffle, we know that the indices goes from:
  // [ offset, ..., 15, 0, ... ], it suffices to check that the offset points
  // to the low byte of a 32x4 element.
  if (!is_concat || !is_swizzle || offset % 4 != 0) {
    return false;
  }

  uint8_t offset_32 = offset / 4;
  for (int i = 0; i < 4; i++) {
    shuffle32x4[i] = (offset_32 + i) % 4;
  }
  return true;
}

bool SimdShuffle::TryMatch32x4Reverse(const uint8_t* shuffle32x4) {
  return shuffle32x4[0] == 3 && shuffle32x4[1] == 2 && shuffle32x4[2] == 1 &&
         shuffle32x4[3] == 0;
}

bool SimdShuffle::TryMatch32x4OneLaneSwizzle(const uint8_t* shuffle32x4,
                                             uint8_t* from_lane,
                                             uint8_t* to_lane) {
  constexpr uint32_t patterns[12]{
      0x30200000,  // 0 -> 1
      0x30000100,  // 0 -> 2
      0x00020100,  // 0 -> 3
      0x03020101,  // 1 -> 0
      0x03010100,  // 1 -> 2
      0x01020100,  // 1 -> 3
      0x03020102,  // 2 -> 0
      0x03020200,  // 2 -> 1
      0x02020100,  // 2 -> 3
      0x03020103,  // 3 -> 0
      0x03020300,  // 3 -> 1
      0x03030100   // 3 -> 2
  };

  unsigned pattern_idx = 0;
  uint32_t shuffle = *reinterpret_cast<const uint32_t*>(shuffle32x4);
#ifdef V8_TARGET_BIG_ENDIAN
  shuffle = base::bits::ReverseBytes(shuffle);
#endif
  for (unsigned from = 0; from < 4; ++from) {
    for (unsigned to = 0; to < 4; ++to) {
      if (from == to) {
        continue;
      }
      if (shuffle == patterns[pattern_idx]) {
        *from_lane = from;
        *to_lane = to;
        return true;
      }
      ++pattern_idx;
    }
  }
  return false;
}

bool SimdShuffle::TryMatch32x4Shuffle(const uint8_t* shuffle,
                                      uint8_t* shuffle32x4) {
  for (int i = 0; i < 4; ++i) {
    if (shuffle[i * 4] % 4 != 0) return false;
    for (int j = 1; j < 4; ++j) {
      if (shuffle[i * 4 + j] - shuffle[i * 4 + j - 1] != 1) return false;
    }
    shuffle32x4[i] = shuffle[i * 4] / 4;
  }
  return true;
}

bool SimdShuffle::TryMatch32x8Shuffle(const uint8_t* shuffle,
                                      uint8_t* shuffle32x8) {
  for (int i = 0; i < 8; ++i) {
    if (shuffle[i * 4] % 4 != 0) return false;
    for (int j = 1; j < 4; ++j) {
      if (shuffle[i * 4 + j] - shuffle[i * 4 + j - 1] != 1) return false;
    }
    shuffle32x8[i] = shuffle[i * 4] / 4;
  }
  return true;
}

bool SimdShuffle::TryMatch16x8Shuffle(const uint8_t* shuffle,
                                      uint8_t* shuffle16x8) {
  for (int i = 0; i < 8; ++i) {
    if (shuffle[i * 2] % 2 != 0) return false;
    for (int j = 1; j < 2; ++j) {
      if (shuffle[i * 2 + j] - shuffle[i * 2 + j - 1] != 1) return false;
    }
    shuffle16x8[i] = shuffle[i * 2] / 2;
  }
  return true;
}

bool SimdShuffle::TryMatchConcat(const uint8_t* shuffle, uint8_t* offset) {
  // Don't match the identity shuffle (e.g. [0 1 2 ... 15]).
  uint8_t start = shuffle[0];
  if (start == 0) return false;
  DCHECK_GT(kSimd128Size, start);  // The shuffle should be canonicalized.
  // A concatenation is a series of consecutive indices, with at most one jump
  // in the middle from the last lane to the first.
  for (int i = 1; i < kSimd128Size; ++i) {
    if ((shuffle[i]) != ((shuffle[i - 1] + 1))) {
      if (shuffle[i - 1] != 15) return false;
      if (shuffle[i] % kSimd128Size != 0) return false;
    }
  }
  *offset = start;
  return true;
}

bool SimdShuffle::TryMatchBlend(const uint8_t* shuffle) {
  for (int i = 0; i < 16; ++i) {
    if ((shuffle[i] & 0xF) != i) return false;
  }
  return true;
}

bool SimdShuffle::TryMatchByteToDwordZeroExtend(const uint8_t* shuffle) {
  for (int i = 0; i < 16; ++i) {
    if ((i % 4 != 0) && (shuffle[i] < 16)) return false;
    if ((i % 4 == 0) && (shuffle[i] > 15 || (shuffle[i] != shuffle[0] + i / 4)))
      return false;
  }
  return true;
}

uint8_t SimdShuffle::PackShuffle4(uint8_t* shuffle) {
  return (shuffle[0] & 3) | ((shuffle[1] & 3) << 2) | ((shuffle[2] & 3) << 4) |
         ((shuffle[3] & 3) << 6);
}

uint8_t SimdShuffle::PackBlend8(const uint8_t* shuffle16x8) {
  int8_t result = 0;
  for (int i = 0; i < 8; ++i) {
    result |= (shuffle16x8[i] >= 8 ? 1 : 0) << i;
  }
  return result;
}

uint8_t SimdShuffle::PackBlend4(const uint8_t* shuffle32x4) {
  int8_t result = 0;
  for (int i = 0; i < 4; ++i) {
    result |= (shuffle32x4[i] >= 4 ? 0x3 : 0) << (i * 2);
  }
  return result;
}

int32_t SimdShuffle::Pack4Lanes(const uint8_t* shuffle) {
  int32_t result = 0;
  for (int i = 3; i >= 0; --i) {
    result <<= 8;
    result |= shuffle[i];
  }
  return result;
}

void SimdShuffle::Pack16Lanes(uint32_t* dst, const uint8_t* shuffle) {
  for (int i = 0; i < 4; i++) {
    dst[i] = wasm::SimdShuffle::Pack4Lanes(shuffle + (i * 4));
  }
}

bool SimdSwizzle::AllInRangeOrTopBitSet(
    std::array<uint8_t, kSimd128Size> shuffle) {
  return std::all_of(shuffle.begin(), shuffle.end(),
                     [](auto i) { return (i < kSimd128Size) || (i & 0x80); });
}

}  // namespace wasm
}  // namespace internal
}  // namespace v8
