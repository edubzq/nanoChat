package es.um.redes.nanoChat.messageML;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 * PRIVATE
----

<message>
<operation>operation</operation>
<user>user</user>
<message>message</message>
</message>

Operaciones válidas:

WHISP
*/


public class NCWhispMessage extends NCMessage {
	
	String user;
	String whisp;
	
	private static final String RE_USER = "<user>(.*?)</user>";
	private static final String USER_MARK = "user";
	private static final String RE_WHISP = "<whisp>(.*?)</whisp>";
	private static final String WHISP_MARK = "whisp";
	
	public NCWhispMessage(byte opcode, String user, String whisp) {
		this.opcode = opcode;
		this.user = user;
		this.whisp = whisp;
	}
	
	@Override
	//Pasamos los campos del mensaje a la codificación correcta en lenguaje de marcas
	public String toEncodedString() {
		StringBuffer sb = new StringBuffer();
		
		sb.append("<"+MESSAGE_MARK+">"+END_LINE);
		sb.append("<"+OPERATION_MARK+">"+opcodeToString(opcode)+"</"+OPERATION_MARK+">"+END_LINE); //Construimos el campo
		sb.append("<"+USER_MARK+">"+user+"</"+USER_MARK+">"+END_LINE);
		sb.append("<"+WHISP_MARK+">"+whisp+"</"+WHISP_MARK+">"+END_LINE);
		sb.append("</"+MESSAGE_MARK+">"+END_LINE);

		return sb.toString(); //Se obtiene el mensaje

	}
	
	public static NCWhispMessage readFromString(byte code, String message) {
		String user = null;
		String whisp = null;
		Pattern pat_name = Pattern.compile(RE_USER);
		Pattern pat_whisp = Pattern.compile(RE_WHISP);
		Matcher mat_name = pat_name.matcher(message);
		Matcher mat_whisp = pat_whisp.matcher(message);
		if (mat_name.find()) {
			// Name found
			user = mat_name.group(1);
		} else {
			System.out.println("Error en RoomMessage: no se ha encontrado parametro.");
			return null;
		}
		if (mat_whisp.find()) {
			// Name found
			whisp = mat_whisp.group(1);
		} else {
			System.out.println("Error en RoomMessage: no se ha encontrado parametro.");
			return null;
		}
		
		return new NCWhispMessage(code, user, whisp);
	}
	
	public String getUser() {
		return user;
	}
	
	public String getWhisp() {
		return whisp;
	}
	
}
