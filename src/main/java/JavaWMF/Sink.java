package JavaWMF;

public interface Sink {

	public void onLostDevice(int deviceIndex);
	public void onNewDevice();
	public void onError(int errorCode, String errorMessage);
	
}
