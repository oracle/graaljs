/*
 * Copyright (c) 2022, 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * @option ecmascript-version=staging
 */

load('../assert.js')

const INJECTIONS = new WeakMap();

function createInjections() {
    const injections = [];
    function injectable(Class) {
        INJECTIONS.set(Class, injections);
    }
    function inject(injectionKey) {
        return function applyInjection(v, context) {
            injections.push({ injectionKey, set: context.access.set });
        }
    }
    return { injectable, inject };
}

class Container {
    registry = new Map();
    register(injectionKey, value) {
        this.registry.set(injectionKey, value);
    }
    lookup(injectionKey) {
        return this.registry.get(injectionKey);
    }

    create(Class) {
        let instance = new Class();
        for (const { injectionKey, set } of INJECTIONS.get(Class) || []) {
            set.call(instance, this.lookup(injectionKey));
        }
        return instance;
    }
}

class Store {}

const { injectable, inject } = createInjections();

@injectable
class C {
    @inject('store') store;
}

let container = new Container();
let store = new Store();
container.register('store', store);

let c = container.create(C);
assertTrue(c.store === store);
