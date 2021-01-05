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
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.security.KeyPair;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.Supplier;

import static java.util.concurrent.TimeUnit.SECONDS;

import rs.igram.kiribi.crypto.KeyPairGenerator;
import rs.igram.kiribi.io.*;
import rs.igram.kiribi.net.Address;
import rs.igram.kiribi.net.AddressMapper;
import rs.igram.kiribi.net.EndpointProvider;
import rs.igram.kiribi.net.NetworkExecutor;
import rs.igram.kiribi.net.NetworkMonitor;
import rs.igram.kiribi.net.natt.*;
import rs.igram.kiribi.service.util.*;

/**
 * 
 *
 * @author Michael Sargent
 */
class Peer {
	KeyPair pair;
	Address address;
	//NetworkExecutor executor;
	ServiceAdmin admin;
	InetSocketAddress serverAdress;
	InetSocketAddress socketAddress;
	EndpointProvider provider;
	EntityManager mgr;
	Set<Service> services = new HashSet<>();
	
	Peer(int port, InetSocketAddress serverAddress, Type type) {
		this.serverAdress = serverAdress;
		try{
			pair = KeyPairGenerator.generateKeyPair();
			address = new Address(pair.getPublic());
			//executor = new NetworkExecutor();
			socketAddress = new InetSocketAddress(NetworkMonitor.inet(), port);
			
			switch(type){
			case LAN:
				provider = EndpointProvider.tcp(AddressMapper.discovery(address, socketAddress, serverAddress));
				break;
			case TCP:
				provider = EndpointProvider.tcp(AddressMapper.lookup(address, socketAddress, serverAddress));
				break;
			case UDP:
				provider = EndpointProvider.udp(socketAddress, address, serverAddress);
				break;	
			}
			admin = new ServiceAdmin(pair, port, provider);
			mgr = admin.entityManager(null);
		} catch(Exception e) {
			throw new RuntimeException(e);
		}	
	}
	
	void configure() {
	
	}
	
	void activate() {
		services.forEach(admin::activate);
	}
	
	void deactivate() {
		admin.shutdown();
	}
	
	static enum Type {LAN, TCP, UDP}
}
