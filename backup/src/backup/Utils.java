package backup;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
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
			// TODO Auto-generated catch block
			e.printStackTrace();
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

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

}
