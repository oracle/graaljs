const assert = require('assert');

const {
    Worker,
    isMainThread
} = require('worker_threads');

var worker = new Worker('java.lang.Thread.sleep(1000000)', {eval: true});
            worker.on('online', function () {
                setTimeout(function () {
                    worker.terminate();
                }, 1000);
            });

var moduleBlock = module { export var test = 5;};

worker.postMessage(moduleBlock.toString());
worker.postMessage(ModuleBlock.prototype.serialize(moduleBlock));