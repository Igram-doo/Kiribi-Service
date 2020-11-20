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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import rs.igram.kiribi.crypto.Address;
import rs.igram.kiribi.io.Encodable;
import rs.igram.kiribi.io.VarInput; 
import rs.igram.kiribi.io.VarInputStream; 
import rs.igram.kiribi.io.VarOutput;
import rs.igram.kiribi.service.util.ConsumerSupport;

/**
 * An instance of this class represents an Entity.
 *
 * @author Michael Sargent
 */
public final class Entity implements Comparable<Entity>, Cloneable, Encodable {
	private static final int SERIAL_VERSION = 0;
	
	private final ConsumerSupport<String> nameSupport = new ConsumerSupport<>();
	private final ConsumerSupport<Boolean> pendingSupport = new ConsumerSupport<>();
	
	private Address address;
	Services exported;
	Services imported;
	private String name = "";
	private boolean pending = true;
	// extensions for future releases
	private byte[] ext = new byte[0];

	/**
	 * Initializes a newly created <code>Entity</code> object.
	 */
	public Entity(){
		exported = new Services();
		imported = new Services();
	}

	/**
	 * Initializes a newly created <code>Entity</code> object
	 * with the given arguents.
	 *
	 * @param pending The pending flag.
	 * @param addr The address of the entity.
	 * @param name The name of the entity.
	 */
	public Entity(boolean pending, String addr, String name) {
		this();

		address = new Address(addr);
		this.name = name;
		this.pending = pending;
	}
	
	/**
	 * Initializes a newly created <code>Entity</code> object
	 * with the given arguents.
	 *
	 * @param pending The pending flag.
	 * @param addr The address of the entity.
	 * @param name The name of the entity.
	 * @param ids The service ids of the services to export.
	 * @param descriptors The service descriptors of the services to export.
	 */
	Entity(boolean pending, String addr, String name, Collection<ServiceId> ids, 
		Collection<Descriptor> descriptors) {
	
		this(pending, addr, name);

		exported = new Services(0, ids, descriptors);
	}

	/**
	 * Initializes a newly created <code>Entity</code> object
	 * with the given arguents.
	 *
	 * @param pending The pending flag.
	 * @param entity The entity to merge with.
	 * @param addr The address of the entity.
	 * @param name The name of the entity.
	 * @param ids The service ids of the services of the services to export.
	 * @param descriptors The service descriptors of the services to export.
	 */
	Entity(boolean pending, Entity entity, String addr, String name, 
		Collection<ServiceId> ids, Collection<Descriptor> descriptors) {
	
		this(pending, addr, name);
		
		imported = entity.imported;
		exported = new Services(0, ids, descriptors);
	}

	/**
	 * Initializes a newly created <code>Entity</code> object
	 * with the given input stream.
	 *
	 * @param in The input stream to initialize from.
	 * @throws IOException if there was a problem reading from the input stream.
	 */
	public Entity(VarInput in) throws IOException {
		int serialVersion = in.readUnsignedByte();
		address = in.read(Address::new);
		name = in.readUTF();
		pending = in.readBoolean();
		exported = in.read(Services::new);
		imported = in.read(Services::new);
		// extensions
		ext = in.readBytes();
	}
	
	/**
	 * Instantiates a new <code>Entity</code> from the provided byte array.
	 *
	 * @param b The byte array to instantiate from.
	 * @throws IOException if there was a problem reading from the provided byte array.
	 */	
	Entity(byte[] b) throws IOException {
		this(new VarInputStream(b));
	}

	/** 
	 * Reconfigures this entity with the provided data. 
	 *
	 * @param addr The new address of this entity.
	 * @param name The new name of this entity.
	 * @param ids The service ids of the services to grant to this entity.
	 * @param descriptors The service descriptors of the services to grant to this entity.
	 */
	public void set(String addr, String name, Collection<ServiceId> ids, Collection<Descriptor> descriptors) {
		address = new Address(addr);
		setName(name);
		setPending(true);
		exported = new Services(exported.version +1, ids, descriptors);
	}

	/** 
	 * Reconfigures this entity with the provided data. 
	 *
	 * @param descriptors The service descriptors of the services to grant to this entity.
	 */
	public void setGranted(Collection<Descriptor> descriptors) {
		setPending(true);
		exported = new Services(exported.version +1, new HashSet<ServiceId>(), descriptors);
	}
	
	/**
	 * Returns the address of this entity.
	 *
	 * @return The address of this entity.
	 */	
	public Address address() {return address;}
	
	/**
	 * Returns the id of this entity.
	 *
	 * @return The id of this entity.
	 */	
	public String id() {return address.toString();}
	
	/** 
	 * Returns <code>true</code> if this entity has been granted access to the service
	 * with the given service id, <code>false</code> otherwise. 
	 *
	 * @param id The service id of the service
	 * @return <code>true</code> if this entity has been granted access to the service
	 * with the given service id, <code>false</code> otherwise.  
	 */
	public boolean granted(ServiceId id) {return exported.contains(id);}
	
	/** 
	 * Returns <code>true</code> if this entity has made available the service
	 * with the given service id, <code>false</code> otherwise. 
	 *
	 * @param id The service id of the service.
	 * @return <code>true</code> if this entity has made available the service
	 * with the given service id, <code>false</code> otherwise.  
	 */
	public boolean available(ServiceId id) {
		return imported == null ?
			false :
			imported.contains(id);
	}
	
	/** 
	 * Returns an optional containing a descriptor if this entity has made available the service
	 * with the given service id, if not, the optional is empty. 
	 *
	 * @param id The service id of the service.
	 * @return An optional containing a descriptor if this entity has made available the service
	 * with the given service id, if not, the optional is empty.
	 */
	public Optional<Descriptor> availableContent(ServiceId id) {
		return imported == null ?
			Optional.empty() :
			imported.content(id);
	}
	
