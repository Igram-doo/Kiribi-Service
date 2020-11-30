/* 
 * MIT License
 * 
 * Copyright (c) 2020 Igram, d.o.o.
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
 
package rs.igram.kiribi.service;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import rs.igram.kiribi.io.*;

/**
 * 
 *
 * @author Michael Sargent
 */
public class ServiceAdminTest extends ServiceTest {
/*
   @Test
   public void testActivate() throws IOException, InterruptedException, Exception {
   	   // todo
   	   assertTrue(true);
   }
   */
/*
   @Test
   public void testPublicConnect() throws IOException, InterruptedException, Exception {
   	   setup();
   	   configureEntities(Scope.PUBLIC);
   	   ServiceAddress address = admin1.address(ID);
   	   TestClientSession client = new TestClientSession(Scope.PUBLIC, address);
   	   
   	   client.connect(admin2);
   	   long a = 1;
   	   long b = 1;
   	   
   	   long result = client.add(a, b, 5);
   	   assertEquals(2, result);
   	   
   	   shutdown();
   }
*/
   @Test
   public void testRestrictedConnect() throws IOException, InterruptedException, Exception {
   	   setup();

   	   configureEntities(Scope.RESTRICTED);
   	   
   	   ServiceAddress address = admin1.address(ID);
   	   TestClientSession client = new TestClientSession(Scope.RESTRICTED, address);
   	   
   	   client.connect(admin2);
   	   long a = 1;
   	   long b = 1;
   	   
   	   long result = client.add(a, b, 5);
   	   assertEquals(2, result);
   	   
   	   shutdown();
   	   
   }
   
/*
   @Test
   public void testShutdown() throws IOException, InterruptedException {
   	   // todo
   	   assertTrue(true);
   }
   */
}