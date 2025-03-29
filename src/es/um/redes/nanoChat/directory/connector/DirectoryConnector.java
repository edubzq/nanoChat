package es.um.redes.nanoChat.directory.connector;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;


/**
 * Cliente con métodos de consulta y actualización específicos del directorio
 */
public class DirectoryConnector {
	// Tamaño máximo del paquete UDP (los mensajes intercambiados son muy cortos)
	private static final int PACKET_MAX_SIZE = 128;
	// Puerto en el que atienden los servidores de directorio
	private static final int DEFAULT_PORT = 6868;
	// Valor del TIMEOUT
	private static final int TIMEOUT = 1000;
	// Opcodes
	private static final byte OPCODE_REGISTRATION = 1;
	private static final byte OPCODE_QUERY = 2;
	private static final byte OPCODE_OK = 3;

	private DatagramSocket socket; // socket UDP
	private InetSocketAddress directoryAddress; // dirección del servidor de directorio

	public DirectoryConnector(String agentAddress) throws IOException {
		// A partir de la dirección y del puerto generar la dirección de conexión
		// para el Socket
		directoryAddress = new InetSocketAddress(InetAddress.getByName(agentAddress), DEFAULT_PORT);

		// Crear el socket UDP
		socket = new DatagramSocket();
		
		/*
		byte[] buf = new byte[PACKET_MAX_SIZE];
		DatagramPacket pckt = new DatagramPacket(buf, buf.length, directoryAddress);
		byte[] res = new byte[PACKET_MAX_SIZE];
		DatagramPacket pcktRes = new DatagramPacket(res, res.length);
		
		System.out.println("Conectando con el servidor...");
		boolean response = false;
		while (!response) {
			socket.send(pckt);
			try {
				socket.setSoTimeout(TIMEOUT);
				socket.receive(pcktRes);
			} catch (SocketTimeoutException e) {
				continue;
			}
			response = true;
		}
		System.out.println("Respuesta recibida.");
		*/
	}

	/**
	 * Envía una solicitud para obtener el servidor de chat asociado a un
	 * determinado protocolo
	 * 
	 */
	public InetSocketAddress getServerForProtocol(int protocol) throws IOException {
		// Generar el mensaje de consulta llamando a buildQuery()
		byte[] msg = buildQuery(protocol);
		// Construir el datagrama con la consulta
		DatagramPacket pckt = new DatagramPacket(msg, msg.length, directoryAddress);
		// Preparar el buffer para la respuesta
		byte[] res = new byte[PACKET_MAX_SIZE];
		DatagramPacket pcktRes = new DatagramPacket(res, res.length);
		// Enviar datagrama por el socket
		boolean response = false;
		int contador = 0;
		while (!response && contador < 6) {
			socket.send(pckt);
			// Establecer el temporizador para el caso en que no haya respuesta
			try {
				// Recibir la respuesta
				socket.setSoTimeout(TIMEOUT);
				socket.receive(pcktRes);
				response = true;
			} catch (SocketTimeoutException e) {
				contador++;
				continue;
			}
		}
		if (contador == 6) System.out.println("Número de reintentos agotado");
		// Procesamos la respuesta para devolver la dirección que hay en ella
		return getAddressFromResponse(pcktRes);
	}

	// Método para generar el mensaje de consulta (para obtener el servidor asociado
	// a un protocolo)
	private byte[] buildQuery(int protocol) {
		// Devolvemos el mensaje codificado en binario según el formato acordado
		ByteBuffer msg = ByteBuffer.allocate(5);
		msg.put(OPCODE_QUERY);
		msg.putInt(protocol);
		return msg.array();
	}

	// Método para obtener la dirección de internet a partir del mensaje UDP de
	// respuesta
	private InetSocketAddress getAddressFromResponse(DatagramPacket packet) throws UnknownHostException {
		ByteBuffer msg = ByteBuffer.wrap(packet.getData());
		if (msg.array().length == 0) {
			// Analizar si la respuesta no contiene dirección (devolver null)
			return null;
		} else {
			// Si la respuesta no está vacía, devolver la dirección (extraerla del
			// mensaje)
			byte[] dir = new byte[4];
			msg = msg.get(dir, 0, 4);
			int port = msg.getInt(4);
			InetSocketAddress serverAddress = new InetSocketAddress(InetAddress.getByAddress(dir), port);
			return serverAddress;
		}
	}

	/**
	 * Envía una solicitud para registrar el servidor de chat asociado a un
	 * determinado protocolo
	 * 
	 */
	public boolean registerServerForProtocol(int protocol, int port) throws IOException {
		// Construir solicitud de registro (buildRegistration)
		byte[] msg = buildRegistration(protocol, port);
		DatagramPacket pckt = new DatagramPacket(msg, msg.length, directoryAddress);
		byte[] res = new byte[PACKET_MAX_SIZE];
		DatagramPacket pcktRes = new DatagramPacket(res, res.length);

		// Enviar solicitud
		boolean response = false;
		while (!response) {
			socket.send(pckt);
			// Recibe respuesta
			try {
				socket.setSoTimeout(TIMEOUT);
				socket.receive(pcktRes);
				response = true;
			} catch (SocketTimeoutException e) {
				continue;
			}
		}

		// Procesamos la respuesta para ver si se ha podido registrar correctamente
		ByteBuffer ret = ByteBuffer.wrap(res);
		byte opcode = ret.get();
		if(opcode == OPCODE_OK) {
			return true;
		} else {
			return false;
		}
	}

	// Método para construir una solicitud de registro de servidor
	// OJO: No hace falta proporcionar la dirección porque se toma la misma desde la
	// que se envió el mensaje
	private byte[] buildRegistration(int protocol, int port) {
		// Devolvemos el mensaje codificado en binario según el formato acordado
		ByteBuffer msg = ByteBuffer.allocate(9);
		msg.put(OPCODE_REGISTRATION);
		msg.putInt(protocol);
		msg.putInt(port);
		return msg.array();
	}

	public void close() {
		socket.close();
	}

	public static void main(String[] args) {
		System.out.println("Directory connector starting...");
	}
}
