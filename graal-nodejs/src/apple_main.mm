// Copyright Joyent, Inc. and other Node contributors.
//
// Permission is hereby granted, free of charge, to any person obtaining a
// copy of this software and associated documentation files (the
// "Software"), to deal in the Software without restriction, including
// without limitation the rights to use, copy, modify, merge, publish,
// distribute, sublicense, and/or sell copies of the Software, and to permit
// persons to whom the Software is furnished to do so, subject to the
// following conditions:
//
// The above copyright notice and this permission notice shall be included
// in all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
// OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
// MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
// NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
// DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
// OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE
// USE OR OTHER DEALINGS IN THE SOFTWARE.

#if defined(__APPLE__)
// Support Cocoa event loop on the main thread.
#include <Cocoa/Cocoa.h>
#include <objc/objc-runtime.h>
#include <objc/objc-auto.h>

static void dummyTimer(CFRunLoopTimerRef timer, void *info) {}

void ParkEventLoop() {
    // RunLoop needs at least one source, and 1e20 is pretty far into the future
    CFRunLoopTimerRef t = CFRunLoopTimerCreate(kCFAllocatorDefault, 1.0e20, 0.0, 0, 0, dummyTimer, NULL);
    CFRunLoopAddTimer(CFRunLoopGetCurrent(), t, kCFRunLoopDefaultMode);
    CFRelease(t);

    // Park this thread in the main run loop.
    int32_t result;
    do {
        result = CFRunLoopRunInMode(kCFRunLoopDefaultMode, 1.0e20, false);
    } while (result != kCFRunLoopRunFinished);
}
#endif
