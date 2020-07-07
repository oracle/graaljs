'use strict';

const {
  customInspectSymbol: kInspect,
} = require('internal/util');

const { format } = require('util');
const { Map, Symbol } = primordials;

const {
  ERR_INVALID_ARG_TYPE,
  ERR_INVALID_ARG_VALUE,
} = require('internal/errors').codes;

const kDestroy = Symbol('kDestroy');
const kHandle = Symbol('kHandle');

// Histograms are created internally by Node.js and used to
// record various metrics. This Histogram class provides a
// generally read-only view of the internal histogram.
class Histogram {

  constructor(internal) {
    this._handle = internal;
    this._map = new Map();
  }

  [kInspect]() {
    const obj = {
      min: this.min,
      max: this.max,
      mean: this.mean,
      exceeds: this.exceeds,
      stddev: this.stddev,
      percentiles: this.percentiles,
    };
    return `Histogram ${format(obj)}`;
  }

  get min() {
    return this._handle ? this._handle.min() : undefined;
  }

  get max() {
    return this._handle ? this._handle.max() : undefined;
  }

  get mean() {
    return this._handle ? this._handle.mean() : undefined;
  }

  get exceeds() {
    return this._handle ? this._handle.exceeds() : undefined;
  }

  get stddev() {
    return this._handle ? this._handle.stddev() : undefined;
  }

  percentile(percentile) {
    if (typeof percentile !== 'number')
      throw new ERR_INVALID_ARG_TYPE('percentile', 'number', percentile);

    if (percentile <= 0 || percentile > 100)
      throw new ERR_INVALID_ARG_VALUE.RangeError('percentile', percentile);

    return this._handle ? this._handle.percentile(percentile) : undefined;
  }

  get percentiles() {
    this._map.clear();
    if (this._handle)
      this._handle.percentiles(this._map);
    return this._map;
  }

  reset() {
    if (this._handle)
      this._handle.reset();
  }

  [kDestroy]() {
    this._handle = undefined;
  }

  get [kHandle]() { return this._handle; }
}

module.exports = {
  Histogram,
  kDestroy,
  kHandle,
};
