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

import rs.igram.kiribi.io.Encodable;
import rs.igram.kiribi.io.VarInput;
import rs.igram.kiribi.io.VarOutput;

/**
 * An instance of this class represents a ServiceID.
 *
 * @author Michael Sargent
 */
public class ServiceId implements Encodable {
	// null - used in ServiceAddress.NULL
	static final ServiceId NULL   = new ServiceId(0l);
	// fixed
	static final ServiceId ENTITY = new ServiceId(-1l);
	
	private final long data;
	    
	private ServiceId(long b) {
		data = b;
	}
	    
	/**
	 * Parses a service id from the provided string.
	 *
	 * @param value The string used to generate the service id.
	 * @return A service id from the provided string.
	 */	
	public static ServiceId parse(String value){
		return new ServiceId(Long.parseLong(value));
	}
	
	/**
	 * Generates a service id from the provided <code>long</code>.
	 *
	 * @param value A positive <code>long</code> used to generate the service id.
	 * @return A service id from the provided <code>long</code>.
	 * @throws IllegalArgumentException if value is not positivie.
	 */	
	public static ServiceId parse(long value){
		if (value < 1) throw new IllegalArgumentException("value must be positive");
		return new ServiceId(value);
	}
	
	/**
	 * Instantiates a new <code>HostAddress</code> with a random value.
	 */	
	public ServiceId() {
		data = ServiceAdmin.random();
	}
       
	/**
	 * Instantiates a new <code>HostAddress</code> from the provided input stream.
	 *
	 * @param in The input stream to instantiate from.
	 * @throws IOException if there was a problem reading from the provided input stream.
	 */	
	public ServiceId(VarInput in) throws IOException {
		data = in.readLong();
	}
		
	/**
	 * Returns the value of this service id.
	 *
	 * @return addr The value of this service id.
	 */	
	public long value() {return data;}
	
	@Override
	public void write(VarOutput out) throws IOException {
		out.writeLong(data);
	}

	@Override
	public int hashCode() {
		return (int)data;
	}

	@Override
	public boolean equals(Object o) {
		if(this == o) return true;
		if(o != null && o.getClass() == ServiceId.class){
			var a = (ServiceId)o;
			return data == a.data;
		}
		return false;
	}
	
	@Override
	public String toString() {return ""+data;}
}
