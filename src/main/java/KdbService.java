import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import kx.c;

public class KdbService {
	private final static Logger log = LogManager.getLogger(KdbService.class);
	
	private final int TRY_ONE_FAIL = 5;
	private final int STOP_RE_TRYING = Integer.MAX_VALUE;
	private c conn = null;
	
	private final String host;
	private final int port;
	private final boolean enabled;
	private final long interval;
	
	private BlockingQueue<Object> queue; // data obj
	
	public KdbService(String host, int port, boolean enabled, int reconInterval) {
		this.host = host;
		this.port = port;
		this.enabled = enabled;
		this.interval = reconInterval * 1000; 
		
		this.queue = new LinkedBlockingQueue<Object>();
	}
	
	public KdbService(String host, int port, boolean enabled, int reconInterval, BlockingQueue<Object> queue) {
		this(host, port, enabled, reconInterval);
		this.queue = queue;
	}
	
	public void doStart() {
		try {
			if (!enabled) {
				log.info("KdbService disabled");
			}
			else {
				new Thread(new KdbConnector()).start();
			}
		}
		catch (Exception e) {
			log.error("ERROR: KDB Service not started");
		}
	}
	
	public void doStop() {
		try {
			conn.close();
		}
		catch (IOException e) {
			log.error("ERROR: closing kdb connection", e);
		}
	}
	
	private void connectionLoad() throws Exception {
		if (connectToKdb()) {
			try {
				new Thread(new KdbListener()).start();
			}
			catch (Exception e) {
				log.error("ERROR: KDB Service not started");
			}
		}
		else {
			throw new Exception("Kdb connection error");
		}
	}
	
	private boolean connectToKdb() {
		log.info("connecting to kdb: " + this .host);
		
		try {
			conn = new c(host, port);
			log.info("connected to kdb: " + this.host);
		}
		catch (Exception e) {
			log.error("ERROR: connection to KDB FAILED - ");
			return false;
		}
		
		return true;
	}
	
	
	private void processKdbData(Object[] data) throws Exception {
		
	}
	
	private class KdbConnector implements Runnable {
		private int COUNTER = 1;
		
		@Override 
		public void run() {
			while (true) {
				try {
					COUNTER++;
					connectionLoad();
					return;
				}
				catch (Exception e) {
					log.warn("Retrying to connect Attempt: " + COUNTER);
					
					if (COUNTER == STOP_RE_TRYING) {
						Thread.currentThread().interrupt();
						return;
					}
					else if (COUNTER > TRY_ONE_FAIL) {
						try {
							Thread.sleep(interval);
						}
						catch (InterruptedException ie) {
							ie.printStackTrace();
						}
					}
				}
			}
		}
	}
	
	private class KdbListener implements Runnable {
		@Override
		public void run() {
			
		}
	}
	
}
