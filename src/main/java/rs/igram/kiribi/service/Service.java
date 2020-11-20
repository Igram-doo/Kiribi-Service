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
import java.util.function.Supplier;

/**
 * Interface all services must implement.
 *
 * @author Michael Sargent
 */
public interface Service {
	/**
	 * Returns the descriptor of the service.
	 *
	 * @return The descriptor of the service.
	 */	
	Descriptor getDescriptor();
	
	/**
	 * Returns the service id of the service.
	 *
	 * @return The service id of the service.
	 */	
	default ServiceId id() {return getAddress().id();}
	
	/**
	 * Returns the title of the service.
	 *
	 * @return The title of the service.
	 */	
	default String getTitle() {return getDescriptor().getDescription().getTitle();}
	
	/**
	 * Returns the description of the service.
	 *
	 * @return The description of the service.
	 */	
	default String getShortDescription() {return getDescriptor().getDescription().getDescription();}
	
	/**
	 * Returns the image byte array of the service.
	 *
	 * @return The image byte array of the service.
	 */	
	default byte[] getImage() {return getDescriptor().getDescription().getImage();}
	
	/**
	 * Returns the service address of the service.
	 *
	 * @return The service address of the service.
	 */	
	default ServiceAddress getAddress() {return getDescriptor().getAddress();}
	
	/**
	 * Returns the type of the service.
	 *
	 * @return The type of the service.
	 */	
	default int getType() {return getDescriptor().getType();}
	
	/**
	 * Returns the scope of the service.
	 *
	 * @return The scope of the service.
	 */	
	default Scope getScope() {return getDescriptor().getScope();}
	
	/**
	 * Returns a new service session.
	 *
	 * @return A new service session.
	 */	
	default Session newSession() {return null;}
}