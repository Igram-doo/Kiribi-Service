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
import rs.igram.kiribi.io.VarOutputStream;

/**
 * An instance of this class represents a data message between peers.
 *
 * @author Michael Sargent
 */
public final class Message implements Encodable {
	/** Indicates the status is ok. */	
	public static final byte OK			= 0;
	/** Indicates the status is error. */	
	public static final byte ERROR		= 1;
	private static final byte REQUEST	= 0;
	private static final byte RESPONSE	= 1;
	
	private byte type;
	private byte status;
	private byte code;
	/** The uid of the message. */	
	public final long uid;
	
	private VarInputStream in;
	private VarOutputStream out = new VarOutputStream();
	
	private Message(byte code){
		this.code = code;
		type = REQUEST;
		status = OK;
		uid = ServiceAdmin.random();
	}
	
	/**
	 * Initializes a newly created <code>Message</code> object
	 * with the given input stream.
	 *
	 * @param in The input stream to initialize from.
	 * @throws IOException if there was a probem reading from the input stream.
	 */
	public Message(VarInput in) throws IOException {
		type = in.readByte();
		uid = in.readLong();
		status = in.readByte();
		code = in.readByte();
		var b = in.readBytes();
		this.in = new VarInputStream(b);
	}

	/**
	 * Generates a new request <code>Message</code> object
	 * with the given code.
	 *
	 * @param code The code of the message.
	 * @return A request message with the given code.
	 */
	public static Message request(byte code) {return new Message(code);}
	
	byte type() {return type;}
	
	byte status() {return status;}
	
	/**
	 * Returns the code of the message.
	 *
	 * @return The the code of the message.
	 */
	public byte code() {return code;}
	
	/**
	 * Returns a <code>VarInput</code> to read data from the message.
	 *
	 * @return A <code>VarInput</code> to read data from the message.
	 */
	public VarInput in() {return in;}
		
	/**
	 * Returns a <code>VarOutput</code> to write data to the message.
	 *
	 * @return A <code>VarOutput</code> to write data to the message.
	 */
	public VarOutput out() {return out;}

	/**
	 * Generates a new response <code>Message</code> object
	 * with the given code.
	 *
	 * @param code The code of the message.
	 * @return A response message with the given code.
	 */
	public Message respond(byte code) {
		this.code = code;
		type = RESPONSE;
		in = null;
		return this;
	}
	
	/**
	 * Generates a new error <code>Message</code> object
	 * with the given message.
	 *
	 * @param msg The error string of the message.
	 * @return An error message with the given error string.
	 */
	public Message error(String msg) {
		code = 0;
		type = RESPONSE;
		status = ERROR;
		try{
			out.writeUTF(msg);
		}catch(IOException e){}
		in = null;
		return this;
	}
	
	@Override
	public void write(VarOutput out) throws IOException {
		out.writeByte(type);
		out.writeLong(uid);
		out.writeByte(status);
		out.writeByte(code);
		var b = this.out.toByteArray();
		out.writeBytes(b);
		out = null;
	}

	@Override
	public String toString() {return "Message:"+type+" "+code;}
}
