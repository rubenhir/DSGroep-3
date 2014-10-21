package be.uantwerpen.server;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import java.rmi.AlreadyBoundException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class nameServer {
	static HashMap<Integer, Client> nodeMap = new HashMap<Integer, Client>();
	static int id = 0;
	static nodeHandling fileImpl;
	public static int k = 0;
	
	/* GET/SET */
	// instantie aanmaken
	public static nodeHandling getFileImpl() {
		return fileImpl;
	}
	public static void setFileImpl(nodeHandling nodeHandling) {
		fileImpl = nodeHandling;
	}
	
	// main functie: aanroepen bij opstart van de server
    public static void main(String[] argv) throws RemoteException, ClassNotFoundException {
    	NodeToNodeInterface ntnI = null;
    	String name = null;
    	setFileImpl(new nodeHandling());

    	
    	// locatie van nameserver
       String bindLocation = "//localhost/nameServer";
       /*
       try { 
			LocateRegistry.createRegistry(9001);
			Naming.bind(bindLocation, getFileImpl());
			//ReadXML();
	        System.out.println("FileServer Server is ready at:" + bindLocation);
            System.out.println("java RMI registry created.");
        } catch (MalformedURLException | AlreadyBoundException e) {
            System.out.println("java RMI registry already exists.");
        }
       */
       
       try { 
    	   byte[] inBuf = new byte[256];
    	   DatagramPacket dgram = new DatagramPacket(inBuf, inBuf.length);
    	   MulticastSocket socket = new MulticastSocket(4545); // must bind receive side
    	   socket.joinGroup(InetAddress.getByName("226.100.100.125"));
		    while(true) {
		      socket.receive(dgram); // blocks until a datagram is received
		      ByteArrayInputStream bis = new ByteArrayInputStream(inBuf);
		      ObjectInput in = null;
		      try {
		        in = new ObjectInputStream(bis);
		        Object o = in.readObject(); 
		        //String[] stringValues = (String[])o;
		        List message = (List)o;
		        String[] clientStats = (String[]) message.get(0);
		        String[] filenames = (String[]) message.get(1);
				addToHashMap(Integer.parseInt(clientStats[0]), clientStats[1], filenames);

			      System.err.println("hash: " + clientStats[0]);
			     
		      } finally {
		        try {
		          bis.close();
		        } catch (IOException ex) {
		          // ignore close exception
		        }
		        try {
		          if (in != null) {
		            in.close();
		          }
		        } catch (IOException ex) {
		          // ignore close exception
		        }
		      }
		      dgram.setLength(inBuf.length); // must reset length field!
		      System.err.println("nummer van clients: " + k);
		      try {
		      name = "//localhost/ntn";
		      ntnI = null;
              ntnI = (NodeToNodeInterface) Naming.lookup(name);
              ntnI.serverAnswer(k);
              k++;
              System.err.println("nummer van clients: " + k);
	          } catch(Exception e) {
		                System.err.println("FileServer exception: "+ e.getMessage());
		                e.printStackTrace();
		      }
              System.err.println("nummer van clients: " + k);
		    }
       }
       catch(UnknownHostException e){}
       catch(IOException e){}
       
    }

    public static void ReadXML() {
    	try {
    		 
    		File fXmlFile = new File("ip-list.xml");
    		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
    		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
    		Document doc = dBuilder.parse(fXmlFile);

    		doc.getDocumentElement().normalize();
    	 
    	 
    		NodeList nList = doc.getElementsByTagName("clients");
    	 
    	 
    		for (int temp = 0; temp < nList.getLength(); temp++) {
    	 
    			Node nNode = nList.item(temp);
    	 
    			//System.out.println("\nCurrent Element :" + nNode.getNodeName());
    			
    			if (nNode.getNodeType() == Node.ELEMENT_NODE) {
    	 
    				Element eElement = (Element) nNode;
    				
    				fileImpl.nodeMap.put(eElement.getElementsByTagName("hashedName").item(0).getTextContent(), eElement.getElementsByTagName("IP").item(0).getTextContent());
    			}
    		}
    		fileImpl.printMap();
    	    } catch (Exception e) {
    	    	e.printStackTrace();
    	    }
    	
    }
    
	/**
	 * not working
	 * @param hashedName
	 * @param ip
	 * @param filenames
	 */
	public void removeFromHashMap(int hashedName, String ip, String[] filenames) {
		Client node = new Client();
		node.setId(1); node.setName(hashedName); node.setIpaddress(ip); node.setFiles(filenames);
		while( nodeMap.values().remove(node) ) {
			System.out.println("nodeMap size: " + nodeMap.size());
		}
		
		//update xml
	}
    
    public static void addToHashMap(int hashedName, String ip, String[] filenames) {
		Client node = new Client();
		node.setId(1); node.setName(hashedName); node.setIpaddress(ip); node.setFiles(filenames);
		if (!nodeMap.containsValue(node)) {
			nodeMap.put(id, node);
			id++;
		}
		System.out.println("nodeMap size: " + nodeMap.size());
		
		//use nodemap to update XML file
		XMLParser xmlParser = new XMLParser();
		//xmlParser.addNode(hashedName, ip);
		//xmlParser.addFilesToNode(hashedName, ip, filenames);
	}
}	