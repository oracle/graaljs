;;
;; Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
;; Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
;;

(module
  (func $mul (param i32 i32) (result i32)
    local.get 0
    local.get 1
    i32.mul
  )
  (export "mul" (func $mul))
)
