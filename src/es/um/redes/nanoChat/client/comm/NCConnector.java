package es.um.redes.nanoChat.client.comm;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.List;

import es.um.redes.nanoChat.messageML.NCListMessage;
import es.um.redes.nanoChat.messageML.NCMessage;
import es.um.redes.nanoChat.messageML.NCOpMessage;
import es.um.redes.nanoChat.messageML.NCRoomMessage;
import es.um.redes.nanoChat.server.roomManager.NCRoomDescription;

//Esta clase proporciona la funcionalidad necesaria para intercambiar mensajes entre el cliente y el servidor de NanoChat
public class NCConnector {
	private Socket socket;
	protected DataOutputStream dos;
	protected DataInputStream dis;
	
	public NCConnector(InetSocketAddress serverAddress) throws UnknownHostException, IOException {
		// Se crea el socket a partir de la dirección proporcionada
		socket = new Socket(serverAddress.getAddress(), serverAddress.getPort());
		// Se extraen los streams de entrada y salida
		dos = new DataOutputStream(socket.getOutputStream());
		dis = new DataInputStream(socket.getInputStream());
	}


	//Método para registrar el nick en el servidor. Nos informa sobre si la inscripción se hizo con éxito o no.
	public boolean registerNickname_UnformattedMessage(String nick) throws IOException {
		//Funcionamiento resumido: SEND(nick) and RCV(NICK_OK) or RCV(NICK_DUPLICATED)
		//Enviamos una cadena con el nick por el flujo de salida
		dos.writeUTF(nick);
		//Leemos la cadena recibida como respuesta por el flujo de entrada
		String response = dis.readUTF();
		//Si la cadena recibida es NICK_OK entonces no está duplicado (en función de ello modificar el return)
		if(response.equals("NICK_OK")){
			return true;
		}
		return false;
	}

	
	//Método para registrar el nick en el servidor. Nos informa sobre si la inscripción se hizo con éxito o no.
	public boolean registerNickname(String nick) throws IOException {
		//Funcionamiento resumido: SEND(nick) and RCV(NICK_OK) or RCV(NICK_DUPLICATED)
		//Creamos un mensaje de tipo RoomMessage con opcode OP_NICK en el que se inserte el nick
		NCRoomMessage message = NCMessage.makeRoomMessage(NCMessage.OP_NICK, nick);
		//Obtenemos el mensaje de texto listo para enviar
		String rawMessage = message.toEncodedString();
		//Escribimos el mensaje en el flujo de salida, es decir, provocamos que se envíe por la conexión TCP
		dos.writeUTF(rawMessage);
		//Leemos el mensaje recibido como respuesta por el flujo de entrada
		NCOpMessage response = (NCOpMessage) NCMessage.readMessageFromSocket(dis);
		//Analizamos el mensaje para saber si está duplicado el nick (modificar el return en consecuencia)
		if(response.getOpcode() == NCMessage.OP_OK) {
			return true;
		} else return false;
		
	}
	
	//Método para obtener la lista de salas del servidor
	public List<NCRoomDescription> getRooms() throws IOException {
		//Funcionamiento resumido: SND(GET_ROOMS) and RCV(ROOM_LIST)
		NCOpMessage message = NCMessage.makeOpMessage(NCMessage.OP_ROOM_LIST);
		String rawMessage = message.toEncodedString();
		dos.writeUTF(rawMessage);
		NCListMessage response = (NCListMessage)NCMessage.readMessageFromSocket(dis);
		return response.getLista();
	}
	
	//Método para solicitar la entrada en una sala
	public boolean enterRoom(String room) throws IOException {
		//Funcionamiento resumido: SND(ENTER_ROOM<room>) and RCV(IN_ROOM) or RCV(REJECT)
		// completar el método
		NCRoomMessage message = NCMessage.makeRoomMessage(NCMessage.OP_ENTER, room);
		dos.writeUTF(message.toEncodedString());
		NCMessage response = NCMessage.readMessageFromSocket(dis);
		if(response.getOpcode() == NCMessage.OP_OK) {
			return true;
		} else if(response.getOpcode() == NCMessage.OP_FAILURE) {
			return false;
		}
		return false;
	}
	
