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

import rs.igram.kiribi.net.Address;
import rs.igram.kiribi.io.Encodable;
import rs.igram.kiribi.io.VarInput;
import rs.igram.kiribi.io.VarOutput;

/**
 * An instance of this class represents service address.
 *
 * @author Michael Sargent
 */
public class ServiceAddress implements Encodable {
	// null - used in Txn if no service address is associated with the transaction
	/** The null address. */
	public static final ServiceAddress NULL = new ServiceAddress(ServiceId.NULL, Address.NULL);

	private final ServiceId id;
	private final Address host;
	
	/**
	 * Instantiates an new <code>ServiceAddress</code>.
	 *
	 * @param value The string used to instantiate this service address.
	 */	
	public ServiceAddress(String value) {
		var s = value.split(":");
		id = ServiceId.parse(s[1]);
		host = new Address(s[0]);
	}
	
	/**
	 * Instantiates an new <code>ServiceAddress</code>.
	 *
	 * @param id The id used to instantiate this service address.
	 * @param host The host used to instantiate this service address.
	 */	
	public ServiceAddress(ServiceId id, Address host){
		this.id = id;
		this.host = host;
	}

	/**
	 * Instantiates an new <code>ServiceAddress</code>.
	 *
	 * @param in The input stream used to instantiate this service address.
	 * @throws IOException if the was a problem reading from the input stream.
	 */	
	public ServiceAddress(VarInput in) throws IOException {
		id = in.read(ServiceId::new);
		host = in.read(Address::new);
	}

	@Override
	public void write(VarOutput out) throws IOException {
		out.write(id);
		out.write(host);
	}

	/**
	 * Returns the address of the service.
	 *
	 * @return The address of the service.
	 */	
	public Address host() {return host;}

	/**
	 * Returns the id of the service.
	 *
	 * @return The id of the service.
	 */	
	public ServiceId id() {return id;}
	
	@Override
	public String toString() {return host+":"+id;}

	@Override
	public int hashCode() {
		var p = 31;
		var r = 1;
		r = p * r + id.hashCode();
		r = p * r + host.hashCode();
		return r;
	}

	@Override
	public boolean equals(Object o) {
		if(this == o) return true;
		if(o != null && o.getClass() == ServiceAddress.class){
			var a = (ServiceAddress)o;
			return id.equals(a.id) && host.equals(a.host);
		}
		return false;
	}
}
