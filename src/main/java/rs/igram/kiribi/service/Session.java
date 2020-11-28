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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import rs.igram.kiribi.crypto.Address;
import rs.igram.kiribi.net.Endpoint;
import rs.igram.kiribi.net.NetworkExecutor;

import static java.util.concurrent.TimeUnit.SECONDS;

import static rs.igram.kiribi.service.Message.*;

/**
 * An instance of this class represents a session.
 *
 * @author Michael Sargent
 */
public class Session {
	private final Map<Byte, RequestHandler> handlers = new HashMap<>();
	
	/** The authentication factory associated with this session. */	
	Supplier<Authenticator> authenticatorFactory;
	
	/** The service id associated with this session. */	
	protected final ServiceId id;
	
	/** The service address associated with this session. */	
	protected final ServiceAddress address;
	
	/** Indicates if this session was instantiated as a service session. */	
	protected final boolean isServiceSession;
	
	/** The transponder associated with this session. */		
	Transponder transponder;
	
	/** The service admin associated with this session. */
	ServiceAdmin admin;
	
	/** The scope associated with this session. */
	Scope scope;
	
	private boolean configured;
	
	// service 
	/**
	 * Initializes a newly created service <code>Session</code> object
	 * with the given service.
	 *
	 * @param service The service associated with this session.
	 */
	protected Session(Service service) {
		this.scope = service.getScope();
		this.id = service.id();
		this.address = null;
		
		isServiceSession = true;
	}
	
	// proxy
	/**
	 * Initializes a newly created client <code>Session</code> object
	 * with the given scope and service address.
	 *
	 * @param scope The service scope associated with this session.
	 * @param address The service address associated with this session.
	 */
	protected Session(Scope scope, ServiceAddress address) {
		this.scope = scope;
		this.address = address;
		this.id = address.id();
		
		isServiceSession = false;
	}

	/**
	 * Submit a request for processing.
	 *
	 * @param <T> The generic type of the request.
	 * @param timeout The timeout of the request.
	 * @param request The request.
	 * @return The result of the request.
	 * @throws ServiceException if there was a probem during the request.
	 */
	protected <T> T request(long timeout, Request<T> request) throws ServiceException {
		if(!isOpen() && isServiceSession) throw new IllegalStateException("Not a proxy session");
		synchronized(this){
			if(!isOpen()) connect(admin);
		}
		
		CompletableFuture<T> future = new CompletableFuture<>();
		try{
			request.request(future);
			return future.get(timeout, SECONDS);
		}catch(Exception e){
			throw new ServiceException(e);
		}
	}
	
	/**
	 * Submit a submission for processing.
	 *
	 * @param timeout The timeout of the submission.
	 * @param submission The submission.
	 * @throws ServiceException if there was a probem during the request.
	 */
	protected void submit(long timeout, Submission submission) throws ServiceException {
		if(!isOpen() && isServiceSession) throw new IllegalStateException("Not a proxy seesion");
		synchronized(this){
			if(!isOpen()) connect(admin);
		}
		
		CompletableFuture<Void> future = new CompletableFuture<>();
		try{
			submission.submit(future);
			future.get(timeout, SECONDS);
		}catch(Exception e){
			throw new ServiceException(e);
		}
	}
	
	// called immediately after initial connection - override to set handlers
	/** Called immediately after initial connection - override to set response handlers. */	
	protected void configure() {}
		
	/**
	 * Connects this session.
	 *
	 * @param admin The service admin associated with this session.
	 * @throws ServiceException if there was a probem connection this session.
	 */
	public void connect(ServiceAdmin admin) throws ServiceException {
		if(admin == null) throw new IllegalStateException("ServiceAdmin is null!");
		this.admin = admin;
		if(isServiceSession || isOpen()) return;
		if(!configured) {
			//this.admin = admin;
			authenticatorFactory = Authenticator.factory(scope, address, admin.entityManager(null));
			configure();
		}
		try{
			Endpoint endpoint = admin.doConnect(address == null ? null : address.host(), id);
			Transponder transponder = new Transponder(admin.executor, admin.server().transponders);
			transponder.connectProxy(endpoint, this);
		}catch(Exception e){
			e.printStackTrace();
			throw new ServiceException(e);
		}
	}
	
	// called by transponder
	final void connected(Transponder value) {
		transponder = value;
		if(!configured){
			configure();
			configured = true;
		}
		
		NetworkExecutor executor = (admin == null) ?
			value.executor : 					// service session
			admin.executor;						// client session
		executor.submit(this::onConnected);
	}
	
	// notifications - override as nedded
	/** Override as needed. */
	protected void onConnected() {}
	
	/** 
	 * Override as needed. 
	 *
	 * @param e The exception to be notified of
	 */
	protected void closed(Exception e) {}
	
	/** 
	 * Override as needed. 
	 *
	 * @param e The exception to be notified of
	 */
	protected void authenticationFailed(Exception e) {}
	
	/** 
	 * Override as needed. 
	 *
	 * @param e The exception to be notified of
	 */
	protected void connectionFailed(Exception e) {}
	
