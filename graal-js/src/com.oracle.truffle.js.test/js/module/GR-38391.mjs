/*
 * Copyright (c) 2022, 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

{
  const item = './GR-38391import.mjs';
  const namespace = await (function() {
    return import(item);
  })();
  if (namespace.default !== 42) {
    throw new Error('Should have been 42: ' + namespace.default);
  }
}

