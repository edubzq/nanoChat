package es.um.redes.nanoChat.messageML;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import es.um.redes.nanoChat.server.roomManager.NCRoomDescription;


public abstract class NCMessage {
	protected byte opcode;

	// IMPLEMENTAR TODAS LAS CONSTANTES RELACIONADAS CON LOS CODIGOS DE OPERACION
	public static final byte OP_INVALID_CODE = 0;
	public static final byte OP_OK = 1;
	public static final byte OP_FAILURE = 2;
	public static final byte OP_NICK = 3;
	public static final byte OP_NICK_DUPLICATED = 4;
	public static final byte OP_ROOM_LIST = 5;
	public static final byte OP_LIST = 6;
	public static final byte OP_ENTER = 7;
	public static final byte OP_SEND = 8;
	public static final byte OP_EXIT = 9;
	public static final byte OP_ROOM_INFO = 10;
	public static final byte OP_CREATE_ROOM = 11;
	public static final byte OP_RENAME = 12;
	public static final byte OP_WHISP = 13;
	public static final byte OP_RENAMED = 14;
	public static final byte OP_KICK = 15;
	public static final byte OP_KICKED = 16;
	public static final byte OP_PROMOTE = 17;
	public static final byte OP_PROMOTED = 18;
	
	
	public static final char DELIMITER = ':';    //Define el delimitador
	public static final char END_LINE = '\n';    //Define el carácter de fin de línea
	
	public static final String OPERATION_MARK = "operation";
	public static final String MESSAGE_MARK = "message";

	/**
	 * Códigos de los opcodes válidos  El orden
	 * es importante para relacionarlos con la cadena
	 * que aparece en los mensajes
	 */
	private static final Byte[] _valid_opcodes = { 
		OP_OK,
		OP_FAILURE,
		OP_NICK,
		OP_NICK_DUPLICATED,
		OP_ROOM_LIST,
		OP_LIST,
		OP_ENTER,
		OP_SEND,
		OP_EXIT,
		OP_ROOM_INFO,
		OP_CREATE_ROOM,
		OP_RENAME,
		OP_WHISP,
		OP_RENAMED,
		OP_KICK,
		OP_PROMOTE,
		OP_KICKED,
		OP_PROMOTED
	};

	/**
	 * cadena exacta de cada orden
	 */
	private static final String[] _valid_operations_str = {
		"Ok",
		"Failure",
		"Nick",
		"NickDuplicated",
		"RoomList",
		"List",
		"Enter",
		"Send",
		"Exit",
		"RoomInfo",
		"Create",
		"Rename",
		"Whisp",
		"Renamed",
		"Kick",
		"Promote",
		"Kicked",
		"Promoted"
	};

	private static Map<String, Byte> _operation_to_opcode;
	private static Map<Byte, String> _opcode_to_operation;
	
	static {
		_operation_to_opcode = new TreeMap<>();
		_opcode_to_operation = new TreeMap<>();
		for (int i = 0 ; i < _valid_operations_str.length; ++i)
		{
			_operation_to_opcode.put(_valid_operations_str[i].toLowerCase(), _valid_opcodes[i]);
			_opcode_to_operation.put(_valid_opcodes[i], _valid_operations_str[i]);
		}
	}
	
	/**
	 * Transforma una cadena en el opcode correspondiente
	 */
	protected static byte stringToOpcode(String opStr) {
		return _operation_to_opcode.getOrDefault(opStr.toLowerCase(), OP_INVALID_CODE);
	}

	/**
	 * Transforma un opcode en la cadena correspondiente
	 */
	protected static String opcodeToString(byte opcode) {
		return _opcode_to_operation.getOrDefault(opcode, null);
	}
	
	//Devuelve el opcode del mensaje
	public byte getOpcode() {
		return opcode;
	}

	//Método que debe ser implementado por cada subclase de NCMessage
	protected abstract String toEncodedString();

	//Analiza la operación de cada mensaje y usa el método readFromString() de cada subclase para parsear
	public static NCMessage readMessageFromSocket(DataInputStream dis) throws IOException {
		String message = dis.readUTF();
		String regexpr = "<"+MESSAGE_MARK+">(.*?)</"+MESSAGE_MARK+">";
		Pattern pat = Pattern.compile(regexpr,Pattern.DOTALL);
		Matcher mat = pat.matcher(message);
		if (!mat.find()) {
			System.out.println("Mensaje mal formado:\n"+message);
			return null;
			// Message not found
		} 
		String inner_msg = mat.group(1);  // extraemos el mensaje

		String regexpr1 = "<"+OPERATION_MARK+">(.*?)</"+OPERATION_MARK+">";
		Pattern pat1 = Pattern.compile(regexpr1);
		Matcher mat1 = pat1.matcher(inner_msg);
		if (!mat1.find()) {
			System.out.println("Mensaje mal formado:\n" +message);
			return null;
			// Operation not found
		} 
		String operation = mat1.group(1);  // extraemos la operación
		
		byte code = stringToOpcode(operation);
		if (code == OP_INVALID_CODE) return null;
		
		switch (code) {
		// Parsear el resto de mensajes 
		case OP_OK:
		{
			return new NCOpMessage(code);
		}
		case OP_FAILURE:
		{
			return new NCOpMessage(code);
		}
		case OP_NICK:
		{
			return NCRoomMessage.readFromString(code, inner_msg);
		}
		case OP_NICK_DUPLICATED:
		{	
			return new NCOpMessage(code);
		}
		case OP_ROOM_LIST:
		{
			return new NCOpMessage(code);
		}
		case OP_LIST:
		{
			return NCListMessage.readFromString(code, inner_msg);
		}
		case OP_ENTER:
		{
			return NCRoomMessage.readFromString(code, inner_msg);
		}
		case OP_SEND:
		{
			return NCRoomMessage.readFromString(code, inner_msg);
		}
		case OP_KICK:
		{
			return NCRoomMessage.readFromString(code, inner_msg);
		}
		case OP_KICKED:
		{
			return NCRoomMessage.readFromString(code, inner_msg);
		}
		case OP_PROMOTE:
		{
			return NCRoomMessage.readFromString(code, inner_msg);
		}
		case OP_EXIT:
		{
			return new NCOpMessage(code);
		}
		case OP_ROOM_INFO:
		{
			return NCRoomMessage.readFromString(code, inner_msg);
		}
		case OP_CREATE_ROOM:
		{
			return NCRoomMessage.readFromString(code, inner_msg);
		}
		case OP_RENAME:
		{
			return NCRoomMessage.readFromString(code, inner_msg);
		}
		case OP_WHISP:
		{
			return NCWhispMessage.readFromString(code, inner_msg);
		}
		case OP_RENAMED:
			return NCRoomMessage.readFromString(code, inner_msg);
			
		
		default:
			System.err.println("Unknown message type received:" + code);
			return null;
		}

	}

	// Programar el resto de métodos para crear otros tipos de mensajes
	
	public static NCRoomMessage makeRoomMessage(byte code, String room) {
		return new NCRoomMessage(code, room);
	}
	
	public static NCOpMessage makeOpMessage(byte code) {
		return new NCOpMessage(code);
	}
	
	public static NCListMessage makeListMessage(byte code, ArrayList<NCRoomDescription> lista) {
		return new NCListMessage(code, lista);
	}
	
	public static NCWhispMessage makeWhispMessage(byte code, String user, String whisp) {
		return new NCWhispMessage(code, user, whisp);
	}
}
