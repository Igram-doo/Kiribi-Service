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
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

import rs.igram.kiribi.io.Encodable;
import rs.igram.kiribi.io.VarInput;
import rs.igram.kiribi.io.VarInputStream;
import rs.igram.kiribi.io.VarOutput;
import rs.igram.kiribi.net.NetworkMonitor;

/**
 * An instance of this class represents a host address.
 *
 * @author Michael Sargent
 */
public class HostAddress implements Encodable {
	private final byte[] inet;
	private final int port;
    
	/**
	 * Instantiates a new <code>HostAddress</code> from the provided socket address.
	 *
	 * @param addr The socket address of this host address.
	 */	
	public HostAddress(InetSocketAddress addr) {
		this(addr.getAddress(), addr.getPort());
	}
    
	private HostAddress(InetAddress addr, int port) {
		inet = addr.getAddress();
		this.port = port;
	}
    
	private HostAddress(int port) throws UnknownHostException {
		inet = NetworkMonitor.inet().getAddress();
		this.port = port;
	}
    
	private HostAddress(VarInput in) throws IOException {
		inet = in.readBytes();
		port = in.readInt();
	}

	/**
	 * Instantiates a new <code>HostAddress</code> from the provided byte array.
	 *
	 * @param b The byte array to instantiate from.
	 * @throws IOException if there was a problem reading from the provided byte array.
	 */	
	public  HostAddress(byte[] b) throws IOException {
		this(new VarInputStream(b));
	}
	
	@Override
	public void write(VarOutput out) throws IOException {
		out.writeBytes(inet);
		out.writeInt(port);
	}

	/**
	 * Returns the socket address of this host address.
	 *
	 * @return The socket address of this host address.
	 * @throws IOException if there was a problem instantiating the socket address.
	 */	
	public InetSocketAddress address() throws IOException {
		return new InetSocketAddress(InetAddress.getByAddress(inet), port);
	}

	boolean inetEquals(InetAddress addr) {
		return addr == null ? false : Arrays.equals(addr.getAddress(), inet);
	}
	
	@Override
	public int hashCode() {return Arrays.hashCode(inet);}

	@Override
	public boolean equals(Object o) {
		if(this == o) return true;
		if(o != null && o.getClass() == HostAddress.class){
			HostAddress a = (HostAddress)o;
			return Arrays.equals(inet, a.inet) && port == a.port;
		}
		return false;
	}
	
	@Override
	public String toString() {
		try{
			return address().toString();
		}catch(IOException e){
			return "UnknownHost:"+port;
		}
	}
}
