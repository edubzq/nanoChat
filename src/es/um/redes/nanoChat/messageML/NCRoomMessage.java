package es.um.redes.nanoChat.messageML;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import es.um.redes.nanoChat.server.roomManager.NCRoomDescription;

/*
 * ROOM
----

<message>
<operation>operation</operation>
<name>name</name>
</message>

Operaciones válidas:

NICK
*/


public class NCRoomMessage extends NCMessage {

	private String name;
	
	//Constantes asociadas a las marcas específicas de este tipo de mensaje
	private static final String RE_NAME = "<name>(.*?)</name>";
	private static final String NAME_MARK = "name";
	private static final String RE_ROOM_INFO = "Room Name: (.*?)\t Members \\((.*?)\\) : (.*)\tLast message: (.*)";


	/**
	 * Creamos un mensaje de tipo Room a partir del código de operación y del nombre
	 */
	public NCRoomMessage(byte opcode, String name) {
		this.opcode = opcode;
		this.name = name;
	}

	@Override
	//Pasamos los campos del mensaje a la codificación correcta en lenguaje de marcas
	public String toEncodedString() {
		StringBuffer sb = new StringBuffer();
		
		sb.append("<"+MESSAGE_MARK+">"+END_LINE);
		sb.append("<"+OPERATION_MARK+">"+opcodeToString(opcode)+"</"+OPERATION_MARK+">"+END_LINE); //Construimos el campo
		sb.append("<"+NAME_MARK+">"+name+"</"+NAME_MARK+">"+END_LINE);
		sb.append("</"+MESSAGE_MARK+">"+END_LINE);

		return sb.toString(); //Se obtiene el mensaje

	}


	//Parseamos el mensaje contenido en message con el fin de obtener los distintos campos
	public static NCRoomMessage readFromString(byte code, String message) {
		String found_name = null;

		// Tienen que estar los campos porque el mensaje es de tipo RoomMessage
		Pattern pat_name = Pattern.compile(RE_NAME);
		Matcher mat_name = pat_name.matcher(message);
		if (mat_name.find()) {
			// Name found
			found_name = mat_name.group(1);
		} else {
			System.out.println("Error en RoomMessage: no se ha encontrado parametro.");
			return null;
		}
		
		return new NCRoomMessage(code, found_name);
	}


	//Devolvemos el nombre contenido en el mensaje
	public String getName() {
		return name;
	}
	
	public NCRoomDescription getRoomInfo() {
		Pattern pat_room_info = Pattern.compile(RE_ROOM_INFO);
		Matcher mat_room_info = pat_room_info.matcher(name);
		if(mat_room_info.find()) {
			String name = mat_room_info.group(1);
			String members = mat_room_info.group(3);
			String lastMessage = mat_room_info.group(4);
			ArrayList<String> membersArray = new ArrayList<String>(); 
			if(members != "") {
				for(String s : members.split(" ")) {
					membersArray.add(s);
				}
			}
			long timeLastMessage = 0;
			if(!lastMessage.equals("not yet")) {
				SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss - dd MMM yyyy");
				try {
					Date date = df.parse(lastMessage);
					timeLastMessage = date.getTime(); 
				} catch (ParseException e) {
					e.printStackTrace();
				}
			}
			return new NCRoomDescription(name, membersArray, timeLastMessage);
		}
		return null;
	}

}
