;; Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
;; Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.

(;
export function fib(n: i32): i64 {
  if (n <= 0) {
    return 0;
  }
  let a: i64 = 0;
  let b: i64 = 1;
  for (let i: i32 = 2; i <= n; i++) {
    let t = a + b;
    a = b;
    b = t;
  }
  return b;
}
;)

(module
  (type $fib (func (param i32) (result i64)))
  (func $fib (param $n i32) (result i64)
    (local $a i64)
    (local $b i64)
    (local $i i32)

    (i32.le_s (local.get $n) (i32.const 0))
    if
      (return (i64.const 0))
    end

    (local.set $a (i64.const 0))
    (local.set $b (i64.const 1))
    (local.set $i (i32.const 2))
    ;; while (i <= n)
    loop $continue
      (i32.le_s (local.get $i) (local.get $n))
      if
        ;; t = a + b
        (i64.add (local.get $a) (local.get $b))
        ;; a = b
        (local.set $a (local.get $b))
        ;; b = t
        local.set $b
        ;; i++
        (local.set $i (i32.add (local.get $i) (i32.const 1)))
        br $continue
      end
    end

    (return (local.get $b))
  )
  (export "fib" (func $fib))
)
