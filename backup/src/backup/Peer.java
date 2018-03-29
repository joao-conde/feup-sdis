package backup;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.Scanner;

import backup.Connection.MulticastChannel;
import backup.Message.ChunkNoException;
import backup.Message.MessageFields;
import backup.Message.MessageType;
import backup.Message.ReplicationDegreeOutOfLimitsException;

public class Peer implements Protocol {

	public static float BACKUP_PROTOCOL_VERSION = 1.0f;
	public final static int CHUNK_MAX_SIZE = 64000;
	public final static int MESSAGE_MAX_SIZE = CHUNK_MAX_SIZE + 150;
	public final static int STORED_WAIT_TIME = 400;
	public final static int PUT_CHUNK_MAX_TIMES = 5;
	public final static int INITIAL_PUT_CHUNK_WAIT_TIME = 1000;
	public final static String HASH_ALGORITHM = "SHA-256";
	public final static char SEPARATOR = ' ';
	public final static String STATE_FILE_NAME = "state";

	public int id;
	private Connection connection;
	private Dispatcher mcDispatcher;
	private Dispatcher mdbDispatcher;
	private Dispatcher mdrDispatcher;
	private Random randomGenerator = new Random(System.currentTimeMillis());
	private HashMap<String, ChunkInfo> chunkMap = new HashMap<>();

	private String pathToPeer;
	private String pathToPeerChunks;
	private String pathToPeerReceivedFiles;
	private CloseResources closeResources = new CloseResources();

	private Registry registry;

	public static class ChunkInfo {

		private int replicationDegree = 0;
		public HashSet<Integer> seeds = new HashSet<>();
		public int desiredReplicationDegree;
		public String chunkId;
		public String fileId;

		public ChunkInfo(int desiredReplicationDegree, int chunkNo, String fileId) {
			this(desiredReplicationDegree, chunkNo, fileId, new int[] {});
		}

		public ChunkInfo(int desiredReplicationDegree, int chunkNo, String fileId, int[] seeds) {
			this.desiredReplicationDegree = desiredReplicationDegree;
			this.chunkId = buildChunkId(chunkNo, fileId);
			this.fileId = fileId;

			for (int i : seeds) {
				this.addPeer(new Integer(i));
			}
		}

		public void addPeer(int peerId) {

			if (this.seeds.add(new Integer(peerId)))
				this.replicationDegree++;

		}

		public static String buildChunkId(int chunkNo, String fileId) {

			return fileId + "-" + chunkNo;

		}

		public static int getChunkNo(String chunkId) {

			String[] components = chunkId.split("-");
			return Integer.parseInt(components[1]);

		}

		public static String getFileId(String chunkId) {

			String[] components = chunkId.split("-");
			return components[0];

		}

	}

	private class MessageHandler implements Runnable {

		private Message message;
		private byte[] buffer;
		private MulticastChannel channel;

		public MessageHandler(byte[] buffer, MulticastChannel channel) {
			this.buffer = buffer;
			this.channel = channel;
		}

		@Override
		public void run() {

			try {

				this.message = Message.processMessage(this.buffer);

				if (id == this.message.getMessageFields().senderId)
					return;

				System.out.println("\n---------  Message Received at " + this.channel.getName() + " ---------\n"
						+ new String(message.getHeaderString()));

				switch (message.getMessageFields().messageType) {

				case PUTCHUNK:
					
					registerMySavedChunk(this.message, false);

					try {
						int delay = randomGenerator.nextInt(STORED_WAIT_TIME);
						Thread.sleep(delay);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}

					sendStored(this.message);

					break;
				
				case STORED:

					registerOtherSavedChunk(this.message);

					break;
				default:
					break;
				}

			}

			catch (NumberFormatException e) {
				e.printStackTrace();
			}

		}

	}

	private class Dispatcher implements Runnable {

		private ExecutorService threadPool = Executors.newFixedThreadPool(100);

		public MulticastChannel multicastChannel;

		public Dispatcher(MulticastChannel multicastChannel) {
			this.multicastChannel = multicastChannel;

		}

