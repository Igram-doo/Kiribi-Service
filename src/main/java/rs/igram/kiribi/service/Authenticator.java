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
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;
import java.util.function.Supplier;

import rs.igram.kiribi.io.Encodable;
import rs.igram.kiribi.io.VarInput; 
import rs.igram.kiribi.io.VarOutput;
import rs.igram.kiribi.io.EncodedStream;
import rs.igram.kiribi.net.Address;
import rs.igram.kiribi.crypto.EC25519PublicKey;
import rs.igram.kiribi.crypto.Signature;
import rs.igram.kiribi.crypto.SignedData;

/**
 * An instance of this class Authenticate services.
 *
 * @author Michael Sargent
 */
abstract class Authenticator {
	static final SecureRandom random;

 	static {
		try{
			random = SecureRandom.getInstance("SHA1PRNG", "SUN"); 
		}catch(Exception e){
			throw new RuntimeException("Could not initialize secure random",e);
		}
	}
	
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
					Address addr = new Address(data.getPublicKey());
					if(!challenge.verify(data, addr)) return false;
					return authenticate(addr);
				}
			}catch(IOException e){
				return false;
			}
		}
		
		public static Supplier<Authenticator> factory(ServiceAddress address, EntityManager mgr) {
			return () -> new RestrictedAuthenticator(address, mgr);
		}
	}
	
	private static final class Challenge implements Encodable {
		static final int SIZE = 16;
		private final byte[] b = new byte[SIZE];
	
		Challenge() {
			random.nextBytes(b);
		}
	
		Challenge(byte[] bytes) {
			System.arraycopy(bytes, 0, b, 0, SIZE);
		}
		
		Challenge(VarInput in) throws IOException {
			in.readFully(b);
		}

		@Override
		public void write(VarOutput out) throws IOException {out.write(b);}
	
		boolean verify(SignedData data, Address address) {
			try{
				Address addr = new Address(data.getPublicKey());
				return data.verify(data.getPublicKey()) && address.equals(addr) && Arrays.equals(b, data.data());
			}catch(IOException e){
				return false;
			}
		}
	
		boolean verify(Signature sig, PublicKey key) {
			return sig.verify(b, key);
		}
	
		@Override
		public byte[] encode() throws IOException {
			byte[] encoded = new byte[SIZE];
			System.arraycopy(b, 0, encoded, 0, SIZE);
			return encoded;
		}
	
		@Override
		public String toString(){return Base64.getEncoder().encodeToString(b);}

		@Override
		public int hashCode(){return Arrays.hashCode(b);}

		@Override
		public boolean equals(Object o){
			if(this == o) return true;
			if(o == null || o.getClass() != Challenge.class) return false;
			return Arrays.equals(b, ((Challenge)o).b);
		}
	}
}
