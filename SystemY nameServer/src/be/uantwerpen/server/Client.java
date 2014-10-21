package be.uantwerpen.server;

import java.util.ArrayList;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name = "nodes")
@XmlType(propOrder = {"id", "name", "ipaddress", "files"})

public class Client {
	//id geen property denk ik maar als atribute voor node
	//anders werkt de check niet om duplicates in de hashmap te vermijden
	private int id;
	private int name;
	private String ipaddress;
	private Files files; 
	//private String[] files;

	public Client() {
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getName() {
		return name;
	}

	public void setName(int name) {
		this.name = name;
	}

	public String getIpaddress() {
		return ipaddress;
	}

	public void setIpaddress(String ipaddress) {
		this.ipaddress = ipaddress;
	}

	public Files getFiles() {
		return files;
	}

	public void setFiles(Files files) {
		this.files = files;
	}

}
