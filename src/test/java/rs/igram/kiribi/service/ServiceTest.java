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
import java.security.PrivateKey;
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

import rs.igram.kiribi.crypto.*;
import rs.igram.kiribi.io.*;
import rs.igram.kiribi.net.*;
import rs.igram.kiribi.net.natt.*;
import rs.igram.kiribi.service.util.*;

/**
 * 
 *
 * @author Michael Sargent
 */
class ServiceTest {
//	static final Key KEY1 = Key.generate();
//	static final Key KEY2 = Key.generate();
	static final PrivateKey KEY1 = Key.generateKeyPair().getPrivate();
	static final PrivateKey KEY2 = Key.generateKeyPair().getPrivate();
	
	static final int PORT1 = 7700;
	static final int PORT2 = 7701;
	static final int PORT3 = 7702;
	
	static final InetAddress LOCAL_HOST;
	static {
		try {
			LOCAL_HOST = InetAddress.getByName("127.0.0.1");
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	SocketAddress SA1 = new InetSocketAddress(LOCAL_HOST, NATTServer.SERVER_PORT);
	SocketAddress SA2 = new InetSocketAddress(LOCAL_HOST, NATTServer.SERVER_PORT);
	SocketAddress SA3 = new InetSocketAddress(LOCAL_HOST, PORT3);
	
	static final ServiceId ID = ServiceId.parse(1l);
	static final byte CODE = 0x01;
	
	static final String BOB = "Bob";
	static final String ALICE = "Alice";
	
	NetworkExecutor executor;	
	NATTServer server;
   	   	
	ServiceAdmin admin1;
	ServiceAdmin admin2;
	
	EntityManager mgr1;
	EntityManager mgr2;
	
	Entity bob;
	Entity alice;
	
	void setup() throws Exception {
		System.out.println("INET: " + LOCAL_HOST);
		executor = new NetworkExecutor();
		NetworkMonitor.monitor(executor);
		server = new NATTServer();
		server.start(LOCAL_HOST, NATTServer.SERVER_PORT);
		admin1 = new ServiceAdmin(KEY1, PORT1, SA1);
		admin2 = new ServiceAdmin(KEY2, PORT2, SA2);
		
		mgr1 = admin1.entityManager(new ArrayList<Entity>());
		mgr2 = admin2.entityManager(new ArrayList<Entity>());
	}
	
	void shutdown() throws Exception {
		admin1.shutdown();
		admin2.shutdown();
		server.shutdown();
	}
	
	void configureEntities(Scope scope) throws Exception {
		CountDownLatch latch = new CountDownLatch(2);
		bob = new Entity(true, address(KEY2).toString(), BOB);
		alice = new Entity(true, address(KEY1).toString(), ALICE);
		
		ServiceAddress address = admin1.address(ID); 
		TestService service = new TestService(address, scope);
		admin1.activate(service);
		
		Set<Descriptor> granted = Set.of(service.getDescriptor());
		bob.setGranted(granted);
		
		mgr1.setOnExchange(e -> latch.countDown());
		mgr1.add(bob);
		Thread.sleep(3000);
		
		mgr2.setOnExchange(e -> latch.countDown());
		mgr2.add(alice);
		Thread.sleep(3000);
		
		latch.await();
	}
	
	static Address address(PrivateKey key) {
		Key.Private privateKey = (Key.Private)key;
		Key.Public publicKey = (Key.Public)(privateKey.generateKeyPair().getPublic());
		return publicKey.address();
	}
	
	static class Listener implements CompletionListener<Entity> {
		CountDownLatch latch;
		
		Listener() {}
		
		Listener(CountDownLatch latch) {
			this.latch = latch;
		}
		
		@Override
		public void completed(Entity entity) {
			System.out.println("Completed: " + entity);
			if (latch != null) latch.countDown();
		}
		
		@Override
		public void failed(Throwable t) {
			t.printStackTrace();
			if (latch != null) latch.countDown();
		}
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
			long a = request.in().readLong();
			long b = request.in().readLong();
			long result = a + b;
			System.out.println("PROCESSED REQUEST: " + result);
			Message response = request.respond(CODE);
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
			final CompletableFuture<Long> future = new CompletableFuture<Long>();
			Message request = Message.request(CODE);
			request.out().writeLong(a);
			request.out().writeLong(b);
			request(
				request, 
				new ResponseAdapter(
					CODE, 
					response -> {
						long result = response.in().readLong();
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