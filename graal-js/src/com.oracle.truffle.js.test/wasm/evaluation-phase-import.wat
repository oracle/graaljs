;; Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
;; Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.

(module
  (type $fib (func (param i32) (result i64)))
  (type $mul (func (param i32 i32) (result i32)))
  (import "./fib.wasm" "fib" (func $fib (type $fib)))
  (import "./evaluation-phase-import.mjs" "mul" (func $mul (type $mul)))

  (memory $0 1)
  (data (memory $0) (i32.const 0) "JS <3 Wasm")
  (global $answer i32 (i32.const 42))
  (func $default (result i32)
    (call $mul (global.get $answer) (global.get $answer))
    return
  )

  (export "memory" (memory $0))
  (export "answer" (global $answer))
  (export "fib" (func $fib))
  (export "default" (func $default))
)
