package es.um.redes.nanoChat.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;

import es.um.redes.nanoChat.messageML.NCListMessage;
import es.um.redes.nanoChat.messageML.NCMessage;
import es.um.redes.nanoChat.messageML.NCOpMessage;
import es.um.redes.nanoChat.messageML.NCRoomMessage;
import es.um.redes.nanoChat.messageML.NCWhispMessage;
import es.um.redes.nanoChat.server.roomManager.NCRoom;
import es.um.redes.nanoChat.server.roomManager.NCRoomDescription;
import es.um.redes.nanoChat.server.roomManager.NCRoomManager;

/**
 * A new thread runs for each connected client
 */
public class NCServerThread extends Thread {
	
	private Socket socket = null;
	//Manager global compartido entre los Threads
	private NCServerManager serverManager = null;
	//Input and Output Streams
	private DataInputStream dis;
	private DataOutputStream dos;
	//Usuario actual al que atiende este Thread
	String user;
	//RoomManager actual (dependerá de la sala a la que entre el usuario)
	NCRoomManager roomManager;
	//Sala actual
	String currentRoom;

	//Inicialización de la sala
	public NCServerThread(NCServerManager manager, Socket socket) throws IOException {
		super("NCServerThread");
		this.socket = socket;
		this.serverManager = manager;
	}

