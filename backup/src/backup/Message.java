package backup;

public class Message {

	public static enum MessageType {
		
		PUTCHUNK("PUTCHUNK"),
		STORED("STORED");
		
		public String text;
		
		private MessageType(String text) {
			this.text = text;
		}
		
	}
		
}
