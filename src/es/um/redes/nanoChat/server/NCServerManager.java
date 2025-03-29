package es.um.redes.nanoChat.server;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import es.um.redes.nanoChat.messageML.NCMessage;
import es.um.redes.nanoChat.server.roomManager.NCRoom;
import es.um.redes.nanoChat.server.roomManager.NCRoomDescription;
import es.um.redes.nanoChat.server.roomManager.NCRoomManager;

/**
 * Esta clase contiene el estado general del servidor (sin la lógica relacionada
 * con cada sala particular)
 */
class NCServerManager {

	// Primera habitación del servidor
	final static byte INITIAL_ROOM = 'A';
	final static String ROOM_PREFIX = "Room";
	// Siguiente habitación que se creará
	byte nextRoom;
	// Usuarios registrados en el servidor
	private Set<String> users = new HashSet<String>();
	// Habitaciones actuales asociadas a sus correspondientes RoomManagers
	private Map<String, NCRoomManager> rooms = new HashMap<String, NCRoomManager>();

	NCServerManager() {
		nextRoom = INITIAL_ROOM;
		for (int i = 0; i < 4; i++) {
			String roomName = ROOM_PREFIX + (char) nextRoom;
			NCRoom rm = new NCRoom(roomName);
			rooms.put(roomName, rm);
			nextRoom += (byte) 1;
		}
	}

	// Método para registrar un RoomManager
	public void registerRoomManager(NCRoomManager rm) {
		// Dar soporte para que pueda haber más de una sala en el servidor
		rooms.put(rm.getDescription().roomName, rm);
	}

	// Devuelve la descripción de las salas existentes
	public synchronized List<NCRoomDescription> getRoomList() {
		List<NCRoomDescription> rd = new ArrayList<NCRoomDescription>();
		// Pregunta a cada RoomManager cuál es la descripción actual de su sala
		for (String room : rooms.keySet()) {
			// Añade la información al ArrayList
			rd.add(rooms.get(room).getDescription());
		}
		return rd;
	}

	// Intenta registrar al usuario en el servidor.
	public synchronized boolean addUser(String user) {
		// Devuelve true si no hay otro usuario con su nombre
		// Devuelve false si ya hay un usuario con su nombre
		if (!users.contains(user)) {
			return users.add(user);
		} else
			return false;
	}

	// Elimina al usuario del servidor
	public synchronized void removeUser(String user) {
		// Elimina al usuario del servidor
		users.remove(user);
	}

	// Un usuario solicita acceso para entrar a una sala y registrar su conexión en
	// ella
	public synchronized NCRoomManager enterRoom(String u, String room, Socket s) {
		// Verificamos si la sala existe
		if (rooms.containsKey(room) && rooms.get(room).registerUser(u, s)) {
			// Si la sala existe y si es aceptado en la sala entonces devolvemos el
			// RoomManager de la sala
			return rooms.get(room);
		}
		// Decidimos qué hacer si la sala no existe (devolver error O crear la sala)
		return null;
	}

	public synchronized void kickRoom(String admin, String u, String room) {
		if(((NCRoom) rooms.get(room)).getAdmin().contains(u)) {
			try {
				((NCRoom) rooms.get(room)).send(admin, NCMessage.OP_SEND, "No puedes kickear a un administrador");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else {
			
		((NCRoom) rooms.get(room)).removeUser(u);
		for (String user : ((NCRoom) rooms.get(room)).getUsers().keySet()) {
			try {
				((NCRoom) rooms.get(room)).send(user,NCMessage.OP_SEND, "El usuario " + u + " fue expulsado de la sala");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		}
	}

	// Un usuario deja la sala en la que estaba
	public synchronized void leaveRoom(String u, String room) {
		// Verificamos si la sala existe
		if (rooms.containsKey(room)) {
			// Si la sala existe sacamos al usuario de la sala
			((NCRoom) rooms.get(room)).removeUser(u);
		}
		// Decidir qué hacer si la sala se queda vacía
	}

	public boolean createRoom(String name) {
		if (rooms.containsKey(name)) {
			return false;
		} else {
			registerRoomManager(new NCRoom(name));
			return true;
		}
	}
	
	public void promoteUser (String room, String admin, String user) {
		NCRoomManager rm = rooms.get(room);
		if (((NCRoom) rm).getAdmin().contains(admin)) {
			try {
				((NCRoom) rm).promote(admin,user);
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		
	}

	public boolean renameRoom(String old_name, String new_name, String a) {
		if (!rooms.containsKey(new_name)) {
			NCRoomManager rm = rooms.get(old_name);
			if (((NCRoom) rm).getAdmin().contains(a)) {
				rooms.remove(old_name);
				rooms.put(new_name, rm);
				return true;
			}
		}
		return false;

	}
}
