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
import rs.igram.kiribi.io.VarInputStream;
import rs.igram.kiribi.io.VarOutput;

/**
 * An instance of this class represents the client side of a descriptor service.
 *
 * @author Michael Sargent
 */
// client side of a descriptor service
class DescriptorProxy implements Service, Encodable {	
	/** The descriptor of the proxy. */
	protected Descriptor descriptor;
	
	/**
	 * instantiates a new <code>DescriptorProxy</code> object.
	 *
	 * @param descriptor The descriptor of the new service.
	 */	
	public DescriptorProxy(Descriptor descriptor) {
		this.descriptor = descriptor;
	}

	/**
	 * instantiates a new <code>DescriptorProxy</code> object.
	 *
	 * @param in The input stream to initialize from.
	 * @throws IOException if there was a probem reading from the input stream.
	 */	
	public DescriptorProxy(VarInput in) throws IOException {
		descriptor = new Descriptor(in);
	}

	/**
	 * instantiates a new <code>DescriptorProxy</code> object.
	 *
	 * @param b The byte array to initialize from.
	 * @throws IOException if there was a probem reading from the byte array.
	 */	
	protected DescriptorProxy(byte[] b) throws IOException {
		this(new VarInputStream(b));
	}

	@Override
	public Descriptor getDescriptor() {return descriptor; }
	
	/**
	 * Sets the descriptor of the proxy.
	 *
	 * @param value The descriptor of the proxy.
	 */
	public void setDescriptor(Descriptor value) {descriptor = value; }

	@Override
	public void write(VarOutput out) throws IOException {
		descriptor.write(out);
	}

	@Override
	public boolean equals(Object o){
		if(o != null && o instanceof DescriptorProxy){
			return descriptor.equals(((DescriptorProxy)o).descriptor);
		}
		return false;
	}
	
	@Override
	public Session newSession() {return null;}
}