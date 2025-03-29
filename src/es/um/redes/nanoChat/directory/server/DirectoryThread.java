package es.um.redes.nanoChat.directory.server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

// Declarar constantes los opcode y cambiar los valores para que sean
// únicos entre DirectoryThread y DirectoryConnector (Aunque no habría problema)
// pero por simplicidad en la memoria.

public class DirectoryThread extends Thread {

	// Tamaño máximo del paquete UDP
	private static final int PACKET_MAX_SIZE = 128;

	// Estructura para guardar las asociaciones ID_PROTOCOLO -> Dirección del
	// servidor
	protected Map<Integer, InetSocketAddress> servers;

	// Socket de comunicación UDP
	protected DatagramSocket socket;

	// Probabilidad de descarte del mensaje
	protected double messageDiscardProbability;

	// Opcodes
	private static final byte OPCODE_REGISTRATION = 1;
	private static final byte OPCODE_QUERY = 2;
	private static final byte OPCODE_OK = 3;

	public DirectoryThread(String name, int directoryPort, double corruptionProbability) throws SocketException {
		super(name);
		InetSocketAddress serverAddress = new InetSocketAddress(directoryPort);
		socket = new DatagramSocket(serverAddress);
		messageDiscardProbability = corruptionProbability;
		// Inicialización del mapa
		servers = new HashMap<Integer, InetSocketAddress>();
	}

	@Override
	public void run() {
		byte[] buf = new byte[PACKET_MAX_SIZE];

		System.out.println("Directory starting...");
		boolean running = true;
		while (running) {

			// 1) Recibir la solicitud por el socket
			DatagramPacket pckt = new DatagramPacket(buf, PACKET_MAX_SIZE);
			try {
				socket.receive(pckt);
			} catch (IOException e) {
				e.printStackTrace();
			}

			// 2) Extraer quién es el cliente (su dirección)
			InetSocketAddress clientAddress = (InetSocketAddress) pckt.getSocketAddress();

			// 3) Vemos si el mensaje debe ser descartado por la probabilidad de descarte
			double rand = Math.random();
			if (rand < messageDiscardProbability) {
				System.err.println("Directory DISCARDED corrupt request from... ");
				continue;
			}

			// 4) Analizar y procesar la solicitud (llamada a processRequestFromCLient)
			// 5) Tratar las excepciones que puedan producirse
			try {
				processRequestFromClient(buf, clientAddress);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		socket.close();
	}

	// Método para procesar la solicitud enviada por clientAddr
	public void processRequestFromClient(byte[] data, InetSocketAddress clientAddr) throws IOException {

		// 1) Extraemos el tipo de mensaje recibido
		ByteBuffer msg = ByteBuffer.wrap(data);
		byte opcode = msg.get();
		if (opcode == OPCODE_REGISTRATION) {
			// 2) Procesar el caso de que sea un registro y enviar mediante sendOK
			int protocolo = msg.getInt();
			int puerto = msg.getInt();
			InetSocketAddress dir = new InetSocketAddress(clientAddr.getAddress(), puerto);
			servers.put(protocolo, dir);
			// System.out.println("protocolo = "+protocolo+", dir = "+dir);
			sendOK(clientAddr);
		} else if (opcode == OPCODE_QUERY) {
			// 3) Procesar el caso de que sea una consulta
			int protocolo = msg.getInt();
			if (servers.containsKey(protocolo)) {
				// 3.1) Devolver una dirección si existe un servidor (sendServerInfo)
				sendServerInfo(servers.get(protocolo), clientAddr);
			} else {
				// 3.2) Devolver una notificación si no existe un servidor (sendEmpty)
				sendEmpty(clientAddr);
			}
		}
	}

	// Método para enviar una respuesta vacía (no hay servidor)
	private void sendEmpty(InetSocketAddress clientAddr) throws IOException {
		// Construir respuesta
		byte[] msg = new byte[0];
		DatagramPacket pckt = new DatagramPacket(msg, msg.length, clientAddr);
		// Enviar respuesta
		try {
			socket.send(pckt);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// Método para enviar la dirección del servidor al cliente
	private void sendServerInfo(InetSocketAddress serverAddress, InetSocketAddress clientAddr) throws IOException {
		// Obtener la representación binaria de la dirección
		byte[] dir = serverAddress.getAddress().getAddress();
		int port = serverAddress.getPort();
		// Construir respuesta
		ByteBuffer msg = ByteBuffer.allocate(dir.length + Integer.SIZE);
		msg.put(dir);
		msg.putInt(port);
		DatagramPacket pckt = new DatagramPacket(msg.array(), msg.array().length, clientAddr);
		// Enviar respuesta
		try {
			socket.send(pckt);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// Método para enviar la confirmación del registro
	private void sendOK(InetSocketAddress clientAddr) throws IOException {
		// Construir respuesta
		ByteBuffer msg = ByteBuffer.allocate(1);
		msg.put(OPCODE_OK);
		DatagramPacket pckt = new DatagramPacket(msg.array(), msg.array().length, clientAddr);
		// Enviar respuesta
		try {
			socket.send(pckt);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
