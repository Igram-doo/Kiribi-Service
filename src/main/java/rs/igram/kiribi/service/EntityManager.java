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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Supplier;

import rs.igram.kiribi.service.util.ConsumerSupport;
import rs.igram.kiribi.crypto.Address;
import rs.igram.kiribi.crypto.Key;
import rs.igram.kiribi.crypto.Signature;
import rs.igram.kiribi.crypto.SignedData;
import rs.igram.kiribi.io.VarInput;
import rs.igram.kiribi.io.VarOutput;
import rs.igram.kiribi.net.Endpoint;
import rs.igram.kiribi.net.NetworkExecutor;
import rs.igram.kiribi.net.ServerEndpoint;
import rs.igram.kiribi.service.util.CompletionListener;

import static java.util.concurrent.TimeUnit.SECONDS;

import rs.igram.kiribi.service.Session;

/**
 * An instance of this class manages entities.
 *
 * @author Michael Sargent
 */
public final class EntityManager {
	private	static final byte CLIENT_REQUEST_DATA_EXCHANGE = 100;
	private static final byte SERVICE_RESPONSE_DATA_EXCHANGE = 100;
	
	private final Map<Address, ExchangeSession> sessions = 
		Collections.synchronizedMap(new HashMap<Address, ExchangeSession>());
	private List<Entity> entities;
	private Consumer<Entity> onExchange;
	private boolean shutdown;

	ServiceAdmin admin;
	
	/**
	 * Initializes a newly created <code>EntityManager</code> object
	 * with the given arguents.
	 *
	 * @param entities The list of entities to manage. Can be empty.
	 * @param admin The service admin which will be associated with this entity manager.
	 */
	EntityManager(List<Entity> entities, ServiceAdmin admin) {
		this.entities = entities;
		this.admin = admin;
		
		ExchangeService service = new ExchangeService(admin.address(ServiceId.ENTITY));
		admin.activate(service);
	}
	
	/**
	 * Set a listener to be notified of exchange events.
	 *
	 * @param value listener to be notified of exchange events.
	 */	
	public void setOnExchange(Consumer<Entity> value) {onExchange = value;}
	
	/**
	 * Returns the list of managed entities.
	 *
	 * @return The list of managed entities.
	 */	
	public synchronized List<Entity> entities() {
		return entities;
	}

	/**
	 * Adds an entity.
	 *
	 * @param entity The entity to be added.
	 * @throws IOException if there was a problem adding the entity.
	 */	
	public void add(Entity entity) throws IOException {
		entities.add(entity);
		exchange(entity);
	}

	/**
	 * Updates an entity.
	 *
	 * @param entity The entity to be updated.
	 * @throws IOException if there was a problem updating the entity.
	 */	
	public void update(Entity entity) throws IOException {
		update(entity, true);
	}
	
	private void update(Entity entity, boolean exchange) throws IOException {
		if(exchange) exchange(entity);
	}

	/**
	 * Exchanges an entity.
	 *
	 * @param entity The entity to be exchanged.
	 */	
	public void exchange(Entity entity) {
		admin.executor.submit(() -> {
			try{
				ServiceAddress address = new ServiceAddress(ServiceId.ENTITY, entity.address());
				ExchangeSession session = sessions.get(entity.address());
				if(session == null) {
					session = session(null, entity);
					session.connect(admin);
				}
				session.exchange(entity, 15);
			}catch(Exception e){
				e.printStackTrace();
				// peer could be offline - just eat exception
			}
		});		
	}
	
	private ExchangeSession session(CompletionListener<Entity> handler, Entity entity) {
		ServiceAddress address = new ServiceAddress(ServiceId.ENTITY, entity.address());
		return new ExchangeSession(
					handler, 
					address
				);
	}
	
	/**
	 * Deletes an entity.
	 *
	 * @param entity The entity to be deleted.
	 */	
	public void delete(Entity entity) {
		entities.remove(entity);
	}

	// update the entity database by deleting all binding to the given servie
	/**
	 * Notifies this entity manager that the service with the given service address has been deleted.
	 *
	 * @param isProxy The isProxy flag of the services.
	 * @param address The service address of the service.
	 */	
	public final void deleted(boolean isProxy, ServiceAddress address) {
		entities().forEach(e -> {
			e.deleted(isProxy, address);
		});
	}

	/**
	 * Returns an optional containing the entity associated with the given address if it exists.
	 *
	 * @param address The address to check.
	 * @return An optional containing the entity associated with the given address if it exists.
	 */	
	public final Optional<Entity> entity(Address address) {
		if(address == null) return Optional.empty();
		return entities()
			.stream()
			.filter(e -> e.address() != null && address.equals(e.address()))
			.findAny();
	}

