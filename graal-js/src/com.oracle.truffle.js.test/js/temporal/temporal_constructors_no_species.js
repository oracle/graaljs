/*
 * Copyright (c) 2022, 2023, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * @option temporal=true
 */

Object.getOwnPropertyNames(Temporal).filter(C => typeof C === 'function').forEach(ctor => {
    if (Symbol.species in ctor) {
        throw new Error(`Temporal.${ctor.name} constructor should not have @@species property.`);
    }
})
