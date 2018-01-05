/*
 * Copyright (c) 2015, 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

"use strict";

(function() {

var REVOCABLE_PROXY = Internal.HiddenKey("RevocableProxy");

function CreateProxyRevokerFunction() {
    var F = function() {
        var p = F[REVOCABLE_PROXY];
        if (p === null) {
            return undefined;
        }
        F[REVOCABLE_PROXY] = null;
        Internal.RevokeProxy(p);
    };
    return F;
}

Internal.ObjectDefineProperty(Proxy, "revocable", {
    configurable: true, enumerable: false, writable: true,
    value: function revocable(target, handler) {
        var p = new Proxy(target, handler);
        var revoker = CreateProxyRevokerFunction();
        revoker[REVOCABLE_PROXY] = p;
        return {proxy: p, revoke: revoker};
    }
});

})();
