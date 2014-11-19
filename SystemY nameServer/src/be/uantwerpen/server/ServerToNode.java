package be.uantwerpen.server;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;

public class ServerToNode extends UnicastRemoteObject implements ServerToNodeInterface {
	
	ClientMap clientMap;
	XMLMarshaller marshaller = new XMLMarshaller();
	
	protected ServerToNode(ClientMap clientMap) throws RemoteException {
		super();
		this.clientMap = clientMap;
	}

	/***
	 * Get an array containing the neighbours of the specified node. Index 0 = previous, index 1 = next
	 */
	public int[] getNeighbourNodes(int nodeHash) throws RemoteException {
		Object[] tmp = null;
		int[] keys = null;
		int[] neighbours = null;
		
		//can't get index for key in treemap so we use a custom implementation
		//copy map to an array of type object
		tmp = this.clientMap.getClientMap().keySet().toArray();
		keys = new int[keys.length];
		neighbours = new int[2];
		//parsing from object[] to int[] isn't possible so
		//individualy parse keys to int
		for (int i = 0; i < tmp.length; i++) {
			keys[i] = (int)tmp[i];
		}
		
		for (int i = 0; i < keys.length; i++) {
			if (keys[i] == nodeHash) {
				//if index is on first element, previous neighbour = last element
				if (i == 0) {
					neighbours[0] = keys[keys.length - 1];
					neighbours[1] = keys[i + 1];
				}
				//if index is on last element, next neighbour = first element
				else if (i == keys.length - 1) {
					neighbours[0] = keys[i - 1];
					neighbours[1] = keys[0];
				}
				//normal case
				else {
					neighbours[0] = keys[i - 1];
					neighbours[1] = keys[i + 1];
				}
			}
		}
		
		return neighbours;
	}

	@Override
	public void removeNode(int nodeHash) throws RemoteException {
		this.clientMap.removeKeyValuePair(nodeHash);
		marshaller.jaxbObjectToXML(clientMap);
	}

	@Override
	public String getNodeIPAddress(int nodeHash) throws RemoteException {
		Client c = null;
		c = this.clientMap.getClientMap().get(nodeHash);
		return c.getIpaddress();
	}

	@Override
	public int[] getPreviousAndNextNodeHash(int hash) throws RemoteException {
		//Object type because can't cast to int array
		Object[] tmp = this.clientMap.getClientMap().keySet().toArray();
		int[] keys = new int[tmp.length];
		//cast all elements to int
		for (int i = 0; i < tmp.length; i++) {
			keys[i] = (int)tmp[i];
		}
		//previous and next node array
		int[] hashes = new int[2];
		for (int i = 0; i < keys.length; i++) {
			if (keys[i] == hash) {
				if (i == 0) {
					hashes[0] = keys[keys.length - 1];
					hashes[1] = keys[i + 1];
				} else if (i == keys.length -1) {
					hashes[0] = keys[i - 1];
					hashes[1] = keys[0];
				} else {
					hashes[0] = keys[i - 1];
					hashes[1] = keys[i + 1];
				}
			}
		}
		return hashes;
	}
}