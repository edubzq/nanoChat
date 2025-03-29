package es.um.redes.nanoChat.messageML;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import es.um.redes.nanoChat.server.roomManager.NCRoomDescription;

/*
 * LIST
----

<message>
<operation>operation</operation>
<item>Description</item>
...
...
...
<item>Description</item>
</message>

Operaciones válidas:

ROOM_LIST
*/


public class NCListMessage extends NCMessage {

	private ArrayList<NCRoomDescription> lista;
	
	private static final String RE_ITEM = "<item>(.*?)</item>";
	private static final String ITEM_MARK = "item";
	
	private static final String RE_ROOM_INFO = "Room Name: (.*?)\t Members \\((.*?)\\) : (.*)\tLast message: (.*)";
	
	public NCListMessage(byte opcode, ArrayList<NCRoomDescription> lista) {
		this.opcode = opcode;
		this.lista = lista;
	}
	
	@Override
	public String toEncodedString() {
		StringBuffer sb = new StringBuffer();
		sb.append("<"+MESSAGE_MARK+">"+END_LINE);
		sb.append("<"+OPERATION_MARK+">"+opcodeToString(opcode)+"</"+OPERATION_MARK+">"+END_LINE);
		for(NCRoomDescription rd : lista) {
			sb.append("<"+ITEM_MARK+">"+rd.toPrintableString()+"</"+ITEM_MARK+">"+END_LINE);
		}
		sb.append("</"+MESSAGE_MARK+">"+END_LINE);
		return sb.toString();
	}
	
	public static NCListMessage readFromString(byte code, String message) {
		ArrayList<NCRoomDescription> lista = new ArrayList<NCRoomDescription>();
		Pattern pat_item = Pattern.compile(RE_ITEM);
		Matcher mat_item = pat_item.matcher(message);
		Pattern pat_room_info = Pattern.compile(RE_ROOM_INFO);
		while(mat_item.find()) {
			String info = mat_item.group(1);
			Matcher mat_room_info = pat_room_info.matcher(info);
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
				
				lista.add(new NCRoomDescription(name, membersArray, timeLastMessage));
			}
		}
		return new NCListMessage(code, lista);
	}
	
	public ArrayList<NCRoomDescription> getLista() {
		ArrayList<NCRoomDescription> copia = new ArrayList<NCRoomDescription>();
		for(NCRoomDescription rd : lista) {
			ArrayList<String> copiaMiembros = new ArrayList<String>();
			for(String u : rd.members) {
				copiaMiembros.add(u);
			}
			copia.add(new NCRoomDescription(rd.roomName, copiaMiembros, rd.timeLastMessage));
		}
		return copia;
	}
}
