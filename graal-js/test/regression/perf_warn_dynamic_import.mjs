/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

async function loadValue() {
  const ns = await import("./perf_warn_dynamic_import_dep.mjs");
  if (ns.value !== 42) {
    throw new Error(`unexpected import value: ${ns.value}`);
  }
  return ns.value;
}

let sum = 0;
for (let i = 0; i < 5_000; i++) {
  sum += await loadValue();
}

if (sum !== 42 * 5_000) {
  throw new Error(`unexpected sum: ${sum}`);
}

print("done");
