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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Function;

import rs.igram.kiribi.net.Address;
import rs.igram.kiribi.net.ConnectionState;
import rs.igram.kiribi.net.Endpoint;
import rs.igram.kiribi.net.NetworkExecutor;

import static rs.igram.kiribi.service.Message.*;

/**
 * An instance of this class represents a UDP connection address.
 *
 * @author Michael Sargent
 */
final class Transponder implements Consumer<ConnectionState> {
	private static final byte REQUEST = 0;
	private static final byte RESPONSE = 1;
	private Set<Transponder> transponders;

	private final Map<Long, ResponseListener[]> activeRequests = new HashMap<Long, ResponseListener[]>();
	
	Endpoint endpoint;
	Authenticator authenticator;
	Session session;
	NetworkExecutor executor;
	
	private Future<?> reader;

	Transponder(NetworkExecutor executor, Set<Transponder> transponders) {
		this.executor = executor;
		this.transponders = transponders;
	}
	 
	private Authenticator authenticator(Scope scope, ServiceAddress address, ServiceAdmin admin) {
		return Authenticator.factory(scope, address, admin.entityManager(null)).get();
	}
	
	private void connect(Endpoint endpoint, boolean isProxy) throws IOException {
		this.endpoint = endpoint;
		
		// state 
		endpoint.state(this);
		
		// manage for shutdown
		transponders.add(this);
	}

	void connectProxy(Endpoint endpoint, Session session) throws IOException {
		connect(endpoint, true);
		
		this.session = session;
		endpoint.write(session.id);
		authenticator = session.authenticatorFactory.get();
		if(!authenticator.authenticate(true, endpoint)) {
			session.authenticationFailed(new IOException("Authentication Failed"));
			close();
			return;
		}
		// notify listener
		session.connected(this);
		
		// start message reader
		reader = executor.submit(this::read);		
	}

	void connectServer(Endpoint endpoint, Map<ServiceId, Service> serviceMap, 
		ServiceAdmin admin) throws IOException {
	
		connect(endpoint, false);
		ServiceId id = endpoint.read(ServiceId::new);
		
		Service service = serviceMap.get(id);
		if(service == null) throw new IOException("Unknown service: "+id);
		session = service.newSession();
		if(session == null) throw new IOException("Unknown session: "+id);
		
		authenticator = authenticator(service.getScope(), service.getAddress(), admin);
		
		if(!authenticator.authenticate(false, endpoint)) {
			session.authenticationFailed(new IOException("Authentication Failed"));
			close();
			return;
		}
		
		session.connected(this);
		
		// start message reader
		reader = executor.submit(this::read);		
	}
	
	// connection state
	@Override
	public void accept(ConnectionState state) {
		switch(state) {
		case CLOSED:
			notify(new IOException("Endpoint closed"));
			break;
		}
	}
	
	// --- transponder methods ---
	Entity entity() {return authenticator.entity();}

	boolean isOpen() {return endpoint == null ? false : endpoint.isOpen();}
		
	void request(Message request, ResponseListener... l) throws IOException {
		if(l != null) activeRequests.put(request.uid, l);
		request(request);
	}
	
	void request(Message request) throws IOException {
		endpoint.write(request);
	}
	
	Message respond(Message request) {
		RequestHandler handler = session.handler(request.code());
		if(handler == null) return request.error("Unknown request: "+request.code());
		try {
			return handler.respond(request);
		} catch(IOException e) {
			return request.error("Remote error: "+e.getMessage());
		}
	}

	private void processIncomingRequest(Message request) throws IOException {
		Message response = respond(request);
		// handle null response
		if(response != null) endpoint.write(response);
	}

	private void processIncomingResponse(Message response){
		ResponseListener l = filter(response.code(), activeRequests.remove(response.uid));
		if(l != null) {
			executor.submit(() -> l.response(response));
		}
	}

	private static ResponseListener filter(byte code, ResponseListener[] listeners) {
		if (listeners == null) return null;
		for (ResponseListener l : listeners) {
			if (l.code() == code) return l;
		}
		
		return null;
	}
	
	private void read() {
		while(!Thread.currentThread().isInterrupted() && endpoint.isOpen()) {
			try {
				Message msg = endpoint.read(Message::new);
				byte type = msg.type();
				boolean valid = type == REQUEST ? true : type == RESPONSE ? activeRequests.containsKey(msg.uid) : false;
				if(!valid) continue;
				switch(type){
				case REQUEST:
					processIncomingRequest(msg);
					break;
				case RESPONSE:
					processIncomingResponse(msg);
					break;
				}
			} catch(IOException e) {	
				notify(e);
				return;
			}
		}
	}

	private void notify(Exception e){
		close();
		if(session != null) session.closed(e);
		session = null;
	}
	
	 void close() {
	 	 if(!isOpen());
		terminate();
		if (transponders != null ) transponders.remove(this);
	}

	void terminate() {
		// called by close all - can't remove from transponders since we're 
		// iterating over them
		if(reader != null) reader.cancel(true);
		if(endpoint != null && endpoint.isOpen()){
			try {
				endpoint.close();
			} catch(IOException e) {
				// ignore
			}
		}
		endpoint = null;
		executor = null;
		transponders = null;
	}
}
