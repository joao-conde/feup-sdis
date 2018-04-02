package backup;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

public class Utils {

	public static String hashString(String originalString, String algorithm) {

		String fileId = "";

		byte[] fileDigested = null;

		try {

			MessageDigest messageDigest = MessageDigest.getInstance(algorithm);
			fileDigested = messageDigest.digest(originalString.getBytes(StandardCharsets.UTF_8));

			for (byte b : fileDigested) {

				String current = Integer.toHexString(b & 0xff);

				String append = current.length() < 2 ? "0" + current : current;

				fileId += append;

			}

		} catch (NoSuchAlgorithmException e) {
			System.out.println(e.getLocalizedMessage());

		}

		return fileId;

	}

	public static ArrayList<byte[]> chunkFile(File file) {

		FileInputStream inputStream;
		ArrayList<byte[]> chunks = new ArrayList<byte[]>();

		try {
			inputStream = new FileInputStream(file);
			long size = file.length();

			byte[] fileBuffer = new byte[(int) size];
			inputStream.read(fileBuffer);

			int numberOfChunks = fileBuffer.length / Peer.CHUNK_MAX_SIZE;

			int i;
			for (i = 0; i < numberOfChunks; i++) {
				byte[] chunk = new byte[Peer.CHUNK_MAX_SIZE];
				System.arraycopy(fileBuffer, i * Peer.CHUNK_MAX_SIZE, chunk, 0, Peer.CHUNK_MAX_SIZE);
				chunks.add(chunk);
			}

			int lastChunkSize = fileBuffer.length % Peer.CHUNK_MAX_SIZE;
			if (lastChunkSize != 0) {

				byte[] lastChunk = new byte[lastChunkSize];
				System.arraycopy(fileBuffer, i * Peer.CHUNK_MAX_SIZE, lastChunk, 0, lastChunkSize);
				chunks.add(lastChunk);

			}

		} catch (IOException e) {
			
			System.out.println(e.getLocalizedMessage());
			
		}

		return chunks;
	}

	public static void mergeChunks(String filePath, String chunksFolder, String fileId) {

		File file = new File(filePath);

		try {
			FileOutputStream out = new FileOutputStream(file, true);

			FileInputStream in;
			int chunkNo = 1;
			File chunk = new File(chunksFolder + '/' + fileId + '-' + chunkNo++);

			while (chunk.exists()) {
				in = new FileInputStream(chunk);

				byte[] chunkBuffer = new byte[(int)chunk.length()];
				in.read(chunkBuffer);
				out.write(chunkBuffer);
				
				chunk.delete();

				chunk = new File(chunksFolder + '/' + fileId + '-' + chunkNo++);
			}
			
			new File(chunksFolder).delete();


			out.close();

		} catch (IOException e) {
			System.out.println(e.getLocalizedMessage());

		}
		
	}
	
	public static long calculateFolderSize(String folderName) {
		
		File folder = new File(folderName);
		
		if(!folder.isDirectory())
			return -1;
		
		File[] files = folder.listFiles();
		
		long result = 0;
		
		for(File file : files) {
			
			if(!file.isDirectory())
				result += file.length();
			
		}
		
		
		return result;
		
	}
	
	
}