	//Metodo para kickear a un usuario
	public void kickUser(String u) throws IOException {
		dos.writeUTF(NCMessage.makeRoomMessage(NCMessage.OP_KICK, u).toEncodedString());
	}
	
	public void forceExitRoom(String u) throws IOException {
		dos.writeUTF(NCMessage.makeRoomMessage(NCMessage.OP_KICKED, u).toEncodedString());
	}
	
	//Metodo para promotear un user a admin
	public void promoteUser(String u) throws IOException {
		dos.writeUTF(NCMessage.makeRoomMessage(NCMessage.OP_PROMOTE, u).toEncodedString());	
	}
	//Método para salir de una sala
	public void leaveRoom(String room) throws IOException {
		//Funcionamiento resumido: SND(EXIT_ROOM)
		dos.writeUTF(NCMessage.makeOpMessage(NCMessage.OP_EXIT).toEncodedString());
	}
	
	//Método que utiliza el Shell para ver si hay datos en el flujo de entrada
	public boolean isDataAvailable() throws IOException {
		return (dis.available() != 0);
	}
	
	//IMPORTANTE!!
	// Es necesario implementar métodos para recibir y enviar mensajes de chat a una sala
	
	public void sendMessage(String message) {
		try {
			dos.writeUTF(NCMessage.makeRoomMessage(NCMessage.OP_SEND, message).toEncodedString());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public NCRoomMessage recieveMessage() {
		try {
			NCRoomMessage message = (NCRoomMessage) NCMessage.readMessageFromSocket(dis);
			return message;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	//Método para pedir la descripción de una sala
	public NCRoomDescription getRoomInfo(String room) throws IOException {
		//Funcionamiento resumido: SND(GET_ROOMINFO) and RCV(ROOMINFO)
		// Construimos el mensaje de solicitud de información de la sala específica
		NCRoomMessage message = NCMessage.makeRoomMessage(NCMessage.OP_ROOM_INFO, room);
		dos.writeUTF(message.toEncodedString());
		// Recibimos el mensaje de respuesta
		NCRoomMessage response = (NCRoomMessage) NCMessage.readMessageFromSocket(dis);
		// Devolvemos la descripción contenida en el mensaje
		return response.getRoomInfo();
	}
	
	//Método para cerrar la comunicación con la sala
	// (Opcional) Enviar un mensaje de salida del servidor de Chat
	public void disconnect() {
		try {
			if (socket != null) {
				socket.close();
			}
		} catch (IOException e) {
		} finally {
			socket = null;
		}
	}
	
	public boolean createRoom(String name) throws IOException {
		dos.writeUTF(NCMessage.makeRoomMessage(NCMessage.OP_CREATE_ROOM, name).toEncodedString());
		NCOpMessage response = (NCOpMessage) NCMessage.readMessageFromSocket(dis);
		if(response.getOpcode() == NCMessage.OP_OK) {
			return true;
		} else {
			return false;
		}
	}
	
	public boolean renameRoom(String name) throws IOException{
		dos.writeUTF(NCMessage.makeRoomMessage(NCMessage.OP_RENAME, name).toEncodedString());
		NCOpMessage response = (NCOpMessage) NCMessage.readMessageFromSocket(dis);
		if(response.getOpcode() == NCMessage.OP_OK) {
			return true;
		} else {
			return false;
		}
	}
	
	public boolean sendWhisp(String name, String message) throws IOException {
		dos.writeUTF(NCMessage.makeWhispMessage(NCMessage.OP_WHISP, name, message).toEncodedString());
		NCOpMessage response = (NCOpMessage) NCMessage.readMessageFromSocket(dis);
		if(response.getOpcode() == NCMessage.OP_SEND) {
			return true;
		} else {
			return false;
		}
	}
	
	public void renamed(String name) throws IOException {
		dos.writeUTF(NCMessage.makeRoomMessage(NCMessage.OP_RENAMED, name).toEncodedString());
	}

}
