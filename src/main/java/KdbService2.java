import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static java.nio.file.Files.readAllBytes;
import static java.nio.file.Paths.get;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;

import kx.c;
import kx.c.Flip;
import kx.c.KException;

public class KdbService2 {
	private final static Logger log = LogManager.getLogger(KdbService2.class);
	
	private final int TRY_ONE_FAIL = 5;
	private final int STOP_RE_TRYING = Integer.MAX_VALUE;
	private c conn = null;
	
	private final String configFile;
	private final String sqlPath;
	
	private BlockingQueue<Object> queue; // data obj
	
	private Properties p;
	
	// from config properties 
	private  String host;
	private  int port;
	private  boolean enabled;
	private  long interval;
	
	private List<String> sections;
	private List<String> sqlFiles;
	
	private Map<String,String> queryMap = new HashMap<String,String>();
	private Map<String,Flip> dataTables = new HashMap<String,Flip>();
	
	public KdbService2(String configFile, String sqlPath, BlockingQueue<Object> queue) {
		this.configFile = configFile;
		this.sqlPath = sqlPath;		
		this.queue = queue;
		
		loadProperties();
	}
	
	// this can be called so updates can be loaded intraday
	private void loadProperties() {
		this.p = new Properties();
		
		System.out.println("xxxx configFile: " + configFile);
		
		File file = new File(configFile);
		String filePath = file.getAbsolutePath();
		String path = file.getPath();
		String parent = file.getParent();
		log.info("loading config file from: " + configFile);
		
		try (final InputStream stream = new FileInputStream(configFile)) {
			log.info("xxxx stream: " + stream);
			p.load(stream);
		}
		catch (IOException io) {
			log.error("ERROR: cannot load properties file: " + configFile);
		}
		finally {
			
		}

		log.info("xxxx conf: " + p);
		// update interval - 
		this.interval = Integer.parseInt(p.getProperty("interval", "5"));		
		this.enabled = Boolean.parseBoolean(p.getProperty("enabled", "true"));
		this.host = p.getProperty("khost");
		this.port = Integer.parseInt(p.getProperty("kport"));
		
		// sections: 
		this.sections = (List<String>) Arrays.asList(p.getProperty("sections").split(","));	
		this.sqlFiles = (List<String>) Arrays.asList(p.getProperty("sqlFiles").split(","));
		
		try {
			// cache the sqlFiles 
			for (int i = 0; i < sections.size(); i++) {
				String section = sections.get(i);
				String sqlFile = sqlFiles.get(i);
				String sqlPath = parent+"/"+sqlFile;
				String sqlCode = new String(Files.readAllBytes(get(sqlPath)));
				
				log.info("xxxx loading section: " + section);
				log.info("xxxx loading sqlFile: " + sqlFile);
				log.info("xxxx loading sqlCode: " + sqlCode);

				queryMap.put(section, sqlCode);
			}
		}
		catch (IOException io) {
			log.error("ERROR loading config/sql data: " );
		}
				
		doStart(); // start kdb service
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
							Thread.sleep(interval*1000);
						}
						catch (InterruptedException ie) {
							ie.printStackTrace();
						}
					}
				}
			}
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
	
	private class KdbListener implements Runnable {
		// process queries from sqlPath and add results to Map<String, Object> 
				
		// starting Kdb query service 
		@Override
		public void run() {
			log.info("Kdb query service starting: ");
			
			while (true) {
				try {
					// execute each query and cache the results
					for (Map.Entry<String, String> kdbQuery : queryMap.entrySet()) {
						String table = kdbQuery.getKey();
						String qcode = kdbQuery.getValue();
						
						//log.info("exec table: " + table + ", qcode: " + qcode);
						Flip data = c.td(conn.k(qcode));
						
						dataTables.put(table, data);
					}
										
					processKdbData(); // trigger UI update
					
					Thread.sleep(interval*1000); // use updateInterval from config file
				}
				catch (InterruptedException | IOException | KException e) {
					log.error("ERROR: kdb query thread stopped. ", e);
					e.printStackTrace();
					
					int reconn = 1; 
					try {
						Thread.sleep(interval*1000*reconn);
						reconn *= 2;  // diminishing the reconn impact
												
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}
				}
			}
		}
	}
	
	// send it to consumer queue
	private void processKdbData2() {
		// send cached data to UI for display
		// is it better to update it on another thread
		try {
			//this.queue.put(dataCache);
			// format the data in HTML / JSON
			StringBuilder html = new StringBuilder();
			
			/*for (Map.Entry<String, Flip> table : dataTables.entrySet()) {
				html.append(toHtml(table.getKey(), table.getValue()));
			}*/
			
			// preserve the sections order from config
			for (String sec : sections) {
				html.append(toHtml(sec, dataTables.get(sec)));
			}
			
			this.queue.put(html.toString());
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private String toHtml(String table, Flip flip) {
		boolean is_orderbook = table.startsWith("OrderBook") ? true : false;
		
		StringBuilder sb = new StringBuilder();
		sb.append("<br/><b>").append(table).append("</b><br/><br/>");
		
		KxDataModel model = new KxDataModel(flip);
		int rows = model.getRowCount();
		int cols = model.getColumnCount();
		
		sb.append("<table id=\"").append(table).append("\" border=\"1\">");
		// header <b></b>
		
		sb.append("<tr BGCOLOR=\"#99CCFF\">");
		for (int i = 0; i < cols; i++) {
			sb.append("<td>").append(model.getColumnName(i)).append("</td>");
		}
		sb.append("</tr>");
		
		for (int rowIndex = 0; rowIndex < rows; rowIndex++) {
			
			if (rowIndex % 2 == 1) 
				sb.append("<tr BGCOLOR=\"#99CCFF\">");
			else 
				sb.append("<tr>");
			
			for (int columnIndex = 0; columnIndex < cols; columnIndex++) {
				Object cell = model.getValueAt(rowIndex, columnIndex);
				sb.append("<td>").append(cell == null ? "" : cell).append("");
			}
			sb.append("</tr>");
		}
		sb.append("</table>");
		sb.append("<br/>");
		
		return sb.toString();
	}
	
	private String toOrderBook(String table, Flip flip) {
		boolean is_orderbook = table.startsWith("OrderBook") ? true : false;
		
		StringBuilder sb = new StringBuilder();
		sb.append("<br/><b>").append(table).append("</b><br/><br/>");
		
		KxDataModel model = new KxDataModel(flip);
		int rows = model.getRowCount();
		int cols = model.getColumnCount();
		
		sb.append("<table id=\"").append(table).append("\" border=\"1\">");
		// header <b></b>
		
		sb.append("<tr BGCOLOR=\"#99CCFF\">");
		for (int i = 0; i < cols; i++) {
			sb.append("<td>").append(model.getColumnName(i)).append("</td>");
		}
		sb.append("</tr>");
		
		for (int rowIndex = 0; rowIndex < rows; rowIndex++) {
			
			if (rowIndex % 2 == 1) 
				sb.append("<tr BGCOLOR=\"#99CCFF\">");
			else 
				sb.append("<tr>");
			
			for (int columnIndex = 0; columnIndex < cols; columnIndex++) {
				Object cell = model.getValueAt(rowIndex, columnIndex);
				sb.append("<td>").append(cell == null ? "" : cell).append("");
			}
			sb.append("</tr>");
		}
		sb.append("</table>");
		sb.append("<br/>");
		
		return sb.toString();
	}

	// send it to consumer queue
	private void processKdbData() {
		try {
			JSONArray json = new JSONArray();
			
			// add an UI update timer
			json.put("<h><font face='Arial' size='-1'><em>Last update: " + new Date() + "<br/>");
			
			// preserve the sections order from config			
			for (String sec : sections) {
				json.put(toJson(sec, dataTables.get(sec)));
			}
			
			this.queue.put(json);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private String toJson(String table, Flip flip) {
		boolean is_orderbook = table.startsWith("OrderBook") ? true : false;
		
		StringBuilder sb = new StringBuilder();
		sb.append("<b>").append(table).append("</b><br/><br/>");
		
		KxDataModel model = new KxDataModel(flip);
		int rows = model.getRowCount();
		int cols = model.getColumnCount();
		
		sb.append("<table id=\"").append(table).append("\" border=\"1\">");
		// header <b></b>
		
		sb.append("<tr BGCOLOR=\"#99CCFF\">");
		for (int i = 0; i < cols; i++) {
			sb.append("<td>").append(model.getColumnName(i)).append("</td>");
		}
		sb.append("</tr>");
		
		for (int rowIndex = 0; rowIndex < rows; rowIndex++) {
			
			if (rowIndex % 2 == 1) 
				sb.append("<tr BGCOLOR=\"#99CCFF\">");
			else 
				sb.append("<tr>");
			
			for (int columnIndex = 0; columnIndex < cols; columnIndex++) {
				Object cell = model.getValueAt(rowIndex, columnIndex);
				sb.append("<td>").append(cell == null ? "" : cell).append("");
			}
			sb.append("</tr>");
		}
		sb.append("</table>");
		sb.append("<br/>");
		
		return sb.toString();
	}
	
	public static void main(String[] args) {
		String configFile = "C:/Users/gfeng/git/myweb/src/main/webapp/config/atdb.properties";
		String sqlPath = "config";
		final BlockingQueue<Object> queue = new LinkedBlockingQueue<Object>();
		
		System.out.println("xxxx main: ");
		KdbService2 kdb = new KdbService2(configFile, sqlPath, queue);
	}
}