		@Override
		public void run() {

			while (true) {

				byte[] buffer = new byte[MESSAGE_MAX_SIZE];

				int realLength = this.multicastChannel.receiveMessage(buffer);

				if (realLength != buffer.length) {

					byte[] temp = buffer;

					buffer = new byte[realLength];

					System.arraycopy(temp, 0, buffer, 0, realLength);

				}

				Runnable handler = new MessageHandler(buffer, this.multicastChannel);

				threadPool.execute(handler);

			}

		}

	}

	private class CloseResources implements Runnable {

		@Override
		public void run() {

			try {

				PrintWriter pw = new PrintWriter(new File(Peer.this.pathToPeer + "/" + STATE_FILE_NAME));

				BiConsumer<String, ChunkInfo> action = new BiConsumer<String, Peer.ChunkInfo>() {

					@Override
					public void accept(String chunkId, ChunkInfo chunkInfo) {

						String[] comp = chunkId.split("-");

						pw.print(comp[1]);
						pw.print(SEPARATOR);
						pw.print(chunkInfo.replicationDegree);
						pw.print(SEPARATOR);
						for (Integer i : chunkInfo.seeds) {
							pw.print(i);
							pw.print(SEPARATOR);

						}

						pw.print(SEPARATOR);
						pw.print(chunkInfo.desiredReplicationDegree);
						pw.print(SEPARATOR);
						pw.println(chunkInfo.fileId);

					}

				};

				Peer.this.chunkMap.forEach(action);

				pw.close();

				Peer.this.registry.unbind(Protocol.PROTOCOL + "-" + Peer.this.id);

			} catch (FileNotFoundException | RemoteException | NotBoundException e) {
			}

		}

	}

	public static void main(String[] args) {

		System.setProperty("java.net.preferIPv4Stack", "true");

		Peer peer = new Peer(Integer.parseInt(args[0]));

		System.out.println("------ Peer " + args[0] + " INITIATED -------");

		Runtime.getRuntime().addShutdownHook(new Thread(peer.closeResources));

		/*if(peer.id == 1)
			peer.backup("pic.jpg", 3, "3/3/3");
			System.out.println(Utils.hashString("pic.jpg" + "-" + "3/3/3", HASH_ALGORITHM));
		*/

		if(peer.id == 3) peer.delete("pic.jpg", "3/3/3");
	}

	public Peer(int id) {

		this.id = id;

		connection = new Connection("224.0.0.1", 2300, "224.0.0.2", 2301, "224.0.0.3", 2302);
		mcDispatcher = new Dispatcher(connection.getMC());
		mdbDispatcher = new Dispatcher(connection.getMDB());
		mdrDispatcher = new Dispatcher(connection.getMDR());

		Thread mcDispatcherThread = new Thread(this.mcDispatcher);
		Thread mdbDispatcherThread = new Thread(this.mdbDispatcher);
		Thread mdrDispatcherThread = new Thread(this.mdrDispatcher);
		mcDispatcherThread.start();
		mdbDispatcherThread.start();
		mdrDispatcherThread.start();

		this.pathToPeer = "../res/peer-" + id;
		this.pathToPeerChunks = pathToPeer + "/chunks";
		this.pathToPeerReceivedFiles = pathToPeer + "/inbox";

		new File(this.pathToPeerChunks).mkdirs();
		new File(this.pathToPeerReceivedFiles).mkdir();

		this.loadChunksTable();

		try {

			registry = LocateRegistry.createRegistry(Registry.REGISTRY_PORT);

		}

		catch (RemoteException e) {
		}

		try {

			registry = LocateRegistry.getRegistry();
			Protocol stub = (Protocol) UnicastRemoteObject.exportObject(this, 0);
			registry.bind(Protocol.PROTOCOL + "-" + id, stub);

		} catch (AlreadyBoundException | RemoteException e) {

			e.printStackTrace();
		}

	}