	/**
	 * Submit a request message for processing.
	 *
	 * @param request The request message.
	 * @throws IOException if there was a probem during the request.
	 */
	protected final void request(Message request) throws IOException {
		transponder.request(request);
	}	
			
	/**
	 * Submit a request message for processing.
	 *
	 * @param request The request message.
	 * @param listeners An array of response listeners each with a different code.
	 * @throws IOException if there was a probem during the request.
	 */
	protected final void request(Message request, ResponseListener... listeners) throws IOException {
		// should have notified session earlier, eg, auth failed, but handle here just in case
		if(transponder == null) throw new IOException("Not connected");
		transponder.request(request, listeners);
	}
					
	/**
	 * Add a request handler to handle requests with the given message code.
	 *
	 * @param code The message code to handle.
	 * @param handler A request handler.
	 */
	protected final void handle(byte code, RequestHandler handler) {
		handlers.put(code, handler);
	}
		
	// called by transponder
	final RequestHandler handler(byte code) {return handlers.get(code);}
	
	/** 
	 * Returns the entity associated with this session. 
	 *
	 * @return The entity associated with this session. 
	 */
	public Entity entity() {return transponder == null ? null : transponder.entity();}
		
	/** 
	 * Returns <code>true</code> if this session is open, <code>false</code> otherwise. 
	 *
	 * @return <code>true</code> if this session is open, <code>false</code> otherwise. 
	 */
	public boolean isOpen() {return transponder != null && transponder.isOpen();}
		
	/** Closes this session. */
	public void close() {
		if(transponder != null) transponder.close();
		transponder = null;
	}	
		
	@Override
	public int hashCode() {
		int p = 31;
		int r = 1;
		r = p * r + id.hashCode();
		r = p * r + ((address == null) ? 0 : address.hashCode());
		return r;
	}
	
	@Override
	public boolean equals(Object o){
		if(this == o) return true;
		if(o != null && getClass().equals(o.getClass())){
			Session s = (Session)o;
			return id.equals(s.id)
			       && authenticatorFactory.equals(s.authenticatorFactory)
			       && handlers.equals(s.handlers)
			       && ((address == null && s.address == null) || address.equals(s.address))
			       && ((transponder == null && s.transponder == null) || transponder.equals(s.transponder))
			       && configured == s.configured;
		}
		return false;
	}
	
	/**
	 * Functional interface for requests.
	 *
	 * @author Michael Sargent
 	 */			
	@FunctionalInterface
	protected static interface Request<T> {
		/**
		 * Submit a request for processing.
		 *
		 * @param future The request future.
		 * @throws Exception if there was a probem during the request.
		 */
		void request(CompletableFuture<T> future) throws Exception;
	}
	
	/**
	 * Functional interface for submissions.
	 *
	 * @author Michael Sargent
 	 */			
	@FunctionalInterface
	protected static interface Submission {
		/**
		 * Submit a submission for processing.
		 *
		 * @param future The submission future.
		 * @throws Exception if there was a probem during the submission.
		 */
		 void submit(CompletableFuture<Void> future) throws Exception;
	}
	
	/**
	 * Concrete implementation of a <code>ResponseListener</code>.
	 *
	 * @author Michael Sargent
	 */
	protected static class ResponseAdapter implements ResponseListener {
		private final Byte code;
		private final ResponseHandler responseHandler;
		private final ErrorHandler errorHandler;

		/**
		 * Initializes a newly created <code>ResponseAdapter</code> object
		 * with the given code and response handler.
		 *
		 * @param code The message code to process.
		 * @param responseHandler The reponse handler used to process the response.
		 */
		public ResponseAdapter(Byte code, ResponseHandler responseHandler){
			this(code, responseHandler, null);
		}

		/**
		 * Initializes a newly created <code>ResponseAdapter</code> object
		 * with the given code, response handler and error handler.
		 *
		 * @param code The message code to process.
		 * @param responseHandler The reponse handler used to process the response.
		 * @param errorHandler The reponse handler used to process the response error.
		 */
		public ResponseAdapter(Byte code, ResponseHandler responseHandler, ErrorHandler errorHandler){
			this.code = code;
			this.responseHandler = responseHandler;
			this.errorHandler = errorHandler;
		}

		@Override
		public void response(Message response){
			try{
				byte c = response.code();
				byte s = response.status();
				if(((code != null && c == code) || (code == null && s == OK))){
					if(responseHandler != null) responseHandler.apply(response);
				}else if(s == ERROR && errorHandler != null){
					String msg = response.in().readUTF();
					errorHandler.error("Remote Exception: "+msg);
				}else if(errorHandler != null ){
					errorHandler.error("Unexpected Response: "+code+" "+response.code());
				}
			}catch(Throwable t){
		//		t.printStackTrace();
				if(errorHandler != null) errorHandler.error("Problem: "+t.getMessage());
			}
		}
		
		@Override
		public byte code() {
			return code;
		}
	}
}
