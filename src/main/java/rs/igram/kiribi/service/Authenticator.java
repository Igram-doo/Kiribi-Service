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
import java.util.Optional;
import java.util.function.Supplier;

import rs.igram.kiribi.io.EncodedStream;
import rs.igram.kiribi.crypto.Address;
import rs.igram.kiribi.crypto.Challenge;
import rs.igram.kiribi.crypto.SignedData;

/**
 * An instance of this class Authenticate services.
 *
 * @author Michael Sargent
 */
abstract class Authenticator {
	Entity entity;
	
	private Authenticator() {}
	
	final Entity entity() {return entity;}
	
	abstract boolean authenticate(boolean isProxy, EncodedStream stream);

	/**
	 * Retruns an authenticator factory.
	 *
	 * @param descriptor The descriptor of the service to be authenticated.
	 * @param mgr The entity manager associated with the service.
	 * @return An authenticator factory.
	 */
	static Supplier<Authenticator> factory(Descriptor descriptor, EntityManager mgr) {
		return descriptor.getScope() == Scope.RESTRICTED ? 
			() -> new RestrictedAuthenticator(descriptor.getAddress(), mgr) :
			() -> new PublicAuthenticator(descriptor.getAddress(), mgr);
	}
	
	static Supplier<Authenticator> factory(Scope scope, ServiceAddress address, EntityManager mgr) {
		return scope == Scope.RESTRICTED ? 
			() -> new RestrictedAuthenticator(address, mgr) :
			() -> new PublicAuthenticator(address, mgr);
	}
	
	private static boolean authenticateServer(ServiceAddress address, boolean isProxy, EncodedStream stream, EntityManager mgr) throws IOException {
		if(isProxy){
			Challenge challenge = new Challenge();
			stream.write(challenge);
			SignedData data = stream.read(SignedData::new);
			return challenge.verify(data, address.host());
		}else{
			Challenge challenge = stream.read(Challenge::new);
			stream.write(mgr.admin.signData(challenge.encode()));
			return true;
		}
	}

	// public services
	static final class PublicAuthenticator extends Authenticator {
		private final ServiceAddress address;
		private EntityManager mgr;
		
		public PublicAuthenticator(ServiceAddress address, EntityManager mgr) {
			this.address = address;
			this.mgr = mgr;
		}
		
		public static Supplier<Authenticator> factory(ServiceAddress address, EntityManager mgr) {
			return () -> new PublicAuthenticator(address, mgr);
		}
		
		@Override
		public boolean authenticate(boolean isProxy, EncodedStream stream) {
			try{
				return authenticateServer(address, isProxy, stream, mgr);
			}catch(IOException e){
				return false;
			}
		}
	}
		
	// restricted services
	static final class RestrictedAuthenticator extends Authenticator {
		private ServiceAddress address;
		private EntityManager mgr;
		
		public RestrictedAuthenticator(ServiceAddress address, EntityManager mgr) {
			this.address = address;
			this.mgr = mgr;
		}
		
		private boolean authenticate(Address addr) {
			try{
				Optional<Entity> optional = mgr.authenticate(addr, address.id());
				if(optional.isPresent()){
					entity = optional.get();
					return true;
				}
				return false;
			}catch(IOException e){
				return false;
			}
		}
		
		@Override
		public boolean authenticate(boolean isProxy, EncodedStream stream) {
			try{
				if(!authenticateServer(address, isProxy, stream, mgr)) return false;
				if(isProxy){
					// make sure the entity for the given address is in the entity manager
					Optional<Entity> optional = mgr.entity(address.host());
					if(optional.isPresent()){
						Challenge challenge = stream.read(Challenge::new);
						stream.write(mgr.admin.signData(challenge.encode()));					
						entity = optional.get();
						return true;
					}
					return false;
				}else{
					Challenge challenge = new Challenge();
					stream.write(challenge);					
					SignedData data = stream.read(SignedData::new);
					if(!challenge.verify(data, data.address())) return false;
					return authenticate(data.address());
				}
			}catch(IOException e){
				return false;
			}
		}
		
		public static Supplier<Authenticator> factory(ServiceAddress address, EntityManager mgr) {
			return () -> new RestrictedAuthenticator(address, mgr);
		}
	}
}