	final Optional<Entity> authenticate(Address address, ServiceId id) 
		throws IOException {
			
		// fetch entity associated with the address	
		Optional<Entity> optional = entity(address);
		if(optional.isPresent()){
			Entity entity = optional.get();
			// check if the entity was granted access to the service
			// note that it doesn't make sense to grant access to the EntityService,
			// so if that is the service to be accessed just continue
			if(ServiceId.ENTITY.equals(id) || entity.granted(id)){
				return Optional.of(entity);
			}
		}
		return Optional.empty();
	}

	// ------------------ Exchange ---------------------------------------------
	private class ExchangeSession extends Session {
		Entity entity;
		CompletionListener<Entity> handler;
		boolean isProxy;
		
		// server
		ExchangeSession(Service service) {
			super(service);
		}
		
		// client
		ExchangeSession(CompletionListener<Entity> handler, ServiceAddress address) {
			super(Scope.RESTRICTED, address);
			this.handler = handler;
		}

		@Override
		protected void configure() {
			handle(CLIENT_REQUEST_DATA_EXCHANGE, this::exchange);
		}
	
		@Override
		protected void onConnected() {
			if(entity != null){
				assert(entity().equals(entity));
			}else{
				entity = entity();
			}
			sessions.put(entity().address(), this);
		}
		
		@Override
		protected void authenticationFailed(Exception e) {
			if(handler != null) handler.failed(e);
		}
		
		@Override
		protected void connectionFailed(Exception e) {
			if(handler != null) handler.failed(e);
		}
		
		@Override
		protected void closed(Exception e) {
			sessions.remove(entity().address());
		}
		
		private void update(Entity entity, boolean notify) {
			EntityManager.this.admin.executor.submit(() -> {
				entity.setPending(false);
				try{
					EntityManager.this.update(entity, false);
					if(onExchange != null) onExchange.accept(notify ? entity : null);
				}catch(IOException e){
					// todo
					e.printStackTrace();
				}
			});
		}
		
		// ---- requests ----
		void exchange(Entity entity, long timeout) throws ServiceException {
			submit(timeout, f -> exchange(entity, f));
		}
		
		private void exchange(Entity entity, CompletableFuture<Void> future) throws IOException {
			final Message request = Message.request(CLIENT_REQUEST_DATA_EXCHANGE);
			VarOutput out = request.out();

			Entity.ExchangeData data = entity.exchange();
			out.write(data);

			request(request, 
				new ResponseAdapter(
					SERVICE_RESPONSE_DATA_EXCHANGE, 
					response -> {
						try{
						System.out.println("EntityManager.proxy.exchange SUCCESS1: ");
						VarInput in = response.in();
						if(in.readBoolean()){
							Entity.ExchangeData d = new Entity.ExchangeData(in);
							entity.exchange(d);
							update(entity, true);
							//System.out.println("ER1: " + entity + " " + entity.imported);
						}else{
							update(entity, true);
							//System.out.println("ER2: " + entity.imported);
						}	

						future.complete(null);
						System.out.println("EntityManager.proxy.exchange SUCCESS2: ");
						}catch(Exception e){
							e.printStackTrace();
						}
					},
					error -> {
						System.out.println("EntityManager.proxy.exchange FAILED: ");
						future.completeExceptionally(error == null ? new IOException("Unkown error") : new IOException(error));
					}
				)
			);
		}		
		
		// ---- responses ----
		Message exchange(Message request) throws IOException {
			Entity entity = entity();
			VarInput in = request.in();

			Entity.ExchangeData d = new Entity.ExchangeData(in);
			entity.exchange(d);
			
			Message response = request.respond(SERVICE_RESPONSE_DATA_EXCHANGE);
			VarOutput out = response.out();
			if(entity.isPending()){
				out.writeBoolean(true);
				Entity.ExchangeData data = entity.exchange();
				out.write(data);
			}else{
				out.writeBoolean(false);
			}
			update(entity, true);
			
			if(handler != null) handler.completed(entity);
				
			return response;
		}
	}	
				
	class ExchangeService extends AbstractService {
		ServiceAdmin admin;
			
		ExchangeService(ServiceAddress address) {
			super(address, 1, Scope.RESTRICTED, "" , new Descriptor.Description());
		}
			
		@Override
		public Session newSession() {
			return new ExchangeSession(this);
		}
	}
}
