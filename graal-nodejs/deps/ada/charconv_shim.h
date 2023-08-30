/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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

#ifndef CHARCONV_SHIM_H_
#define CHARCONV_SHIM_H_

// Poor man's <charconv> substitute for unsigned integer types only.
// Allows Ada to be compiled on pre-C++17 macOS <10.15 (GR-48310).

#include <type_traits>
#include <limits>
#include <system_error> // std::errc

namespace charconv_shim {
    struct from_chars_result {
        const char* ptr;
        std::errc ec;
    };
    struct to_chars_result {
        char* ptr;
        std::errc ec;
    };

    constexpr char to_lower(char x) {
        return (x | 0x20);
    }

    template <typename UINT_T, UINT_T max = std::numeric_limits<UINT_T>::max(),
        typename std::enable_if<std::is_integral<UINT_T>::value, int>::type = 0,
        typename std::enable_if<!std::is_signed<UINT_T>::value, int>::type = 0>
    inline from_chars_result from_chars(const char* first, const char* last, UINT_T& value, int base = 10) {
        if (base < 2 || base > 36) {
            return {first, std::errc::invalid_argument};
        }
        const UINT_T mul_limit = max / base;
        UINT_T result = 0;
        auto cursor = first;
        while (cursor < last) {
            char c = *cursor;
            char l = to_lower(c);
            char d;
            if (c >= '0' && c < '0' + std::min(base, 10)) {
                d = c - '0';
            } else if (base > 10 && l >= 'a' && l < 'a' + (base - 10)) {
                d = l - 'a';
            } else {
                break;
            }
            if (result > mul_limit) {
                // multiplication would result in integer overflow
                return {last, std::errc::result_out_of_range};
            }
            result *= base;
            if (result > max - d) {
                // addition would result in integer overflow
                return {last, std::errc::result_out_of_range};
            }
            result += d;
            ++cursor;
        }
        if (cursor == first) {
            return {first, std::errc::invalid_argument};
        } else {
            value = result;
            return {cursor, {}};
        }
    }

    template <typename UINT_T,
        typename std::enable_if<std::is_integral<UINT_T>::value, int>::type = 0,
        typename std::enable_if<!std::is_signed<UINT_T>::value, int>::type = 0>
    inline to_chars_result to_chars(char* first, char* last, UINT_T value, int base = 10) {
        if (base < 2 || base > 36) {
            return {first, std::errc::invalid_argument};
        }
        int length = 1;
        for (auto v = value; v >= base; v /= base) {
            ++length;
        }
        if (last - first < length) {
            return {last, std::errc::value_too_large};
        }

        const char *const digits = "0123456789abcdefghijklmnopqrstuvwxyz";
        last = first + length;
        char* cursor = last;
        if (value == 0) {
            *--cursor = '0';
        } else {
            for (auto v = value; v > 0; v /= base) {
                *--cursor = digits[v % base];
            }
        }
        return {last, {}};
    }
}

namespace std {
    using ::charconv_shim::from_chars_result;
    using ::charconv_shim::to_chars_result;
    using ::charconv_shim::from_chars;
    using ::charconv_shim::to_chars;
}

#endif
