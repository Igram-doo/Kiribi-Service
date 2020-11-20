package rs.igram.kiribi.service;

import java.io.IOException;

/**
 * Functional interface for response handling.
 *
 * @author Michael Sargent
 */
@FunctionalInterface
public interface  ResponseHandler {
	/**
	 * Process a response.
	 *
	 * @param response The response to process.
	 * @throws IOException if the was a problem processing the response.
	 */	
	void apply(Message response) throws IOException;
}