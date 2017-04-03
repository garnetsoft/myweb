

public class Order {
	public static enum EnSide {sell, buy, INVALID};
	
	public static enum EnType {
		received, open, done, match, change, error, SNAPSHOT
	};
	
	public static enum EnState { filled, canceled };
}
