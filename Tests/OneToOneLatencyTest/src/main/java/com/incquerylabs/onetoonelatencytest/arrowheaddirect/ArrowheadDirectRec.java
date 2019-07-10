package com.incquerylabs.onetoonelatencytest.arrowheaddirect;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.incquerylabs.onetoonelatencytest.Constants;
import com.incquerylabs.onetoonelatencytest.Receiver;

import eu.arrowhead.client.common.Utility;
import eu.arrowhead.client.common.exception.ArrowheadException;
import eu.arrowhead.client.common.exception.ExceptionType;
import eu.arrowhead.client.common.model.ArrowheadService;
import eu.arrowhead.client.common.model.ArrowheadSystem;
import eu.arrowhead.client.common.model.ServiceRegistryEntry;

public class ArrowheadDirectRec extends Thread implements Receiver {

	private static final String SR_REG_PATH = "serviceregistry/register";
	private static final String SR_UNREG_PATH = "serviceregistry/remove";
	Map<Integer, Instant> mid = new HashMap<Integer, Instant>();
	Map<Integer, Instant> end = new HashMap<Integer, Instant>();
	ServerSocket serverSocket = null;

	@Override
	public void run() {
		String srUri = Utility.getUri(Constants.ARROWHEAD_SERVICE_REGISTRY_IP,
				Constants.ARROWHEAD_SERVICE_REGISTRY_PORT, SR_REG_PATH, false, true);
		ArrowheadSystem me = new ArrowheadSystem("arrdrec", "0.0.0.0", Constants.ARROWHEAD_PROVIDER_PORT, "null");
		Set<String> interfaces = new HashSet<String>();
		interfaces.add(Constants.ARROWHEAD_INTERFACE_NAME);
		Map<String, String> serviceMetadata = new HashMap<String, String>();
		ArrowheadService service = new ArrowheadService(Constants.ARROWHEAD_SERVICE_NAME, interfaces, serviceMetadata);
		ServiceRegistryEntry sre = new ServiceRegistryEntry(service, me, "NOTRESTFUL");
		try {
			Utility.sendRequest(srUri, "POST", sre);
		} catch (ArrowheadException e) {
			if (e.getExceptionType() == ExceptionType.DUPLICATE_ENTRY) {
				System.out.println(
						"Received DuplicateEntryException from SR, sending delete request and then registering again.");
				String unregUri = Utility.getUri(Constants.ARROWHEAD_SERVICE_REGISTRY_IP,
						Constants.ARROWHEAD_SERVICE_REGISTRY_PORT, SR_UNREG_PATH, false, false);
				Utility.sendRequest(unregUri, "PUT", sre);
				Utility.sendRequest(srUri, "POST", sre);
			}
		}
		try {
			serverSocket = new ServerSocket(Constants.ARROWHEAD_PROVIDER_PORT);
			System.out.println("Listener started.");
			while (true) {
				Socket socket = serverSocket.accept();
				PrintWriter out = new PrintWriter(socket.getOutputStream());
				InputStream in = socket.getInputStream();
				BufferedReader reader = new BufferedReader(new InputStreamReader(in));
				byte[] buff = new byte[65536];
				int count;
				String s = reader.readLine();
				Integer fileSize = Integer.parseInt(s);
				System.out.println("Arrowhead direct message Received");
				while (fileSize > 0) {
					count = in.read(buff);
					fileSize = fileSize - count;
				}
				out.println("gg");
				try {
					socket.close();
				} catch (IOException e) {
					// expected?
				}
			}
		} catch (IOException e) {
			// Expected: the kill of organizer interrupts the severSocket.accept()
			//System.out.println("In diect rec: " + e.getMessage());
		} finally {
			if (serverSocket != null) {
				try {
					serverSocket.close();
					serverSocket = null;
				} catch (IOException e) {
					System.out.println("Writer unclosing!!!?");
				}
			}
		}
	}

	@Override
	public void kill() {
		if (serverSocket != null) {
			try {
				serverSocket.close();
				serverSocket = null;
			} catch (IOException e) {
				System.out.println("Arr Rec unable to kill serversocket");
			}
		}
		this.interrupt();
	}
}