package es.um.redes.nanoChat.server.roomManager;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import es.um.redes.nanoChat.messageML.NCMessage;

public class NCRoom extends NCRoomManager {

	private NCRoomDescription rd;
	private HashMap<String, Socket> users;
	private HashSet<String> admin;

	public NCRoom(String name) {
		super.roomName = name;
		rd = new NCRoomDescription(name, new LinkedList<String>(), 0);
		users = new HashMap<String, Socket>();
		admin = new HashSet<String>();
	}

	public void promoteDefault(String u) throws IOException {
		if (users.containsKey(u) && usersInRoom() < 2) {
			rd.admins.add(u);
			admin.add(u);
			
		}
	}

	public void promote(String admin, String user) throws IOException {
		if (users.containsKey(user)) {
		rd.admins.add(user);
		this.admin.add(user);
		}
		else {
			send(admin, NCMessage.OP_SEND, "El usuario no se encuentra en la sala");
		}
	}

	@Override
	public boolean registerUser(String u, Socket s) {
		if (users.containsKey(u)) {
			return false;
		} else {
			rd.members.add(u);
			users.put(u, s);
			return true;
		}
	}

	@Override
	public void broadcastMessage(String u, String message) throws IOException {
		for (String user : users.keySet()) {
			Socket socket = users.get(user);
			DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
			dos.writeUTF(NCMessage.makeRoomMessage(NCMessage.OP_SEND, u + ": " + message).toEncodedString());
		}
		rd.timeLastMessage = System.currentTimeMillis();
	}

	@Override
	public void removeUser(String u) {
		rd.members.remove(u);
		users.remove(u);
	}

	@Override
	public void setRoomName(String roomName) {
		super.roomName = roomName;
		rd.roomName = roomName;
	}

	@Override
	public NCRoomDescription getDescription() {
		LinkedList<String> rd_members_copy = new LinkedList<String>();
		for (String u : rd.members) {
			rd_members_copy.add(u);
		}
		return new NCRoomDescription(rd.roomName, rd_members_copy, rd.timeLastMessage);
	}

	@Override
	public int usersInRoom() {
		return rd.members.size();
	}

	public void joinRoom(String u) throws IOException {

		for (String user : users.keySet()) {
			if (!user.equals(u)) {
				Socket socket = users.get(user);
				DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
				dos.writeUTF(NCMessage.makeRoomMessage(NCMessage.OP_SEND, "El usuario " + u + " se ha unido a la sala.")
						.toEncodedString());
			}
		}
	}

	public void leaveRoom(String u) throws IOException {
		for (String user : users.keySet()) {
			if (!user.equals(u)) {
				Socket socket = users.get(user);
				DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
				dos.writeUTF(NCMessage.makeRoomMessage(NCMessage.OP_SEND, "El usuario " + u + " abandonó la sala.")
						.toEncodedString());
			}
		}
	}

	public void sendKick(String admin, String u) throws IOException {

		if (users.containsKey(u)) {
			if (getAdmin().contains(u));
				
			else {
				Socket socket = users.get(u);
				DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
				dos.writeUTF(NCMessage.makeRoomMessage(NCMessage.OP_KICKED, u).toEncodedString());
			}
		} else {
			send(admin, NCMessage.OP_SEND, "El usuario no se encuentra en la sala");
		}
	}

	public void send(String user, byte opCode, String msg) throws IOException {
		Socket socket = users.get(user);
		DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
		dos.writeUTF(NCMessage.makeRoomMessage(opCode, msg).toEncodedString());
	}

	public void sendWhisp(String userA, String userB, String message) throws IOException {

		if (!users.containsKey(userB)) {
			Socket socket = users.get(userA);
			DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
			dos.writeUTF(NCMessage.makeOpMessage(NCMessage.OP_FAILURE).toEncodedString());
		} // TODO
		else {
			Socket socket = users.get(userB);
			DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
			dos.writeUTF(NCMessage.makeRoomMessage(NCMessage.OP_SEND, "(Whisp) " + userA + ": " + message)
					.toEncodedString());
			// Los mensajes privados no actualizaran el momento del ultimo mensaje.
		}
	}

	public void rename(String name) throws IOException {

		super.roomName = name;
		rd.roomName = name;
		for (Socket s : users.values()) {
			DataOutputStream dos = new DataOutputStream(s.getOutputStream());
			dos.writeUTF(NCMessage.makeRoomMessage(NCMessage.OP_RENAMED, name).toEncodedString());
		}
	}

	public Set<String> getAdmin() {
		return Collections.unmodifiableSet(admin);
	}
	
	public Map<String, Socket> getUsers() {
		return Collections.unmodifiableMap(users);
	}

}
