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

/** 
 * <h2>Kiribi Service Module</h2>
 *
 * <h3>Overview</h3> 
 * Provides classes and interfaces to support secure peer-to-peer networking.
 * 
 * <h3>Dependencies</h3>
 * <h4>Requires:</h4>
 *     &emsp;rs.igam.kiribi.io<br>
 *     &emsp;rs.igam.kiribi.crypto<br>
 *     &emsp;rs.igam.kiribi.net<br>
 *
 * <h4>Exports:</h4>   
 *	   &emsp;rs.igam.kiribi.service<br>
 * 
 * @author Michael Sargent
 */
module rs.igram.kiribi.service {
	requires java.base;
	requires transitive rs.igram.kiribi.crypto;
	requires transitive rs.igram.kiribi.io;
	requires transitive rs.igram.kiribi.net;
	exports rs.igram.kiribi.service;
}