package backup;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.ArrayList;
import java.util.Collections;
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
	public final static int MESSAGE_MAX_SIZE = CHUNK_MAX_SIZE + 100;
	public final static int MAX_RANDOM_WAIT_TIME = 400;
	public final static int PUT_CHUNK_MAX_TIMES = 5;
	public final static int INITIAL_PUT_CHUNK_WAIT_TIME = 1000;
	public final static int KBYTES = 1000;
	public final static int MAX_STORAGE_SIZE = 10000000; 
	public final static String HASH_ALGORITHM = "SHA-256";
	public final static char SEPARATOR = ' ';
	public final static String STATE_FILE_NAME = "state";
	public final static int PUT_CHUNK_DELAY = 15;
	
	public final static String STATE_FILES_FILE_NAME = "backedUpFiles";

	public int id;
	private Connection connection;
	private Dispatcher mcDispatcher;
	private Dispatcher mdbDispatcher;
	private Dispatcher mdrDispatcher;
	private Random randomGenerator = new Random(System.currentTimeMillis());
	private HashMap<String, ChunkInfo> chunkMap = new HashMap<>();

	private String pathToPeer;
	private String pathToPeerChunks;
	private String pathToPeerRestored;
	private String pathToPeerTemp;
	private CloseResources closeResources = new CloseResources();
	
	private HashMap<String, Boolean> sendingChunks = new HashMap<>();
	private HashMap<String, Boolean> receivingChunks = new HashMap<>();
	
	private HashMap<String, String[]> fileMap = new HashMap<>();
	
	private int remainingChunks;

	private Registry registry;
	
	private long currentMaxChunkFolderSize;

	public static class ChunkInfo {

		private int replicationDegree = 0;
		public HashSet<Integer> seeds = new HashSet<>();
		public int desiredReplicationDegree;
		public String chunkId;
		public String fileId;
		public int chunkNo;
		public int backupInitiatorPeer;

		public ChunkInfo(int desiredReplicationDegree, int chunkNo, String fileId, int backupInitiatorPeer) {
			this(desiredReplicationDegree, chunkNo, fileId, backupInitiatorPeer, new int[] {});
		}

		public ChunkInfo(int desiredReplicationDegree, int chunkNo, String fileId, int backupInitiatorPeer, int[] seeds) {
			this.desiredReplicationDegree = desiredReplicationDegree;
			this.chunkId = buildChunkId(chunkNo, fileId);
			this.fileId = fileId;
			this.backupInitiatorPeer = backupInitiatorPeer;
			this.chunkNo = chunkNo;

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
					
					try {
						int delay = randomGenerator.nextInt(MAX_RANDOM_WAIT_TIME);
						Thread.sleep(delay);
					} catch (InterruptedException e) {
						System.out.println(e.getLocalizedMessage());

					}

					sendStored(this.message);

					break;
				
				case STORED:

					registerOtherSavedChunk(this.message);

					break;
									
				case GETCHUNK:
					
					sendChunk(this.message);
					
					break;
				
					
				case CHUNK:
					
					String chunkId = ChunkInfo.buildChunkId(message.getMessageFields().chunkNo, message.getMessageFields().fileId);
					Boolean expectingChunk = receivingChunks.get(chunkId);
					updateSentChunk(ChunkInfo.buildChunkId(message.getMessageFields().chunkNo, message.getMessageFields().fileId),true); //chunk already sent
					
					
					if(expectingChunk != null) {
						
						if(expectingChunk) {
							
							processReceivedChunk(message);	
							updateReceivingChunk(chunkId, false);
							updateSentChunk(chunkId, false);
							break;
							
						}
						
			
					}
					
					break;

				case DELETE:

					deleteFileFromDisk(this.message.getMessageFields().fileId);

					break;


				default:
					break;
				}


			}

			catch (NumberFormatException e) {
				System.out.println(e.getLocalizedMessage());
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
						pw.print(chunkInfo.fileId);
						pw.print(SEPARATOR);
						pw.println(chunkInfo.backupInitiatorPeer);

					}

				};

				Peer.this.chunkMap.forEach(action);

				pw.close();
				PrintWriter pwf = new PrintWriter(new File(Peer.this.pathToPeer + "/" + STATE_FILES_FILE_NAME));
				
				
				for(String fileId : fileMap.keySet()) {
					
					String[] values = fileMap.get(fileId);
					
					pwf.write(fileId);
					pwf.write(SEPARATOR);
					pwf.write(values[0]);
					pwf.write(SEPARATOR);
					pwf.write(values[1]);
					pwf.write(SEPARATOR);
					pwf.write(values[2]);

				}
				
				pwf.close();
				

				Peer.this.registry.unbind(Protocol.PROTOCOL + "-" + Peer.this.id);

			} catch (FileNotFoundException | RemoteException | NotBoundException e) {
			
				System.out.println(e.getLocalizedMessage());

			
			}

		}

	}

	public static void main(String[] args) {

		System.setProperty("java.net.preferIPv4Stack", "true");

		Peer peer = new Peer(Integer.parseInt(args[0]));

		System.out.println("------ Peer " + args[0] + " INITIATED -------");

		Runtime.getRuntime().addShutdownHook(new Thread(peer.closeResources));
		
		if(peer.id == 3) {
			
			if(peer.chunkMap.containsKey("cef8c1533606122ea6bbcc20036f1aa778386c0d870242c0db3cf02c08e13604-1"))
			
				peer.requestChunk("cef8c1533606122ea6bbcc20036f1aa778386c0d870242c0db3cf02c08e13604-1");
		}

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
		this.pathToPeerRestored = this.pathToPeer + "/restored";
		this.pathToPeerTemp = this.pathToPeer + "/temp";
		
		this.currentMaxChunkFolderSize = MAX_STORAGE_SIZE;

		new File(this.pathToPeerChunks).mkdirs();
		new File(this.pathToPeerRestored).mkdir();
		new File(this.pathToPeerTemp).mkdir();

		this.loadChunksTable();
	
		try {

			registry = LocateRegistry.createRegistry(Registry.REGISTRY_PORT);

		}

		catch (RemoteException e) {
			System.out.println(e.getLocalizedMessage());
		}

		try {

			registry = LocateRegistry.getRegistry();
			Protocol stub = (Protocol) UnicastRemoteObject.exportObject(this, 0);
			registry.bind(Protocol.PROTOCOL + "-" + id, stub);

		} catch (AlreadyBoundException | RemoteException e) {

			System.out.println(e.getLocalizedMessage());

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
			System.out.println(e.getLocalizedMessage());

		}

	}

	private void sendDelete(String fileId) {

		try {
			Message deleteMsg = Message.buildMessage(new MessageFields(MessageType.DELETE, BACKUP_PROTOCOL_VERSION, this.id, fileId));
			Peer.this.connection.getMC().sendMessage(deleteMsg);
		} catch (ReplicationDegreeOutOfLimitsException | ChunkNoException e) {
			System.out.println(e.getLocalizedMessage());

		}

	}

	private void sendPutChunk(String fileId, byte[] chunk, int chunkNo, int desiredReplicationDegree, int peerId) {

		class PutChunk implements Runnable {

			Message message;
			private int waitingTime = Peer.INITIAL_PUT_CHUNK_WAIT_TIME;
			ChunkInfo chunkInfo;

			public PutChunk(int desiredReplicationDegree) {

				chunkInfo = registerSentChunk(chunkNo, fileId, desiredReplicationDegree, peerId);

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

						chunkInfo = registerSentChunk(chunkNo, fileId, desiredReplicationDegree, peerId);

						System.out.println("Attempt " + ++counter + "\n");

						try {
							Thread.sleep(waitingTime);
						} catch (InterruptedException e) {
							System.out.println(e.getLocalizedMessage());

						}

						this.waitingTime *= 2;

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
					System.out.println(e.getLocalizedMessage());

				}

			}

		}

		Thread putChunk = new Thread(new PutChunk(desiredReplicationDegree));
		putChunk.start();

	}

	private synchronized ChunkInfo registerSentChunk(int chunkNo, String fileId, int desiredReplicationDegree, int peerId) {

		ChunkInfo chunkInfo = chunkMap.get(ChunkInfo.buildChunkId(chunkNo, fileId));

		if (chunkInfo == null) {

			chunkInfo = new ChunkInfo(desiredReplicationDegree, chunkNo, fileId, peerId);
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
					putChunkMessage.getMessageFields().chunkNo, putChunkMessage.getMessageFields().fileId, putChunkMessage.getMessageFields().senderId,
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
					storedMessage.getMessageFields().senderId,
					new int[] { storedMessage.getMessageFields().senderId });
			chunkMap.put(chunkId, chunkInfo);

		}

		else
			chunkInfo.addPeer(storedMessage.getMessageFields().senderId);

	}

	private synchronized void saveChunkToDisk(Message msg) {

		String chunkId = ChunkInfo.buildChunkId(msg.getMessageFields().chunkNo, msg.getMessageFields().fileId);

		ChunkInfo chunkInfo = chunkMap.get(chunkId);
		
		
		long chunksFolderSize = Utils.calculateFolderSize(pathToPeerChunks);
		

		Boolean save = true;

		if (chunkInfo != null) {
			if (chunkInfo.replicationDegree >= msg.getMessageFields().replicationDegree
					|| chunkInfo.seeds.contains(this.id) || chunksFolderSize + msg.getChunk().length > this.currentMaxChunkFolderSize)
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

			} catch (IOException e) {
				System.out.println(e.getLocalizedMessage());
			}

		}

	}


	public String backup(String filePath, int desiredReplicationDegree) {

		File file = new File(filePath);
		
		Path path = Paths.get(file.getAbsolutePath());
		
		BasicFileAttributes attributes;
		try {
			
			attributes = Files.readAttributes(path, BasicFileAttributes.class);
			
			FileTime lastModifiedTime = attributes.lastModifiedTime();
			
			String fileId = Utils.hashString(file.getName() + "-" + lastModifiedTime, HASH_ALGORITHM);

			ArrayList<byte[]> chunks = Utils.chunkFile(file);
			
			for (int i = 0; i < chunks.size(); i++) {
				try {
					Thread.sleep(PUT_CHUNK_DELAY);
				} catch (InterruptedException e) {
					System.out.println(e.getLocalizedMessage());

				}
				sendPutChunk(fileId, chunks.get(i), i + 1, desiredReplicationDegree, this.id);
			}
			
			String[] fileInfo = new String[3];
			fileInfo[0] = file.getName();
			fileInfo[1] = lastModifiedTime.toString();
			fileInfo[2] = Integer.toString(chunks.size());
			
			fileMap.put(fileId, fileInfo);
			
		
		} catch (IOException e1) {
			
			return "Could not backup file, file '" + filePath + "' not found";
			
		}
		
		return "File '" + file.getName() + " successfully restored";

		
		

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

				int backupInitiatorPeer = lineScanner.nextInt();

				ChunkInfo chunkInfo = new ChunkInfo(desiredRepDeg, chunkNo, fileId, backupInitiatorPeer, seeds);

				this.chunkMap.put(chunkInfo.chunkId, chunkInfo);

				lineScanner.close();

			}
			
			
			scanner = new Scanner(new File(Peer.this.pathToPeer + "/" + STATE_FILES_FILE_NAME));
			
			while(scanner.hasNextLine()) {
				
				String line = scanner.nextLine();
				
				Scanner lineScanner = new Scanner(new InputStreamReader(new ByteArrayInputStream(line.getBytes())));
				
				String fileId = lineScanner.next();
				String fileName = lineScanner.next();
				String lastModifiedDate = lineScanner.next();
				String numberOfChunks = lineScanner.next();
				
				fileMap.put(fileId, new String[] {fileName,lastModifiedDate,numberOfChunks});
				
				lineScanner.close();
				
			}
			
			

			scanner.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
		}

	}
	
	private void requestChunk(String chunkId) {
		
		ChunkInfo chunkInfo = chunkMap.get(chunkId);
		
		try {
			
			Message message = Message.buildMessage(new MessageFields(MessageType.GETCHUNK, BACKUP_PROTOCOL_VERSION, id, chunkInfo.fileId, ChunkInfo.getChunkNo(chunkId)));
			
			this.connection.getMC().sendMessage(message);
			
			updateReceivingChunk(chunkId, true);
			
		
		} catch (ReplicationDegreeOutOfLimitsException | ChunkNoException e) {
			System.out.println(e.getLocalizedMessage());

		}
		
		
	}
	
	private synchronized void updateSentChunk(String chunkId, boolean alreadySent) {
		
		this.sendingChunks.remove(chunkId);
		this.sendingChunks.put(chunkId, new Boolean(alreadySent));
				
	}
	
	private synchronized void updateReceivingChunk(String chunkId, boolean received) {
			
		this.receivingChunks.remove(chunkId);
		this.receivingChunks.put(chunkId, new Boolean(received));
				
	}
	
	
	private void sendChunk(Message getChunkMessage) {
		
		String chunkId = ChunkInfo.buildChunkId(getChunkMessage.getMessageFields().chunkNo, getChunkMessage.getMessageFields().fileId);
		
		ChunkInfo chunk = chunkMap.get(chunkId);
		
		if(chunk == null) {
			
			updateSentChunk(chunkId, false);
			return;
		}
			
		
		if(!chunk.seeds.contains(this.id)) {
			
			updateSentChunk(chunkId, false);
			return;
		}
			
		
		updateSentChunk(chunkId, false);
		
		ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
		
		int waitingTime = randomGenerator.nextInt(MAX_RANDOM_WAIT_TIME);
		
		service.schedule(new Thread() {
			
			@Override
			public void run() {
				

				if(Peer.this.sendingChunks.get(chunkId) == true) {
					
					updateSentChunk(chunkId, false);
					return;
				}
					
				
				
				
				byte[] chunkBuffer = loadChunk(chunkId);
				
				
				
				try {
					
					Message chunkMessage = Message.buildMessage(new MessageFields(MessageType.CHUNK, BACKUP_PROTOCOL_VERSION, Peer.this.id, chunk.fileId, getChunkMessage.getMessageFields().chunkNo), chunkBuffer);
					Peer.this.connection.getMDR().sendMessage(chunkMessage);
					
					updateSentChunk(chunkId, false);
					
					
				} catch (ReplicationDegreeOutOfLimitsException | ChunkNoException e) {
					System.out.println(e.getLocalizedMessage());

				}
				
				
				
				
				
				
			}
			
		}, waitingTime, TimeUnit.MILLISECONDS);
		
	}
	
	
	private byte[] loadChunk(String chunkId) {
		
		File chunk = new File(pathToPeerChunks + "/" + chunkId); 
		byte[] buffer = new byte[(int)chunk.length()];
		
		try {
			
			FileInputStream inputStream = new FileInputStream(chunk);
			inputStream.read(buffer);
			inputStream.close();
			
			
		} catch (IOException e) {
			System.out.println(e.getLocalizedMessage());

		}
		
		
		return buffer;
		
	}
	
	
	public String restore(String fileName) {
		
		String fileId = findBackedUpFileId(fileName);
		
		if(fileId == null) {
			
			return "Could not restore file, file '" + fileName + "' not found"; 
		}
			
		
		String[] fileInfo = fileMap.get(fileId);
		
		int numberOfChunks = Integer.parseInt(fileInfo[2]);
		
		this.remainingChunks = numberOfChunks;
		
		File tempFolder = new File(pathToPeerTemp + "/temp-" + fileInfo[0]);
		tempFolder.mkdirs();
		
		for(int i = 1; i <= numberOfChunks; i++) {
			
			String chunkId = fileId + "-" + i;
			requestChunk(chunkId);
			
		}
		
		return "File '" + fileName + " successfully restored";
		
	}
	
	public synchronized void processReceivedChunk(Message message) {
		
		
		String chunkId = ChunkInfo.buildChunkId(message.getMessageFields().chunkNo, message.getMessageFields().fileId);
		
		String fileName = fileMap.get(message.getMessageFields().fileId)[0];
		
		String tempFolder = pathToPeerTemp + "/temp-" + fileName;
		
		File chunkFile = new File(pathToPeerTemp + "/temp-" + fileName + "/" + chunkId);
		
		if(chunkFile.exists())
			return;
		
		this.remainingChunks--;
		
		try {
			FileOutputStream output = new FileOutputStream(chunkFile);
			
			output.write(message.getChunk());
			
			output.close();
			
		} catch (IOException e) {
			System.out.println(e.getLocalizedMessage());

		}
		
		if(this.remainingChunks == 0) {
			
			Utils.mergeChunks(pathToPeerRestored + "/" + fileName, tempFolder, message.getMessageFields().fileId);
			
			
		}
		
		
	}
	

	public String findBackedUpFileId(String fileName){
		ArrayList<FileTime> dates = new ArrayList<FileTime>(); 
		
		for(String[] f: fileMap.values()){
			if(f[0].equals(fileName)){
				dates.add(FileTime.from(Instant.parse(f[1])));
			}
		}

		String result = null;
		
		if(dates.size() > 0)
			result = Utils.hashString(fileName + "-" + Collections.max(dates).toString(), HASH_ALGORITHM);
		
		return result;
	}

	public String delete(String fileName){
		String fileIdToDelete = findBackedUpFileId(fileName);

		if(fileIdToDelete == null) {
			return "Could not delete file, file '" + fileName + "' not found";
		}
		
		sendDelete(fileIdToDelete);
		deleteFileFromDisk(fileIdToDelete);
		
		return "File '" + fileName + " successfully deleted";
		
		
	}

	public void deleteFileFromDisk(String fileIdToDelete){
		File[] chunks = new File(this.pathToPeerChunks).listFiles();

		if(chunks != null){
			for(File chunk: chunks){
				String[] splitId = chunk.getName().split("-"); 
				String chunkId = ChunkInfo.buildChunkId(Integer.parseInt(splitId[1]), splitId[0]);

				if(splitId[0].equals(fileIdToDelete)){
					chunk.delete();
					chunkMap.remove(chunkId);
					fileMap.remove(fileIdToDelete);
				}
			}
		}
	}


	public String showServiceState(){
		
		String serviceState = "";

		ArrayList<ChunkInfo> selfInitBackupChunks = new ArrayList<ChunkInfo>();
		ArrayList<ChunkInfo> storedChunks = new ArrayList<ChunkInfo>();

		for (ChunkInfo chunkInfo : chunkMap.values()) {
			
			if(chunkInfo.backupInitiatorPeer == this.id)
				selfInitBackupChunks.add(chunkInfo);

			if(chunkInfo.seeds.contains(this.id))
				storedChunks.add(chunkInfo);
			
		}

		serviceState += showRequestedBackupChunks(selfInitBackupChunks);
		serviceState += showStoredChunks(storedChunks);	
		serviceState += showStorageCapacity(storedChunks);	

		return serviceState;
	}

	public String showRequestedBackupChunks(ArrayList<ChunkInfo> selfRequestedChunks){
		
		ByteArrayOutputStream outBuffer = new ByteArrayOutputStream(); 
		PrintWriter out = new PrintWriter(outBuffer);
		
		if(selfRequestedChunks.size() > 0)
			out.println("\n-----Requested Backup Files-----");
		else
			out.println("\n-----No backups requested-----");
		
		ArrayList<String> uniqueFiles = new ArrayList<String>();

		for(ChunkInfo chunkInfo: selfRequestedChunks){

			if(uniqueFiles.contains(new String(chunkInfo.fileId)))
				continue;
			else uniqueFiles.add(chunkInfo.fileId);

			out.println("\nFile path: " + "../res/peer-" + this.id + "/chunks/inbox");
			out.println("File service ID: " + chunkInfo.fileId);

			String[] fileAttr = fileMap.get(chunkInfo.fileId);
			out.println("File name: " + fileAttr[0]);
			out.println("File last modified date: " + fileAttr[1]);
			out.println("Desired replication degree: " + chunkInfo.desiredReplicationDegree);
			
			ArrayList<ChunkInfo> fileChunks = getStoredFileChunks(chunkInfo.fileId);

			for(ChunkInfo filechunk: fileChunks){
				out.println("File Chunk ID: " + filechunk.chunkNo);
				out.println("File Chunk replication degree: " + filechunk.replicationDegree);
			} 
		}
		
		out.close();
		return outBuffer.toString();
	}

	public String showStoredChunks(ArrayList<ChunkInfo> storedChunks){

		ByteArrayOutputStream outBuffer = new ByteArrayOutputStream(); 
		PrintWriter out = new PrintWriter(outBuffer);
		
		if(storedChunks.size() > 0)
			out.println("\n-----Locally stored file chunks-----");
		else
			out.println("\n-----No chunk stored locally-----");
		
		for(ChunkInfo chunkInfo: storedChunks){

			out.println("\nChunk ID: " + chunkInfo.chunkId);

			File chunk = new File(this.pathToPeerChunks + '/' + chunkInfo.chunkId);
			out.println("Chunk size: " + chunk.length()/KBYTES + " KBytes"); 

			out.println("Chunk replication degree: " + chunkInfo.replicationDegree);
		}

		out.close();
		return outBuffer.toString();
	}

	public String showStorageCapacity(ArrayList<ChunkInfo> storedChunks){

		ByteArrayOutputStream outBuffer = new ByteArrayOutputStream(); 
		PrintWriter out = new PrintWriter(outBuffer);

		out.println("\n-----Peer storage-----");
		int storedSize = 0;
		for(ChunkInfo chunkInfo: storedChunks){
			File chunk = new File(this.pathToPeerChunks + "/" + chunkInfo.chunkId);
			storedSize += chunk.length();
		}

		out.println("\nPeer storage maximum capacity: " + MAX_STORAGE_SIZE + " KBytes");
		out.println("\nPeer used space (stored chunks size): " + storedSize/KBYTES + " KBytes");

		out.close();
		return outBuffer.toString();
	}

	public ArrayList<ChunkInfo> getStoredFileChunks(String fileId){
		ArrayList<ChunkInfo> fileChunks = new ArrayList<ChunkInfo>();

		for (ChunkInfo chunkInfo : chunkMap.values()) {
			
			if(chunkInfo.fileId.equals(fileId))
				fileChunks.add(chunkInfo);

		}

		return fileChunks;
	}
	

}