	//Main loop
	public void run() {
		try {
			//Se obtienen los streams a partir del Socket
			dis = new DataInputStream(socket.getInputStream());
			dos = new DataOutputStream(socket.getOutputStream());
			//En primer lugar hay que recibir y verificar el nick
			receiveAndVerifyNickname();
			//Mientras que la conexión esté activa entonces...
			while (true) {
				// Obtenemos el mensaje que llega y analizamos su código de operación
				NCMessage message = NCMessage.readMessageFromSocket(dis);
				switch (message.getOpcode()) {
				// 1) si se nos pide la lista de salas se envía llamando a sendRoomList();
				case NCMessage.OP_ROOM_LIST:
				{
					sendRoomList();
					break;
				}
				// 2) Si se nos pide entrar en la sala entonces obtenemos el RoomManager de la sala,
				case NCMessage.OP_ENTER:
				{
					roomManager = serverManager.enterRoom(user, ((NCRoomMessage) message).getName(), socket); 
					if(roomManager != null) {
						// 2) notificamos al usuario que ha sido aceptado y procesamos mensajes con processRoomMessages()
						currentRoom = ((NCRoomMessage) message).getName();
						if(roomManager instanceof NCRoom) {
							((NCRoom) roomManager).joinRoom(user);
							((NCRoom) roomManager).promoteDefault(user);
						}
						dos.writeUTF(NCMessage.makeOpMessage(NCMessage.OP_OK).toEncodedString());
						processRoomMessages();
					} else {
						// 2) Si el usuario no es aceptado en la sala entonces se le notifica al cliente
						dos.writeUTF(NCMessage.makeOpMessage(NCMessage.OP_FAILURE).toEncodedString());
					}
					break;
				}
				case NCMessage.OP_CREATE_ROOM:
				{
					if(serverManager.createRoom(((NCRoomMessage) message).getName())){
						dos.writeUTF(NCMessage.makeOpMessage(NCMessage.OP_OK).toEncodedString());
					} else {
						dos.writeUTF(NCMessage.makeOpMessage(NCMessage.OP_FAILURE).toEncodedString());
					}
					break;
				}
				}
			}
		} catch (Exception e) {
			//If an error occurs with the communications the user is removed from all the managers and the connection is closed
			System.out.println("* User "+ user + " disconnected.");
			serverManager.leaveRoom(user, currentRoom);
			serverManager.removeUser(user);
		}
		finally {
			if (!socket.isClosed())
				try {
					socket.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
		}
	}

	//Obtenemos el nick y solicitamos al ServerManager que verifique si está duplicado
	private void receiveAndVerifyNickname() {
		//La lógica de nuestro programa nos obliga a que haya un nick registrado antes de proseguir
		//Entramos en un bucle hasta comprobar que alguno de los nicks proporcionados no está duplicado
		boolean registered = false;
		while(!registered) {
			try {
				//Extraer el nick del mensaje
				NCRoomMessage message = (NCRoomMessage) NCMessage.readMessageFromSocket(dis);
				String nick = message.getName();
				//Validar el nick utilizando el ServerManager - addUser()
				boolean validNick = serverManager.addUser(nick);
				//Contestar al cliente con el resultado (éxito o duplicado)
				NCOpMessage response;
				if(validNick) {
					user = nick;
					response = (NCOpMessage) NCMessage.makeOpMessage(NCMessage.OP_OK);
					registered = true;
				} else {
					response = (NCOpMessage) NCMessage.makeOpMessage(NCMessage.OP_NICK_DUPLICATED);
				}
				dos.writeUTF(response.toEncodedString());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	//Mandamos al cliente la lista de salas existentes
	private void sendRoomList()  {
		// La lista de salas debe obtenerse a partir del RoomManager y después enviarse mediante su mensaje correspondiente
		ArrayList<NCRoomDescription> rooms = (ArrayList<NCRoomDescription>) serverManager.getRoomList();
		NCListMessage message = (NCListMessage) NCMessage.makeListMessage(NCMessage.OP_LIST, rooms);
		try {
			dos.writeUTF(message.toEncodedString());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void processRoomMessages() throws Exception  {
		// Comprobamos los mensajes que llegan hasta que el usuario decida salir de la sala
		boolean exit = false;
		while (!exit) {
			// Se recibe el mensaje enviado por el usuario
			NCMessage message = NCMessage.readMessageFromSocket(dis);
			// Se analiza el código de operación del mensaje y se trata en consecuencia
			switch (message.getOpcode()) {
			case NCMessage.OP_SEND:
			{
				roomManager.broadcastMessage(user, ((NCRoomMessage) message).getName());
				break;
			}
			case NCMessage.OP_EXIT:
			{
				serverManager.leaveRoom(user, currentRoom);
				if(roomManager instanceof NCRoom) {
					((NCRoom) roomManager).leaveRoom(user);
				}
				exit = true;
				break;
			}
			case NCMessage.OP_KICKED:
				exit = true;
				break;
			case NCMessage.OP_KICK:
			{
				String usuario_kick = ((NCRoomMessage) message).getName();
				if(roomManager instanceof NCRoom) {
					if(((NCRoom) roomManager).getAdmin().contains(user)) {
					((NCRoom) roomManager).sendKick(user,usuario_kick);
					serverManager.kickRoom(user,usuario_kick, currentRoom);
					}
					else {
						((NCRoom) roomManager).send(user, NCMessage.OP_SEND, "No posees los permisos necesarios para echar a un usuario.");
					}
				}
				break;
			}
			case NCMessage.OP_PROMOTE:
			{
				String usuario= ((NCRoomMessage) message).getName();
				if(roomManager instanceof NCRoom) {
					serverManager.promoteUser(currentRoom, user, usuario);
					((NCRoom) roomManager).send(usuario, NCMessage.OP_SEND, "¡Felicidades! " + user + " te ha concedido permisos de administrador.");
				}
				break;
			}
			case NCMessage.OP_ROOM_INFO:
			{
				dos.writeUTF(NCMessage.makeRoomMessage(NCMessage.OP_ROOM_INFO, roomManager.getDescription().toPrintableString()).toEncodedString());
				break;
			}
			case NCMessage.OP_RENAME:
			{
				String new_name = ((NCRoomMessage) message).getName();
				if(serverManager.renameRoom(currentRoom, new_name,user)) {
					dos.writeUTF(NCMessage.makeOpMessage(NCMessage.OP_OK).toEncodedString());
					((NCRoom) roomManager).rename(new_name);
				} else {
					dos.writeUTF(NCMessage.makeOpMessage(NCMessage.OP_FAILURE).toEncodedString());
				}
				break;
			}
			case NCMessage.OP_RENAMED:
			{
				currentRoom = ((NCRoomMessage) message).getName();
				break;
			}
			case NCMessage.OP_WHISP:
			{
				((NCRoom) roomManager).sendWhisp(user, ((NCWhispMessage) message).getUser(), ((NCWhispMessage) message).getWhisp());
				break;
			}
			}
		}
	}
}
