/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

function Natural() {
    x = 2;
    return {
        'next' : function() { return x++; }
    };
}

function Filter(number, filter) {
    var self = this;
    this.number = number;
    this.filter = filter;
    this.accept = function(n) {
      var filter = self;
      for (;;) {
          if (n % filter.number === 0) {
              return false;
          }
          filter = filter.filter;
          if (filter === null) {
              break;
          }
      }
      return true;
    };
    return this;
}

function Primes(natural) {
    var self = this;
    this.natural = natural;
    this.filter = null;

    this.next = function() {
        for (;;) {
            var n = self.natural.next();
            if (self.filter === null || self.filter.accept(n)) {
                self.filter = new Filter(n, self.filter);
                return n;
            }
        }
    };
}

const N = 1000;
const expected = 7927;

function primesMain() {
    var primes = new Primes(Natural());
    var primArray = [];
    for (var i=0;i<=N;i++) { primArray.push(primes.next()); }

    if (primArray[N] !== expected) {
        throw new Error("wrong result: "+primArray[N]+" expected: "+expected);
    }
}

primesMain();

