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
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.net.SocketException;
import java.security.KeyPair;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Logger;

import rs.igram.kiribi.net.Address;
import rs.igram.kiribi.crypto.EC25519PrivateKey;
import rs.igram.kiribi.crypto.EC25519PublicKey;
import rs.igram.kiribi.crypto.SignedData;
import rs.igram.kiribi.net.ConnectionAddress;
import rs.igram.kiribi.net.Endpoint;
import rs.igram.kiribi.net.EndpointProvider;
import rs.igram.kiribi.net.NetworkExecutor;
import rs.igram.kiribi.net.NetworkMonitor;
import rs.igram.kiribi.service.util.retry.RetryListener;
import rs.igram.kiribi.service.util.retry.RetryTask;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.logging.Level.*;
/**
 * Provides methods for service administration.
 *
 * @author Michael Sargent
 */
public final class ServiceAdmin {
	static final Logger LOGGER = Logger.getLogger(ServiceAdmin.class.getName());
	static final SecureRandom random;	
	
	final Address address;	
	private final EC25519PrivateKey privateKey;
	private final int serverPort;	
	private final Map<Address,InetSocketAddress> cache = new HashMap<>();
	private final SessionServer server;
	
	final SocketAddress nattServerAddress;
	final NetworkExecutor executor = new NetworkExecutor();
	final NetworkInterface networkInterface;
	final EndpointProvider endpointProvider;
	
	private EntityManager mgr;
	
	static {
		try{
			random = SecureRandom.getInstance("SHA1PRNG"); 
		}catch(Exception e){
			LOGGER.log(SEVERE, e.toString(), e);
			throw new RuntimeException("Could not initialize secure random",e);
		}
	}

	/**
	 * Initializes a newly created <code>ServiceAdmin</code> object
	 * with the given arguents.
	 *
	 * @param pair The key pair which will be associated with this service admin.
	 * @param serverPort The port to accept connections on.
	 * @param nattServerAddress The socket address of the NATT Server.
	 * @throws ClassCastException if the provided key is not an instance of rs.igram.kiribi.crypto.Key.Private
	 * @throws SocketException if there was a problem determining the default network interface
	 */
	public ServiceAdmin(KeyPair pair, int serverPort, SocketAddress nattServerAddress) throws SocketException { 
		this(NetworkMonitor.defaultNetworkInterface(), pair, serverPort, nattServerAddress);
	}

	/**
	 * Initializes a newly created <code>ServiceAdmin</code> object
	 * with the given arguents.
	 *
	 * @param networkInterface The <code>NetworkInterface</code> which will be associated with this service admin.
	 * @param pair The key pair which will be associated with this service admin.
	 * @param serverPort The port to accept connections on.
	 * @param nattServerAddress The socket address of the NATT Server.
	 * @throws ClassCastException if the provided key is not an instance of rs.igram.kiribi.crypto.Key.Private
	 */
	public ServiceAdmin(NetworkInterface networkInterface, KeyPair pair, int serverPort, SocketAddress nattServerAddress) { 
		this.networkInterface = networkInterface;
		this.privateKey = (EC25519PrivateKey)pair.getPrivate();
		this.serverPort = serverPort;
		this.nattServerAddress = nattServerAddress;
		this.address = new Address(pair.getPublic());
		
		System.out.println("Address: "+address);
		LOGGER.log(INFO, "Starting ServiceAdmin with Address {0}", address);
		
		endpointProvider = EndpointProvider.udpProvider(executor, address, nattServerAddress);
		server = new SessionServer(serverPort, this, endpointProvider);
		executor.onShutdown(1, this::shutdown);
	}

	/**
	 * Returns a random <code>long</code>.
	 *
	 * @return A random <code>long</code>.
	 */
	static long random() {
		return random.nextLong();
	}
	
	/**
	 * Returns the port of this service admin.
	 *
	 * @return The port of this service admin.
	 */	
	public int serverPort() {
		return serverPort;
	}
	
	/**
	 * Returns the entity manager of this service admin.
	 *
	 * @param entities The initial list of entities to manage. Can be empty. Will be ignored after initial call.
	 * @return The entity manager of this service admin.
	 */	
	public EntityManager entityManager(List<Entity> entities) {
		if (entities == null) entities = new ArrayList<Entity>();
		if (mgr == null) mgr = new EntityManager(entities, this);
		
		return mgr;
	}

	SessionServer server()  {
		return server;
	}

	// -------------- key stuff ------------------------------------------------	
	
	SignedData signData(byte[] data) throws IOException {
		return SignedData.signData(data, privateKey);
	}
	
	/**
	 * Returns a new service address with a random service id and this service admin's address.
	 *
	 * @return A new service address with a random service id and this service admin's address.
	 */	
	public ServiceAddress address() {
		return new ServiceAddress(new ServiceId(), address);
	}
	
	/**
	 * Returns a new service address with the given service id and this service admin's address.
	 *
	 * @param id The service id to use.
	 * @return A new service address with the given service id and this service admin's address.
	 */	
	public ServiceAddress address(ServiceId id) {
		return new ServiceAddress(id, address);
	}
	
	/**
	 * Returns the collection of active services.
	 *
	 * @return The collection of active services.
	 */		
	public Collection<Service> activeServices() {
		return server().activeServices();
	}
	// -------------- Activation ----------------------------------------------
	// initial batch activation
	/**
	 * Activates a service.
	 *
	 * @param service The service to activate.
	 */	
	public void activate(Service service) {
		server().put(service);
		LOGGER.log(FINE, "ServiceAdmin activated: {0}", service.getDescriptor());
	}
	
	/**
	 * Deactivates a service.
	 *
	 * @param id The id of the service to deactivate.
	 */	
	public void deactivate(ServiceId id) {
		server().remove(id);
		LOGGER.log(FINE, "ServiceAdmin deactivated: {0}", id);
	}

	// -------------- network connection  --------------------------------------
	Endpoint doConnect(Address address, ServiceId id) throws ServiceException {
		return doConnect(new ServiceAddress(id, address));
	}
	
	Endpoint doConnect(ServiceAddress address) throws ServiceException {
		try{
			return endpointProvider.open(new ConnectionAddress(address.host(), address.id().value()));
		}catch(Exception e){
			throw new ServiceException(e);
		}
	}
	
	// -------------- shutdown -------------------------------------------------
	/**
	 * Shuts down this service admin and all active services.
	 */	
	public void shutdown() {
		synchronized (this) {
			RetryTask.shutdown();
			if(server != null) server.shutdown();
			endpointProvider.shutdown();
			
			LOGGER.log(INFO, "Shutdown ServiceAdmin with Address {0}", address);
			System.out.println("ServiceAdmin: shutdown");
		}
	}	
}
