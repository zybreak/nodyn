/*
 * Copyright 2015 lanceball.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.nodyn.runtime.nashorn;

import io.nodyn.NodeProcess;
import io.nodyn.runtime.NodynConfig;
import io.nodyn.runtime.Program;
import io.vertx.core.Vertx;
import org.junit.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

/**
 *
 * @author lanceball
 */
public class NashornRuntimeTest {
    private Vertx vertx;
    private NodynConfig config;
    private NashornRuntime runtime;

    public NashornRuntimeTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
        vertx = Vertx.vertx();
        config = new NodynConfig();
        runtime = new NashornRuntime(config, vertx, false);
    }
    
    @After
    public void tearDown() {
        vertx.close();
    }

    /**
     * Test of loadBinding method, of class NashornRuntime.
     */
    @Test
    public void testLoadBinding() {
//        Object result = runtime.loadBinding("v8");
//        assertEquals(true, result instanceof JSObject);
//        
//        // the v8 module has a function let's see if we can access it
//        JSObject exports = (JSObject) result;
//        JSObject f = (JSObject) exports.getMember("getHeapStatistics");
//        assertEquals(true, f != null);
//        assertEquals("Function", f.getClassName());
    }

    @Test
    public void testBuffer() throws Throwable {
        runtime.initialize();
        Program p = runtime.compile("var b1 = new Buffer('hello world'),"
              + "    b2 = new Buffer(64);"
              + "b1.copy(b2);"
              + "print(b2);"
              + "b2;", "testBuffer.js", true);
        p.execute(runtime.getGlobalContext());
    }
    
    /**
     * Test of compile method, of class NashornRuntime.
     */
    @Test
    public void testCompile() throws Exception, Throwable {
        String source = "var foo = 'bar'; foo";
        boolean displayErrors = false;
        Program program = runtime.compile(source, null, displayErrors);
        assertNotEquals(null, program);
        Object result = program.execute(runtime.getGlobalContext());
        assertEquals("bar", result);
    }

    /**
     * Test of makeContext method, of class NashornRuntime.
     */
	@Ignore
    @Test
    public void testMakeContext() {
        System.out.println("makeContext");
        Object global = null;
        runtime.makeContext(global);
        // TODO review the generated test code and remove the default call to fail.
        //fail("The test case is a prototype.");
    }

    /**
     * Test of isContext method, of class NashornRuntime.
     */
	@Ignore
    @Test
    public void testIsContext() {
        System.out.println("isContext");
        //boolean result = runtime.isContext(global);
//        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        //fail("The test case is a prototype.");
    }

    /**
     * Test of handleThrowable method, of class NashornRuntime.
     */
    @Test
    public void testHandleThrowable() {
        System.out.println("handleThrowable");
        Throwable t = new Exception("A test exception - this should appear in build output.");
        runtime.handleThrowable(t);
        // TODO review the generated test code and remove the default call to fail.
        //fail("The test case is a prototype.");
    }

    /**
     * Test of initialize method, of class NashornRuntime.
     */
    @Test
    public void testInitialize() {
        System.out.println("initialize");
        NodeProcess expResult = null;
        NodeProcess result = runtime.initialize();
//        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        //fail("The test case is a prototype.");
    }

    /**
     * Test of runScript method, of class NashornRuntime.
     */
    @Test
	@Ignore
    public void testRunScript() {
        System.out.println("runScript");
        String script = "";
        Object expResult = null;
        Object result = runtime.runScript(script);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        //fail("The test case is a prototype.");
    }

    /**
     * Test of getGlobalContext method, of class NashornRuntime.
     */
    @Test
    public void testGetGlobalContext() {
        System.out.println("getGlobalContext");
        Object result = runtime.getGlobalContext();
        assertNotNull(result);
    }
    
}
