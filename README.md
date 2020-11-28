# Kiribi-Service
Kiribi Peer-to-Peer Service Framework

### Introduction
Provides classes and interfaces to support secure peer-to-peer networking.

### Features

##### Network
Each peer only requires a single open upd port on the host device. NAT transversal and peer-to-peer network communication is provided by the [Kiribi-Net](http://github.com/Igram-doo/Kiribi-Net) module.

##### Security
Encryption and authentication between peers is provided by the [Kiribi-Crypto](http://github.com/Igram-doo/Kiribi-Crypto) module.

### Overview
Provides classes and interfaces to support secure peer-to-peer networking.

##### Addresses
To do

##### Services
To do

##### Scope
To do

##### Sessions
To do

##### Messages
To do

##### Entities
To do

##### Service Administration
To do

##### Entity Management
To do

### Code Example
Protocol

	class Protocol {
		static final byte ADD = 0x01;	// request code
		static final byte ADDED = 0x01;	// response code
	}

Service Session

	public class AddServiceSession extends Session {
		public AddServiceSession(Service service) {
			super(service);
		}
		
		protected void configure() {
			handle(Protocol.ADD, this::add);
		}
		
		// ---- responses ----
		Message add(Message request) throws IOException {
			long a = request.in().readLong();
			long b = request.in().readLong();
			long result = a + b;
			Message response = request.respond(Protocol.ADDED);
			response.out().writeLong(result);
		
			return response;
		}
   	}

Client Session
   
	public class AddClientSession extends Session {
		public AddClientSession(Scope scope, ServiceAddress address) {
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
			Message request = Message.request(Protocol.ADD);
			request.out().writeLong(a);
			request.out().writeLong(b);
			request(
				request, 
				new ResponseAdapter(
					Protocol.ADDED, 
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

Service
		
	public class AddService extends AbstractService {
		public AddService(ServiceAddress address, Scope scope) {
			super(address, scope, new  Descriptor.Description("Add"));
		}
			
		@Override
		public Session newSession() {
			return new AddServiceSession(this);
		}
	}

### Module Dependencies
##### Requires
* rs.igram.kiribi.io
* rs.igram.kiribi.crypto
* rs.igram.kiribi.net

##### Exports
* rs.igram.kiribi.service

### Requirements
To do

### Known Issues
To do
