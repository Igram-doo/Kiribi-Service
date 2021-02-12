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
import java.net.SocketAddress;
import java.security.KeyPair;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.Supplier;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import rs.igram.kiribi.crypto.KeyPairGenerator;
import rs.igram.kiribi.io.*;
import rs.igram.kiribi.net.Address;
import rs.igram.kiribi.net.EndpointProvider;
import rs.igram.kiribi.net.NetworkExecutor;
import rs.igram.kiribi.net.NetworkMonitor;
import rs.igram.kiribi.net.natt.*;
import rs.igram.kiribi.service.util.*;

/**
 * 
 *
 * @author Michael Sargent
 */
class ServiceTest {
	static final ServiceId ID = ServiceId.parse(1l);
	static final byte CODE = 0x01;
	
	static final String BOB = "Bob";
	static final String ALICE = "Alice";
	
	Entity bob;
	Entity alice;
	
	Peer peer1;
	Peer peer2;
	
	NATTServer server;
	
	void setup(int offset, Peer.Type type, Scope scope) throws Exception {
		var port = 4000;
		
		var serverAddress = type == Peer.Type.LAN ?
			EndpointProvider.defaultGroup():
			new InetSocketAddress(NetworkMonitor.inet(), port + offset);
			
		server = new NATTServer();
		server.start(serverAddress);
		
		peer1 = new Peer(port + offset + 1, serverAddress, type);
		peer2 = new Peer(port + offset + 2, serverAddress, type);		
	}
	
	void shutdown() throws Exception {
		peer1.admin.shutdown();
		peer2.admin.shutdown();
		server.shutdown();
		
		Thread.sleep(500);
	}
	
	void configureEntities(Scope scope) throws Exception {
		var latch = new CountDownLatch(1);
		bob = new Entity(true, peer2.address.toString(), BOB);
		alice = new Entity(true, peer1.address.toString(), ALICE);
		
		var address = peer1.admin.address(ID); 
		var service = new TestService(address, scope);
		peer1.admin.activate(service);
		
		var granted = Set.of(service.getDescriptor());
		bob.setGranted(granted);
		
		peer1.mgr.setOnExchange(e -> latch.countDown());
		peer1.mgr.add(bob);
		Thread.sleep(3000);
		
		peer2.mgr.setOnExchange(e -> latch.countDown());
		peer2.mgr.add(alice);
		Thread.sleep(3000);
		
		latch.await();
	}
	
	static class TestServiceSession extends Session {
		TestServiceSession(Service service) {
			super(service);
		}
		
		protected void configure() {
			handle(CODE, this::add);
		}
		
		// ---- responses ----
		Message add(Message request) throws IOException {
			var a = request.in().readLong();
			var b = request.in().readLong();
			var result = a + b;
			//System.out.println("PROCESSED REQUEST: " + result);
			var response = request.respond(CODE);
			response.out().writeLong(result);
		
			return response;
		}
   	}
   
	static class TestClientSession extends Session {
		TestClientSession(Scope scope, ServiceAddress address) {
			super(scope, address);
		}
		
		// ---- requests ----
		public final long add(long a, long b, long timeout) throws ServiceException {
			try{
				return add(a, b).get(timeout, SECONDS);
			}catch(Exception e){
				throw new ServiceException(e);
			}
		}
		
		private Future<Long> add(long a, long b) throws IOException {
			final var future = new CompletableFuture<Long>();
			var request = Message.request(CODE);
			request.out().writeLong(a);
			request.out().writeLong(b);
			request(
				request, 
				new ResponseAdapter(
					CODE, 
					response -> {
						var result = response.in().readLong();
						future.complete(result);
					},
					error -> future.completeExceptionally(new IOException(error))
				)
			); 
		
			return future;
		}
   	}
		
	static class TestService extends AbstractService {
		TestService(ServiceAddress address, Scope scope) {
			super(address, scope, new  Descriptor.Description("Add"));
		}
			
		@Override
		public Session newSession() {
			return new TestServiceSession(this);
		}
	}
}