// We need to prime vertx since jasmine tests don't run in the vert.x container.
if ((typeof NativeRequire) !== 'object') {
  __jvertx = org.vertx.java.core.VertxFactory.newVertx();
  load('./npm_modules.js');
  load('./node.js');
}

// jasmine's fake clock thinks it's using milliseconds,
// but in fact it's using seconds. Set the timeout increment
// to one second.
jasmine.WaitsForBlock.TIMEOUT_INCREMENT = 1;
jasmine.DEFAULT_TIMEOUT_INTERVAL = 1;

var fs = require('vertx/file_system');

var jasmineEnv = jasmine.getEnv();
var origCallback = jasmineEnv.currentRunner_.finishCallback;

jasmineEnv.currentRunner_.finishCallback = function() {
  origCallback.call(this);
  process.emit('exit', '__jvertx.stop()');
  __jvertx.stop();
};

describe('Nodyn', function() {
  it('should execute integration specs', function() {
    var complete = false;
    waitsFor(function() { return complete; }, 300); // bail after 5 minutes?

    fs.readDir('src/test/javascript', '.+Spec.js', function(err, arr) {
      for(var i in arr) {
        require(arr[i]);
      }
      complete = true;
    });
  });
});
