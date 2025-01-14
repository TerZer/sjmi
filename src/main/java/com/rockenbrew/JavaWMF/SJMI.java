package com.rockenbrew.JavaWMF;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SJMI {


	private static boolean os64bit;
	private static boolean libAvailable;
	private static boolean libAccessible;
	private static String libError;
	
	public static boolean debugMode;
	private ArrayList<VideoDevice> devices;
	private boolean isInitialised;
	
	private Sink monitorSink;
	private Thread monitor; // monitor any library feedback

	private Pattern pattern = Pattern.compile("\\$(.*?)\\$");
	
	
	//
	// load library & catch any errors
	//
	static {
		
		System.out.print("SJMI0.1 ");
		
		if (!System.getProperty("os.arch").matches("x86")) {
			
			os64bit = true; // probably
					
			try {

				final Path libraryPath = extractNativeBinary();
				System.load(libraryPath.normalize().toString());

				libAvailable = true;
				libAccessible = true;

				System.out.println("successfully loaded");

			} catch (UnsatisfiedLinkError uleEx) {

				System.out.println("load failed: Library, (or dependencies thereof,) unavailable");
				System.out.println(uleEx.getMessage());

				libError = uleEx.getMessage();
				libAvailable = false;

			} catch (SecurityException sEx) { // I've no idea what this exception entails in practice

				System.out.println("load failed: Library inaccessible");
				System.out.println(sEx.getMessage());

				libError = sEx.getMessage();
				libAccessible = false;
			}
		
	}
		else
			os64bit = false;
					
	}

	public static Path extractNativeBinary(){
		String location = "/lib/sjmi.dll";
		final InputStream binary = SJMI.class.getResourceAsStream(location);
		final Path destination;

		// Do not try to delete the temporary directory on the close if Windows
		// because there will be a write lock on the file which will cause an
		// AccessDeniedException. Instead, try to delete existing instances of
		// the temporary directory before extracting.
		destination = new TemporaryDirectory().deleteOldInstancesOnStart().getPath().resolve("./" + location).normalize();

		try {
			Files.createDirectories(destination.getParent());
			Files.copy(binary, destination);
			binary.close();
		} catch (final IOException ioe) {
			throw new IllegalStateException(String.format("Error writing native library to \"%s\".", destination), ioe);
		}

		return destination;
	}

	public static void main(String[] args) {
		
		System.out.println("SJMI library is not designed to run as a standalone.");
		System.out.println("Library to be included (as .jar) and initiated form another Java applciation.");
	}

	public SJMI() {

	}

	public static boolean isDebugMode() {
		return debugMode;
	}

	public static void setDebugMode(boolean debugMode) {
		SJMI.debugMode = debugMode;
	}

	public void debug_ListDevices()	{
		
		for (VideoDevice d : devices)
			getMonitorSink().onError(0, d.getFriendlyName());
	}
	
	public ArrayList<VideoDevice> initDevices()	{ 
		//
		// returns array of available devices, each within the VideoDevice Object structure
		// further device details (ie. capture formats) are not available until a device is activated
		
		String[] deviceName = GetAvailDevices(); // array of paired values ("friendly device names" & "device symbolic link")
		
		int deviceCount = deviceName.length / 3; // SJMI library will always return an even number
		
		devices = new ArrayList<VideoDevice>(deviceCount); // overwrites any previous instances
		
		for (int i=0; i<deviceCount*3; i+=3) {

			String parentId = deviceName[i+2];
			Matcher matcher = pattern.matcher(parentId);
			if(matcher.find()){
				parentId = matcher.group();
			}
			VideoDevice newDevice = new VideoDevice(this, (int)(i/3), deviceName[i], deviceName[i+1], parentId, true);
			devices.add(newDevice);
		}
		
		return devices;
	}
	
	
	public ArrayList<VideoDevice> getDevices()	{ 
		return devices;
	}

	
	public VideoDevice getDeviceByIndex(int i)	{ 
		return devices.get(i);
	}

	
	public int[] getActiveDeviceIndex()	{ 
	
		int[] activeDeviceIndices = new int[devices.size()];
		int i = 0;
		
		for (VideoDevice d : devices)
			if (d.isActive()) {
				activeDeviceIndices[i] = d.getId();
				i++;
			}
			
		return Arrays.copyOf(activeDeviceIndices, i); // return trimmed
	}

	
	public boolean initialise()	{

		if (getMonitorSink() == null)
			setMonitorSink(new MessageSink());

		if (!isOs64bit()) {
		
			if (isDebugMode()) {
				
				final String error64bitSys = "SJMI requires a 64-bit system";
				
				System.out.println(error64bitSys);
				getMonitorSink().onError(1, error64bitSys);
			}
			
			return false;
		}
		
		
		if (!isInitialised) { // not previously initialised

			Initialise();
			
			monitor = new Monitor(); // monitor notifications from the library
			monitor.start();

			isInitialised = true;
			
		} 
		
		initDevices();
				
		try {
			Thread.sleep(250); // give it a moment to initialise
		} catch (InterruptedException e) {
			
		}

		return isInitialised;
	}
	
	
	class Monitor extends Thread {

		
		public Monitor()	{
			setName("SJMI-EventMonitor");		
		}
		
		@Override
    	public void run() {
			
			Monitor(new Receiver(getMonitorSink()));
			
    	} 
    }
	
	
	public boolean uninitialise()	{
		
		if (isInitialised) {
			
			return Uninitialise();
			
		} else
			return true;
	}
	
	public Sink getMonitorSink() {
		return monitorSink;
	}

	public void setMonitorSink(Sink monitorSink) {
		this.monitorSink = monitorSink;
	}

	public boolean isInitialised() {
		return isInitialised;
	}

    
	public static boolean isLibAvailable() {
		return libAvailable;
	}

	
	public static boolean isLibAccessible() {
		return libAccessible;
	}


	public static boolean isOs64bit() {
		return os64bit;
	}
	
	public static String getLibError() {
		return libError;
	}
	
	// JNI functions - non-device specific

	private native String[] GetAvailDevices();
	private native boolean Initialise();
	private native boolean Monitor(Receiver rcvr);
	private native boolean Uninitialise();



}
