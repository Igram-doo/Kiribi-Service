# Kiribi-Service
Kiribi Peer-to-Peer Service Framework

### Introduction
Provides classes and interfaces for building a secure service based peer-to-peer network.

### Features
* Each peer only requires a single open upd port on the host device. NAT transversal and peer-to-peer network communication is provided by the [Kiribi-Net](http://github.com/Igram-doo/Kiribi-Net) module.
* Encryption and authentication between peers is provided by the [Kiribi-Crypto](http://github.com/Igram-doo/Kiribi-Crypto) module.

### Overview
Provides classes and interfaces for building a secure service based peer-to-peer network.

##### Addresses
Each peer maintains a unique *Address*. Addresses are instantiated with a crypto-graphic public key to ensure they are unique.

##### ServiceAddresses
Each *Service* maintains a unique *ServiceAddress* consisting of the peer's Address and a unique long value.

##### Services
Each *Service* provides descriptive information about the service and a factory method for creating server sessions to handle incoming requests.

##### Scope
Services have one of three possible scopes:

* **Private**	
Local only.
* **Restricted**  
Require aunthentication and to have been explicitly granted access.
* **Public**  
Require aunthentication.

##### Sessions
Sessions define which *Requests* and *Responses* a service supports. Sessions can be instantiated in one of two modes:

* **Client Session**  
  Connects to the *ServiceAddress* of a remote service.
* **Server Session**  
  Accepts connections from *Client Sessions*.

##### Messages
*Messages* encapsulate data sent and received by *Sessions*.

##### Service Administration
Provides methods to manage services.

##### Entities
Entities encapsulate which services a remote peer has made available to us as well as which services we have made available to that peer.

##### Entity Management
Provides methods to manage entities. Includes a mechanism to publish available services to remote peers.

### Code Examples
Protocol

	class AddProtocol {
		static final byte ADD = 0x01;	// request code
		static final byte ADDED = 0x01;	// response code
	}

Service Session

	public class AddServiceSession extends Session {
		public AddServiceSession(Service service) {
			super(service);
		}
		
		protected void configure() {
			handle(AddProtocol.ADD, this::add);
		}
		
		// ---- responses ----
		Message add(Message request) throws IOException {
			VarInput in = request.in();
			long a = in.readLong();
			long b = in.readLong();
			long result = a + b;
			Message response = request.respond(AddProtocol.ADDED);
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
			Message request = Message.request(AddProtocol.ADD);
			VarOutput out = request.out();
			out.writeLong(a);
			out.writeLong(b);
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
* java.base
* rs.igram.kiribi.io
* rs.igram.kiribi.crypto
* rs.igram.kiribi.net

##### Exports
* rs.igram.kiribi.service

### To Do
* Determine minimum supported Java version.

### Known Issues
* Shutdown does work properly. Probably due to open exchange session...
