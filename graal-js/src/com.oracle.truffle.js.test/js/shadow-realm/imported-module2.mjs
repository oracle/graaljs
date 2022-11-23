/*
 * Copyright (c) 2022, 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Used by ShadowRealm tests.
 */

export var foo = {};

var localValue;
export function setLocal(value) {
    localValue = value;
}

export function getLocal() {
    return localValue;
}

export function setGlobal(value) {
    globalThis.globalValue = value;
}

export function getGlobal() {
    return globalThis.globalValue;
}
