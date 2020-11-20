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
 * Abstract super class of a service class.
 *
 * @author Michael Sargent
 */
// server side of a descriptor service
public abstract class AbstractService implements Service, Encodable {
	/** The descriptor of service. */
	protected Descriptor descriptor;
	// extensions for fututre releases
	private byte[] ext = new byte[0];

	/**
	 * Constructor.
	 *
	 * @param address The service address of the new service.
	 * @param scope The scope of the new service.
	 * @param description The description of the new service.
	 */	
	protected AbstractService(ServiceAddress address, Scope scope, Descriptor.Description description) {
		this(address, 0, scope, "", description);
	}

	/**
	 * Constructor.
	 *
	 * @param address The service address of the new service.
	 * @param type The type of the new service.
	 * @param scope The scope of the new service.
	 * @param tags The tags of the new service.
	 * @param description The description of the new service.
	 */	
	protected AbstractService(ServiceAddress address, int type, Scope scope, String tags, Descriptor.Description description) {
		descriptor = new Descriptor(address, type, scope, tags, description);
	}

	/**
	 * Constructor.
	 *
	 * @param in The input stream to initialize from.
	 * @throws IOException if there was a probem reading from the input stream.
	 */	
	protected AbstractService(VarInput in) throws IOException {
		descriptor = new Descriptor(in);
		// extensions
		ext = in.readBytes();
	}

	@Override
	public void write(VarOutput out) throws IOException {
		descriptor.write(out);
		// extensions
		out.writeBytes(ext);
	}

	@Override
	public Descriptor getDescriptor() {return descriptor; }

	/**
	 * Sets the description of the service.
	 *
	 * @param value The description of the service.
	 */
	public void setDescription(Descriptor.Description value) {
		descriptor.setDescription(value); 
	}

	@Override
	public int hashCode() {return descriptor.hashCode();}
}