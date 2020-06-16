/*******************************************************************************
 * Copyright (c) 2015 Institute for Pervasive Computing, ETH Zurich and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 * 
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v20.html
 * and the Eclipse Distribution License is available at
 *    http://www.eclipse.org/org/documents/edl-v10.html.
 * 
 * Contributors:
 *    Matthias Kovatsch - creator and main architect
 ******************************************************************************/
package org.eclipse.californium.plugtests.resources;

import static org.eclipse.californium.core.coap.CoAP.ResponseCode.*;
import static org.eclipse.californium.core.coap.MediaTypeRegistry.*;

import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.server.resources.CoapExchange;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

/**
 * This resource implements a test of specification for the
 * ETSI IoT CoAP Plugtests, London, UK, 16, June 2020.
 */
public class Files extends CoapResource {

	public Files() {
		super("files");
		getAttributes().setTitle("File resource");
		getAttributes().addResourceType("block");
		getAttributes().setMaximumSizeEstimate(1280);
	}

	@Override
	public void handleGET(CoapExchange exchange) {
		String filename = "test_FW.bin"; // default name ...
		
		System.out.println("Large ### options" + exchange.getRequestOptions());
		if(exchange.getRequestOptions() != null) {
			List<String> paths = exchange.getRequestOptions().getUriPath();
			System.out.println("Large ### paths" + paths);
			if(paths != null) {
				for(int i = 0; i < paths.size(); i ++) {
					String path = paths.get(i);
					System.out.println("Large ### path = " + path);
					int idx = path.indexOf("name=");
					if(idx != -1) {
						filename = path.substring(idx + "name=".length());
						if(filename.isEmpty())
							filename = "test_FW.bin";
					}
				}
			}
		}
	
		System.out.println("Large ### filename = " + filename);

		byte[] bData = null;
        DataInputStream dis = null;
        try {
        	File f = new File(filename);
        	dis = new DataInputStream(new FileInputStream(f));
            System.out.println("size available: " + dis.available());
            System.out.println("file size: " + f.length());
            bData = new byte[dis.available()];
            int size = dis.read(bData);
            System.out.println("read size: " + size);
        } catch (IOException e) {
        	System.out.println("read file failed!");
        	e.printStackTrace();
        	try {
        		dis.close();
        	} catch (IOException e1) {
        		e1.printStackTrace();
        	}
        } finally {
        	try {
        		dis.close();
        	} catch (IOException e) {
        		e.printStackTrace();
        	}
        }

        exchange.respond(CoAP.ResponseCode.CONTENT, bData, 0);
	}

}
