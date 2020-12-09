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
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Logger;

import rs.igram.kiribi.net.Endpoint;
import rs.igram.kiribi.net.EndpointProvider;
import rs.igram.kiribi.net.NetworkExecutor;
import rs.igram.kiribi.net.NetworkMonitor;
import rs.igram.kiribi.net.Endpoint;
import rs.igram.kiribi.net.ServerEndpoint;

import static java.util.logging.Level.*;

/**
 * Manages sessions.
 *
 * @author Michael Sargent
 */
// manage incoming connections
final class SessionServer {
	static final Logger LOGGER = Logger.getLogger(SessionServer.class.getName());
	private final EndpointProvider endpointProvider;
		
	private final Map<Endpoint,Transponder> endpoints = Collections.synchronizedMap(new HashMap<>());
	private final Map<ServiceId, Service> serviceMap = Collections.synchronizedMap(new HashMap<>());
	
	private NetworkExecutor executor;
	private ServiceAdmin admin;
	
	final Set<Transponder> transponders = Collections.synchronizedSet(new HashSet<>());
	
	protected ServerEndpoint endpoint;
	protected int port;
	protected boolean autoStart = true;
	protected boolean starting, started;

	public SessionServer(int port, ServiceAdmin admin, EndpointProvider endpointProvider) {
		this.port = port;
		this.admin = admin;
		this.endpointProvider = endpointProvider;
		
		executor = admin.executor;
		
		if(autoStart){
			NetworkMonitor.onAvailable(() -> {	
				try{
					activate();
				}catch(Exception e){
					// todo
					LOGGER.log(SEVERE, e.toString(), e);
				}	
			});
		}
	}
	
	public int getPort() {return port;}
	
	public void setPort(int value) {
		port = value;
		try{
			deactivate();
			activate();
		}catch(Exception e){
			// todo
			LOGGER.log(SEVERE, e.toString(), e);
		}
	}
	
	public boolean isOpen() {
		return endpoint != null && endpoint.isOpen();
	}
	
	private void activate() throws InterruptedException, IOException, TimeoutException {
		if(!NetworkMonitor.isUp()) return;
		if(endpoint != null && endpoint.isOpen()) return;
//		if(sessionFactories.isEmpty()) return;
		if(starting || started) return;
		starting = true;
		try{
			endpoint = endpointProvider.open(port);
			listen();
			started = true;
		}finally{
			starting = false;
		}
		LOGGER.log(INFO, "Session server started on port {0} with Address {1}", new Object[]{port, admin.address});
	}
	
	public void shutdown() {
		try{
			deactivate();
		}catch(IOException e){
			// ignore
		}
	}
	
	public void deactivate() throws IOException {
		transponders.forEach(t -> t.terminate());
		transponders.clear();
		if(endpoint == null) return;
		endpoint.close();
		started = false;
		endpoint = null;
		endpointProvider.shutdown();
		LOGGER.log(INFO, "Shut down SessionServer with Address {0}", admin.address);
	}
	
	private void listen() throws IOException {
		endpoint.accept(e -> {
			try{
				LOGGER.log(FINEST, "SessionServer.listen:  {0}", e);
				accept(e);
			}catch(Exception ex){
				try{
					e.close();
				}catch(IOException ex2){
					// ignore
				}	
			}
		});
	}

	protected Transponder accept(Endpoint e) throws IOException {
		Transponder t = endpoints.get(e);
		if(t == null){
			t = new Transponder(executor, transponders);
			t.connectServer(e, serviceMap, admin);
			endpoints.put(e,t);
		}
		
		return t;
	}	
		
	// -------------------------------------------------------------------------
	
	public void put(Service service) {
		ServiceId id = service.id(); 
		serviceMap.put(id, service);
		if(autoStart){
			try{
				activate();
			}catch(Exception e){
				// todo
				LOGGER.log(SEVERE, e.toString(), e);
			}
		}
	}
	
	public void remove(ServiceId id) {
		serviceMap.remove(id);
		if(autoStart && serviceMap.isEmpty()){
			shutdown();
		}
	}
}