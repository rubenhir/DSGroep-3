package be.uantwerpen.server;

import java.net.*;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.io.*;
import java.rmi.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.List;

public class Client {
	
	//Client client;
	public int previousHash, ownHash, nextHash; //declaratie van de type hashes
	public NodeToNode ntn; //declaratie van remote object

	public Client() throws RemoteException, InterruptedException, IOException, ClassNotFoundException {

		ntn = new NodeToNode();
		Registry registry = null;
		
		/******************************************/
		/* enter client name in console and enter */
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        System.out.print("Please enter client name: ");
        String nameClient = null;
        try {
        	nameClient = reader.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
        /* end console input */
        /******************************************/
        
        //get all filenames
		String[] filenames = listFilesInDir("C:/Users/Kennard/Projects/Eclipse-JEE-Luna-SR1/Workspace/DSGroep-3/Client/src/be/uantwerpen/server/files/");
		//sendFilesOverTCP(filenames, 20000);
		//receiveFilesOverTCP("127.0.0.1", 20000);
		Boolean shutdown = false;
		
		//set own to hashed own name
		ownHash = hashString(nameClient);
		
		//fill array with data
		String[] clientStats = new String[2];
		clientStats[0] = ownHash + ""; //hashed own name
		clientStats[1] = Inet4Address.getLocalHost().getHostAddress(); //own ip address
		clientStats[2] = "online"; //status van client 
		
		//list with clientstats arr and filenames arr
		List message = new ArrayList();
		message.add(clientStats);
		message.add(filenames);
		message.add(shutdown);

		//create message and multicast it
		Object obj = message; 
		DatagramSocket socket = new DatagramSocket();
		ByteArrayOutputStream byteArr = new ByteArrayOutputStream();
		ObjectOutput objOut = new ObjectOutputStream(byteArr);
		objOut.writeObject(obj);
		byte[] b = byteArr.toByteArray();
		DatagramPacket dgram;
		dgram = new DatagramPacket(b, b.length, InetAddress.getByName("226.100.100.125"), 4545);
		String bindLocation = "//localhost/ntn";
		try {
			registry = LocateRegistry.createRegistry(1099);
		} catch (Exception e) {
		}
		try {
			Naming.bind(bindLocation, ntn);
		} catch (Exception e) {
		}
		socket.send(dgram);
		System.out.println("Multicast sent");
		
		//keep looping as long as nextHash isn't changed or number of nodes isn't changed
		while (ntn.nextHash == -1 || ntn.numberOfNodes == -1)
		{
			System.out.println("Waiting, next hash: "+ntn.nextHash + " # of nodes: " + ntn.numberOfNodes);
			
			//if there are no neighbor nodes 
			if (ntn.numberOfNodes == 0)
			{
				System.out.println("No neighbours! All hashes set to own");
				//set next and previous hash equal to own hash
				ntn.nextHash = ownHash;
				ntn.prevHash = ownHash;
			}
			try {
				//wait 100 ms
				Thread.sleep(100);
			} catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
		}

		try {
			Naming.unbind(bindLocation); //unbind to free for other nodes
		} catch (NotBoundException e) {
			System.err.println("Not bound");
		}
		System.out.println("Total connected clients: " + (ntn.numberOfNodes + 1)); //waarom +1?
		
		//set client's hash fields
		nextHash = ntn.nextHash();
		previousHash = ntn.prevHash();
		System.out.println("Hashes: Previous: " + ntn.prevHash + ". Own: " + ownHash + ". Next: " + ntn.nextHash);
		
		if(ntn.numberOfNodes == 2){
			//shutdown(previousHash, nextHash, clientStats, filenames, message);
		}
		
		
		waitForClients();

	}
	
	public void run() {
		try {
			main(null);
		} catch (ClassNotFoundException | InterruptedException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	void failure(){
		//NodeToNode ntn = new NodeToNode();
		int next = ntn.nextHash;
		int prev = ntn.prevHash;
		//if ()
	}
	
	void waitForClients() throws ClassNotFoundException {
		try {
			byte[] inBuf = new byte[256];
			DatagramPacket dgram = new DatagramPacket(inBuf, inBuf.length);
			MulticastSocket socket = new MulticastSocket(4545);
			socket.joinGroup(InetAddress.getByName("226.100.100.125"));
			
			//do this forever
			while (true) {
							
				socket.receive(dgram); //blocks untill package is received
				ByteArrayInputStream bis = new ByteArrayInputStream(inBuf);
				ObjectInput in = null;
				
				try {
					in = new ObjectInputStream(bis);
					Object o = in.readObject();
					List message = (List) o;
					String[] clientStats = (String[]) message.get(0);
					int receivedHash = Integer.parseInt(clientStats[0]); //get hashesName from message
				
					try {
						String name = "//localhost/ntn";
					
							NodeToNodeInterface ntnI = (NodeToNodeInterface) Naming.lookup(name);

						if (ownHash > nextHash) { //laatste hash 
							if ((previousHash < receivedHash) && (ownHash > receivedHash)) {
								try{
									ntnI.answerDiscovery(previousHash, ownHash); //send my hashes to neighbours via RMI
								}catch(RemoteException e){
									System.out.println("geen antwoord van vorige hash");
								}
								
								previousHash = receivedHash;
								System.out.println(previousHash + " "  + ownHash + " " + nextHash);
							} 
							else {
								ntnI.answerDiscovery(ownHash, nextHash); //send my hashes to neighbours via RMI
								nextHash = receivedHash;
								System.out.println(previousHash + " "  + ownHash + " " + nextHash);
							}
						} 
						else if(ownHash == nextHash) {
							ntnI.answerDiscovery(ownHash, ownHash); //send my hashes to neighbours via RMI
							previousHash = receivedHash;
							nextHash = receivedHash;
							System.out.println(previousHash + " "  + ownHash + " " + nextHash);
							//doorsturen via RMI
							
						}
						else { 
							if ((previousHash < receivedHash) && (ownHash > receivedHash)) {
								ntnI.answerDiscovery(previousHash, ownHash); //send my hashes to neighbours via RMI
								previousHash = receivedHash;
								System.out.println(previousHash + " "  + ownHash + " " + nextHash);
								//RMI
							} else if ((ownHash < receivedHash) && (nextHash > receivedHash)) {
								ntnI.answerDiscovery(ownHash, nextHash); //send my hashes to neighbours via RMI
								nextHash = receivedHash;
								System.out.println(previousHash + " "  + ownHash + " " + nextHash);
							}
							else if((previousHash == nextHash) || (previousHash > ownHash)) {
								ntnI.answerDiscovery(previousHash, ownHash); //send my hashes to neighbours via RMI
								previousHash = receivedHash;
								System.out.println(previousHash + " "  + ownHash + " " + nextHash);
							}
						}
						
						//System.out.println("waitForClients hashes set : Previous: " + ntn.prevHash + ". Own: " + ownHash + ". Next: " + ntn.nextHash);
						
					} catch(Exception e) {
						System.err.println("Fileserver exception: " + e.getMessage());
						e.printStackTrace();
					}
				} finally {
					try {
						bis.close();
					} catch (IOException ex) {

					}
					try {
						if (in != null) {
							in.close();
						}
					} catch (IOException ex) {

					}
				}
				dgram.setLength(inBuf.length);
			}
		} catch (UnknownHostException e) {
		} catch (IOException e) {
		}
	}
	
    public void shutdown(int previoushashnode, int nexthashnode, String[] cs, String[] fn, List<Object> message) throws IOException {
        System.out.println("Shutting down..");

    	previousHash = nexthashnode;

        System.out.println("Sending id from next node to previous node..");

    	System.out.printf("Client %d down!", ownHash);
    	
        previousHash = nexthashnode;
        System.out.println("Changing info from next node in previous node..");

        System.out.printf("the next client's previous hash is changed to %d \n", previousHash);
        System.out.println("Sending id from previous node to next node..");
        System.out.printf("the previous client's next hash is changed to %d \n", nextHash);
        System.out.println("Delete node at nameserver..");

        nextHash = previoushashnode;

        ntn.numberOfNodes--;
        Boolean shutdown = true;
        //create message and multicast it
        Object obj = message; 
        message.remove(shutdown);
        message.add(shutdown);
        DatagramSocket socket = new DatagramSocket();
        ByteArrayOutputStream byteArr = new ByteArrayOutputStream();
        ObjectOutput objOut = new ObjectOutputStream(byteArr);
        objOut.writeObject(obj);
        byte[] b = byteArr.toByteArray();
        DatagramPacket dgram;
        dgram = new DatagramPacket(b, b.length, InetAddress.getByName("226.100.100.125"), 4545);
        String bindLocation = "//localhost/ntn";
        try {
            Registry registry = LocateRegistry.createRegistry(1099);
        } catch (Exception e) {
        }
        try {
            Naming.bind(bindLocation, ntn);
        } catch (Exception e) {
        }
        socket.send(dgram);
        System.out.println("Multicast sent");
        System.exit(1);

	}
	
    /**
     * Send multiple files over a socket via TCP
     * @param filenames
     * a string array containing all the filenames 
     * @param port
     * what port to send the files over
     * @throws IOException
     */
    void sendFilesOverTCP(String[] filenames, int port) throws IOException  {
    	ServerSocket ssocket = new ServerSocket(port);
        File[] files = new File[filenames.length];
        for (int i = 0; i < files.length; i++) {
			files[i] = new File(filenames[i]);
		}
        while (true) {
          Socket socket = ssocket.accept();
          System.out.println("Socket created");
          for (File file : files) {
        	  DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
        	  byte[] nameInBytes = file.getName().getBytes("UTF-8");
        	  byte[] contentsInBytes = fileToByteArr(file.getName());
        	  dos.writeInt(nameInBytes.length);
        	  dos.write(nameInBytes);
        	  dos.writeInt(contentsInBytes.length);
        	  dos.write(contentsInBytes);
        	  dos.flush();
        	  System.out.println("Sent file: " + file.getName());
          }
          socket.close();
        }
    }
    
    /**
     * Receive multiple files over a socket via TCP
     * @param ip
     * What host to receive on, passing null is equal to loopback 
     * @param port
     * @throws UnknownHostException
     * @throws IOException
     */
    void receiveFilesOverTCP(String ip, int port) throws UnknownHostException, IOException {
    	boolean filesRcvd = false;
    	while (!filesRcvd) {
    		System.out.println("Polling for files");
    		Socket socket = new Socket(ip, port);
        	DataInputStream dis = new DataInputStream(socket.getInputStream());
        	FileOutputStream fos;
        	
    		int size = dis.readInt();  
	    	byte[] nameInBytes = new byte[size];  
	    	dis.readFully(nameInBytes);  
	    	String name = new String(nameInBytes, "UTF-8");
	    	if (!new File(name).isFile()) {
	    		fos = new FileOutputStream(name);
	    		
	    		size = dis.readInt();  
		    	byte[] contents = new byte[size];  
		    	dis.readFully(contents);
		    	
		    	fos.write(contents);
		    	fos.close();
			}
	    	
	    	socket.close();
    	}
    	System.out.println("All files received and saved");
    }
    
    /***
     * Helper function to convert the contents of a file to a byte array
     * @param path
     * path to the file
     * @return bFile
     * byte array of the contents of the file
     */
    byte[] fileToByteArr(String path) {
    	FileInputStream fileInputStream=null;
    	 
        File file = new File(path);
 
        byte[] bFile = new byte[(int) file.length()];
 
        try {
            //convert file into array of bytes
		    fileInputStream = new FileInputStream(file);
		    fileInputStream.read(bFile);
		    fileInputStream.close();
 
		    for (int i = 0; i < bFile.length; i++) {
		    	System.out.print((char)bFile[i]);
            }
 
		    System.out.println("Done");
        }catch(Exception e){
        	e.printStackTrace();
        }
        
        return bFile;
    }
    
    /**
     * List all the files under a directory
     * @param directoryName to be listed
     */
    public String[] listFilesInDir(String directoryName){
 
        File directory = new File(directoryName);
 
        //get all the files from a directory
        File[] fList = directory.listFiles();
        String[] names = new String[fList.length];
        for (int i = 0; i < names.length; i++){
            if (fList[i].isFile()){
                names[i] = directoryName + fList[i].getName();
            }
        }
        return names;
    }
    
    /**
     * Helper method to convert a string to a hash. Range goes from 0 to 32768.
     * @param name
     * String to be hashed
     * @return Returns the hashed inputted string.
     */
    int hashString(String name) {
		return Math.abs(name.hashCode()) % 32768; // berekening van de hash
	}
	
	public static void main(String argv[]) throws InterruptedException, IOException, ClassNotFoundException {
		Client client = new Client();
		
	}
}