	/** 
	 * Returns the set of descriptors of services this entity has made available. 
	 *
	 * @return The set of descriptors of services this entity has made available
	 */
	public Set<Descriptor> availableContent()  {
		return imported == null ?
			new HashSet<Descriptor>() :
			imported.descriptors;
	}
	
	/**
	 * Returns the name of this entity.
	 *
	 * @return The name of this entity.
	 */	
	public String getName() {return name;}
		
	/** 
	 * Returns <code>true</code> if this entity is pending, <code>false</code> otherwise. 
	 *
	 * @return <code>true</code> if this session is pending, <code>false</code> otherwise. 
	 */
	public boolean isPending() {return pending;}

	/**
	 * Sets the name of this entity.
	 *
	 * @param value name of this entity.
	 */	
	public void setName(String value) {
		name = value;
		nameSupport.consume(name);
	}
	
	/**
	 * Sets the pending flag of this entity.
	 *
	 * @param value The pending flag of this entity.
	 */	
	public void setPending(boolean value) {
		pending = value;
		pendingSupport.consume(pending);
	}
		
	/**
	 * Adds a name change consumer to be notified of name changes of this entity.
	 *
	 * @param consumer The name change consumer.
	 */	
	public void addNameConsumer(Consumer<String> consumer) {nameSupport.addConsumer(consumer);}
		
	/**
	 * Removes a name change consumer to be notified of name changes of this entity.
	 *
	 * @param consumer The name change consumer.
	 */	
	public void removeNameConsumer(Consumer<String> consumer) {nameSupport.removeConsumer(consumer);}
	
	/**
	 * Adds a pending flag change consumer to be notified of pending flag changes of this entity.
	 *
	 * @param consumer The pending flag consumer.
	 */	
	public void addPendingConsumer(Consumer<Boolean> consumer) {pendingSupport.addConsumer(consumer);}
	
	/**
	 * Removes a pending flag change consumer to be notified of pending flag changes of this entity.
	 *
	 * @param consumer The pending flag consumer.
	 */	
	public void removePendingConsumer(Consumer<Boolean> consumer) {pendingSupport.removeConsumer(consumer);}

	void set(Entity e) {
		setName(e.getName());
		exported = e.exported;
	}

	@Override
	public void write(VarOutput out) throws IOException {
		out.writeByte(SERIAL_VERSION);
		out.write(address);
		out.writeUTF(name);
		out.writeBoolean(pending);
		out.write(exported);
		out.write(imported);
		// extensions
		out.writeBytes(ext);
	}

	// remove the given service address from the client and service sets
	/**
	 * Removes the given service address from the client and service sets.
	 *
	 * @param isProxy The proxy flag.
	 * @param address The service address to remove.
	 */	
	public void deleted(boolean isProxy, final ServiceAddress address){
		if(isProxy){
			imported.delete(address);
		}else{
			exported.delete(address);
		}
	}

	ExchangeData exchange() {
		return new ExchangeData(exported == null ? new Services() : exported);
	}
	
	void exchange(ExchangeData value) {
		imported = value.services;
	}

	@Override
	public int compareTo(Entity e) {
		return name.compareTo(e.name);
	}
	
	@Override
	public String toString() {return name;}

	@Override
	public int hashCode(){return address.hashCode();}

	@Override
	public boolean equals(Object o){
		if(this == o) return true;
		if(o != null && o.getClass() == Entity.class){
			final Entity e = (Entity)o;
			return address.equals(e.address)
				&& ((imported == null && e.imported == null) || imported.equals(e.imported))
				&& exported.equals(e.exported)
				&& pending == e.pending
				&& name.equals(e.name);
		}
		return false;
	}

	@Override
	public Entity clone() {
		try{
			return new Entity(encode());
		}catch(IOException e){
			throw new RuntimeException("Stream corrupted", e);
		}
	}

	// either services exported to or imported a remote bloblet
	private static class Services implements Encodable {
		private final long version;
		private final Set<ServiceId> ids = new HashSet<>();
		private final Set<Descriptor> descriptors = new HashSet<>();
		
		Services() {
			version = 0l;
		}
		
		Services(long version, Collection<ServiceId> ids, Collection<Descriptor> descriptors) {
			this.version = version;
			this.ids.addAll(ids);
			this.descriptors.addAll(descriptors);
		}
		
		Services(VarInput in) throws IOException {
			version = in.readLong();
			in.read(ids, ServiceId::new);
			in.read(descriptors, Descriptor::new);
		}

		@Override
		public void write(VarOutput out) throws IOException {
			out.writeLong(version);
			out.write(ids);
			out.write(descriptors);
		}
		
		boolean contains(ServiceId id) {
			if(ids.contains(id)) return true;
			return descriptors.stream().anyMatch(d -> d.getAddress().id().equals(id));
		}
		
		Optional<Descriptor> content(ServiceId id) {
			return descriptors.stream()
				.filter(d -> d.getAddress().id().equals(id))
				.findAny();
		}
		
		void delete(ServiceAddress address) {
			descriptors.removeIf(d -> d.getAddress().equals(address));
		}
		
		@Override
		public String toString() {return "version:"+version+"\n"+ids+"\n"+descriptors;}
	}
	
	// public key and services to be exported/imported
	static class ExchangeData implements Encodable {
		private final Services services;
		
		private ExchangeData(Services services) {
			this.services = services;
		}
		
		ExchangeData(VarInput in) throws IOException {
			services = new Services(in);
		}

		@Override
		public void write(VarOutput out) throws IOException {
			out.write(services);
		}
		
		@Override
		public String toString() {return services.toString();}
	}
}
