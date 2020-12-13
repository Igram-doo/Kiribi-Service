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
import java.net.NoRouteToHostException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * An instance of this class represents a service exception.
 *
 * @author Michael Sargent
 */
public class ServiceException extends Exception {
	/** Enumeration of service exception types. */	
	public static enum Type{
		/** Indicates an execution problem. */	
		EXECUTION,
		/** Indicates the task was interrupted. */	
		INTERRUPTED,
		/** Indicates the remote address was not registered. */	
		UNREGISTERED,
		/** Indicates an IO problem. */	
		IO,
		/** Indicates the task timed out. */	
		TIMEOUT
	}
	
	/** The type of the service exception. */	
	protected final Type type;
	
	/**
	 * Instantiates an new <code>ServiceException</code>.
	 *
	 * @param t The cause of the service exception.
	 */	
	public ServiceException(Throwable t){
		if(t instanceof RuntimeException) throw (RuntimeException)t;
		
		if(t instanceof ExecutionException){
			type = Type.EXECUTION;
			initCause(t);
		}else if(t instanceof InterruptedException){
			type = Type.INTERRUPTED;
			initCause(t);
		}else if(t instanceof NoRouteToHostException){
			type = Type.UNREGISTERED;
			initCause(t);
		}else if(t instanceof IOException){
			type = Type.IO;
			initCause(t);
		}else if(t instanceof TimeoutException){
			type = Type.TIMEOUT;
			initCause(t);
		}else if(t instanceof ServiceException){
			ServiceException se = (ServiceException)t;
			type = se.type;
			initCause(se.getCause());
		}else{
			throw new IllegalArgumentException(t);
		}
	}
	
	    
	/**
	 * Returns the type of the service exception.
	 *
	 * @return The type of the service exception.
	 */	
	public Type getType() {return type;}
}