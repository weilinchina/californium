/*******************************************************************************
 * Copyright (c) 2015 Institute for Pervasive Computing, ETH Zurich and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 * 
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 *    http://www.eclipse.org/org/documents/edl-v10.html.
 * 
 * Contributors:
 *    Matthias Kovatsch - creator and main architect
 *    Martin Lanter - architect and re-implementation
 *    Francesco Corazza - HTTP cross-proxy
 ******************************************************************************/
package org.eclipse.californium.proxy.resources;

import org.eclipse.californium.core.coap.CoAP.ResponseCode;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.proxy.CoapTranslator;
import org.eclipse.californium.proxy.TranslationException;


/**
 * Resource that forwards a coap request with the proxy-uri option set to the
 * desired coap server.
 */
public class ProxyCoapClientResource extends ForwardingResource {
	
	private long timeout;
	
	public ProxyCoapClientResource() {
		this(100000); // 100 s
	} 
	
	public ProxyCoapClientResource(long timeout) {
		super("coap2coap");
		this.timeout = timeout;
	}

	@Override
	public Response forwardRequest(Request request) {
		LOGGER.info("ProxyCoAP2CoAP forwards "+request);
		Request incomingRequest = request;

		// check the invariant: the request must have the proxy-uri set
		if (!incomingRequest.getOptions().hasProxyUri()) {
			LOGGER.warning("Proxy-uri option not set.");
			return new Response(ResponseCode.BAD_OPTION);
		}

		// create a new request to forward to the requested coap server
		Request outgoingRequest = null;
		try {
			// create the new request from the original
			outgoingRequest = CoapTranslator.getRequest(incomingRequest);

			// execute the request
			LOGGER.finer("Sending coap request.");

			LOGGER.info("ProxyCoapClient received CoAP request and sends a copy to CoAP target");
			outgoingRequest.send();

			// accept the request sending a separate response to avoid the
			// timeout in the requesting client
			LOGGER.finer("Acknowledge message sent");
		} catch (TranslationException e) {
			LOGGER.warning("Proxy-uri option malformed: " + e.getMessage());
			return new Response(CoapTranslator.STATUS_FIELD_MALFORMED);
		} catch (Exception e) {
			LOGGER.warning("Failed to execute request: " + e.getMessage());
			return new Response(ResponseCode.INTERNAL_SERVER_ERROR);
		}

		try {
			// receive the response
			Response receivedResponse = outgoingRequest.waitForResponse(timeout);

			if (receivedResponse != null) {
				LOGGER.finer("Coap response received.");

				// create the real response for the original request
				Response outgoingResponse = CoapTranslator.getResponse(receivedResponse);

				return outgoingResponse;
			} else {
				LOGGER.warning("No response received.");
				return new Response(CoapTranslator.STATUS_TIMEOUT);
			}
		} catch (InterruptedException e) {
			LOGGER.warning("Receiving of response interrupted: " + e.getMessage());
			return new Response(ResponseCode.INTERNAL_SERVER_ERROR);
		}
	}
}