	private void sendStored(Message putChunkMessage) {

		saveChunkToDisk(putChunkMessage);

		String chunkId = ChunkInfo.buildChunkId(putChunkMessage.getMessageFields().chunkNo,
				putChunkMessage.getMessageFields().fileId);

		if (!chunkMap.get(chunkId).seeds.contains(new Integer(id)))
			return;

		try {
			Message reply = Message.buildMessage(new MessageFields(MessageType.STORED, BACKUP_PROTOCOL_VERSION, id,
					putChunkMessage.getMessageFields().fileId, putChunkMessage.getMessageFields().chunkNo));
			Peer.this.connection.getMC().sendMessage(reply);
		} catch (ReplicationDegreeOutOfLimitsException | ChunkNoException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private void sendPutChunk(String fileId, byte[] chunk, int chunkNo, int desiredReplicationDegree) {

		class PutChunk implements Runnable {

			Message message;
			private int waitingTime = Peer.INITIAL_PUT_CHUNK_WAIT_TIME;
			ChunkInfo chunkInfo;

			public PutChunk(int desiredReplicationDegree) {

				chunkInfo = registerSentChunk(chunkNo, fileId, desiredReplicationDegree);

			}

			@Override
			public void run() {

				int counter = 0;
				boolean continueLoop = true;

				try {
					this.message = Message.buildMessage(new Message.MessageFields(MessageType.PUTCHUNK,
							BACKUP_PROTOCOL_VERSION, Peer.this.id, fileId, chunkNo, desiredReplicationDegree), chunk);

					do {

						Peer.this.connection.getMDB().sendMessage(message);

						chunkInfo = registerSentChunk(chunkNo, fileId, desiredReplicationDegree);

						System.out.println("Attempt " + ++counter + "\n");

						try {
							Thread.sleep(waitingTime);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

						this.waitingTime *= 2;

						System.out.println("Next desired replication degree: " + desiredReplicationDegree);
						System.out.println("Current desired replication degree: " + chunkInfo.desiredReplicationDegree);
						System.out.println("Current replication degree: " + chunkInfo.replicationDegree);

						if (Peer.PUT_CHUNK_MAX_TIMES < counter)
							continueLoop = false;

						else {

							if (chunkInfo.replicationDegree >= chunkInfo.desiredReplicationDegree) {

								if (desiredReplicationDegree == chunkInfo.desiredReplicationDegree) {

									continueLoop = false;

								}

							}

						}

					}

					while (continueLoop);

				} catch (ReplicationDegreeOutOfLimitsException | ChunkNoException e) {
					e.printStackTrace();
				}

			}

		}

		Thread putChunk = new Thread(new PutChunk(desiredReplicationDegree));
		putChunk.start();

		System.out.println("Creating Thread for chunk " + chunkNo + "date: " + System.currentTimeMillis());

	}

	private synchronized ChunkInfo registerSentChunk(int chunkNo, String fileId, int desiredReplicationDegree) {

		ChunkInfo chunkInfo = chunkMap.get(ChunkInfo.buildChunkId(chunkNo, fileId));

		if (chunkInfo == null) {

			chunkInfo = new ChunkInfo(desiredReplicationDegree, chunkNo, fileId);
			chunkMap.put(chunkInfo.chunkId, chunkInfo);
		}

		else {
			chunkInfo.desiredReplicationDegree = desiredReplicationDegree;
		}

		return chunkInfo;

	}

	private synchronized void registerMySavedChunk(Message putChunkMessage, boolean saved) {

		String chunkId = ChunkInfo.buildChunkId(putChunkMessage.getMessageFields().chunkNo,
				putChunkMessage.getMessageFields().fileId);

		ChunkInfo chunkInfo = chunkMap.get(chunkId);

		if (chunkInfo == null) {

			chunkInfo = new ChunkInfo(putChunkMessage.getMessageFields().replicationDegree,
					putChunkMessage.getMessageFields().chunkNo, putChunkMessage.getMessageFields().fileId,
					new int[] {});
			chunkMap.put(chunkId, chunkInfo);

		}

		if (saved) {
			chunkInfo.addPeer(Peer.this.id);
		}

		// ALWAYS UPDATE THE DESIRED REPLICATION DEGREE
		chunkInfo.desiredReplicationDegree = putChunkMessage.getMessageFields().replicationDegree;

	}

	private synchronized void registerOtherSavedChunk(Message storedMessage) {

		String chunkId = ChunkInfo.buildChunkId(storedMessage.getMessageFields().chunkNo,
				storedMessage.getMessageFields().fileId);

		ChunkInfo chunkInfo = chunkMap.get(chunkId);

		if (chunkInfo == null) {

			chunkInfo = new ChunkInfo(storedMessage.getMessageFields().replicationDegree,
					storedMessage.getMessageFields().chunkNo, storedMessage.getMessageFields().fileId,
					new int[] { storedMessage.getMessageFields().senderId });
			chunkMap.put(chunkId, chunkInfo);

		}

		else
			chunkInfo.addPeer(storedMessage.getMessageFields().senderId);

		if (chunkInfo.replicationDegree == chunkInfo.desiredReplicationDegree) {
			System.out.println("Chunk " + chunkInfo.chunkId + " has reached the desired replication degree ("
					+ chunkInfo.replicationDegree + ")");
		}

	}

	private void saveChunkToDisk(Message msg) {

		String chunkId = ChunkInfo.buildChunkId(msg.getMessageFields().chunkNo, msg.getMessageFields().fileId);

		ChunkInfo chunkInfo = chunkMap.get(chunkId);

		Boolean save = true;

		if (chunkInfo != null) {
			if (chunkInfo.replicationDegree >= msg.getMessageFields().replicationDegree
					|| chunkInfo.seeds.contains(this.id))
				save = false;
		}

		registerMySavedChunk(msg, save);

		if (save) {

			System.out.println(" ---- Saving Chunk -----");

			try {
				FileOutputStream stream = new FileOutputStream(this.pathToPeerChunks + "/"
						+ ChunkInfo.buildChunkId(msg.getMessageFields().chunkNo, msg.getMessageFields().fileId));
				stream.write(msg.getChunk());
				stream.close();

			} catch (Exception e) {
				e.printStackTrace();
			}

		}

	}

	public void receiveData(String fileName, int length, byte[] buffer) {

		File file = new File(this.pathToPeerReceivedFiles + "/" + fileName);

		try {

			FileOutputStream output = new FileOutputStream(file, true);

			output.write(buffer, 0, length);

			output.close();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public void backup(String fileName, int desiredReplicationDegree, String lastModifiedDate) {

		File file = new File(this.pathToPeerReceivedFiles + "/" + fileName);
		ArrayList<byte[]> chunks = Utils.chunkFile(file);
		String fileId = Utils.hashString(file.getName() + "-" + lastModifiedDate, HASH_ALGORITHM);

		for (int i = 0; i < chunks.size(); i++) {
			sendPutChunk(fileId, chunks.get(i), i + 1, desiredReplicationDegree);
		}

		file.delete();

	}

	private void loadChunksTable() {

		try {

			Scanner scanner = new Scanner(new File(Peer.this.pathToPeer + "/" + STATE_FILE_NAME));

			while (scanner.hasNextLine()) {

				String line = scanner.nextLine();

				Scanner lineScanner = new Scanner(new InputStreamReader(new ByteArrayInputStream(line.getBytes())));

				int chunkNo = lineScanner.nextInt();
				int numberOfSeeds = lineScanner.nextInt();

				int[] seeds = new int[numberOfSeeds];

				for (int i = 0; i < numberOfSeeds; i++) {
					seeds[i] = lineScanner.nextInt();
				}

				int desiredRepDeg = lineScanner.nextInt();

				String fileId = lineScanner.next();

				ChunkInfo chunkInfo = new ChunkInfo(desiredRepDeg, chunkNo, fileId, seeds);

				this.chunkMap.put(chunkInfo.chunkId, chunkInfo);

				lineScanner.close();

			}

			scanner.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
		}

	}


	public void delete(String fileName, String lastModifiedDate){
		System.out.println("Delete protocol initiated");

		String fileIdToDelete = Utils.hashString(fileName + "-" + lastModifiedDate, HASH_ALGORITHM);
		File[] files = new File(this.pathToPeerChunks).listFiles();
		
		if(files != null){
			for(File f: files){
				String fileId = f.getName().split("-")[0];

				if(fileId == fileIdToDelete){
					f.delete();
				}
			}
		}
	
	}

}
