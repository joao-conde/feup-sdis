package test;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;

import org.junit.Test;

import backup.Peer;
import backup.Utils;

public class TestPeerMethods {

	@Test
	public void testHash() {
		
		ArrayList<String> fileNames = new ArrayList<>();
		
		fileNames.add("hello dfsdjfkhxgfxghkxj");
		fileNames.add("hellodfsdf");
		fileNames.add("hellsdfdsfdsf");
		fileNames.add("helsdfdsf");
		fileNames.add("hello dfsdjfkhxgfxghkxj");
		
		ArrayList<String> fileIds = new ArrayList<>();
				
		for(String s : fileNames) {
			
			String fileId = Utils.hashString(s, Peer.HASH_ALGORITHM);
			fileIds.add(fileId);
			
			assertEquals(64, fileId.length());
			
		}
		
		
		assertTrue(fileIds.get(0).equals(fileIds.get(fileIds.size()-1)));
		

	}
	
}
