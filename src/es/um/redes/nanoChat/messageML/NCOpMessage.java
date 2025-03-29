package es.um.redes.nanoChat.messageML;

/*
 * OPERATION
----

<message>
<operation>operation</operation>
</message>

Operaciones válidas:

NICK_OK
NICK_DULPICATED
*/

public class NCOpMessage extends NCMessage {
	
	public NCOpMessage(byte code) {
		this.opcode = code;
	}
	
	@Override
	public String toEncodedString() {
		StringBuffer sb = new StringBuffer();
		sb.append("<"+MESSAGE_MARK+">"+END_LINE);
		sb.append("<"+OPERATION_MARK+">"+opcodeToString(opcode)+"</"+OPERATION_MARK+">"+END_LINE);
		sb.append("</"+MESSAGE_MARK+">"+END_LINE);
		return sb.toString();
	}

}
