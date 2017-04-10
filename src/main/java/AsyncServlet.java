
import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author George Feng (ghf2106@columbia.edu)
 */
@javax.servlet.annotation.WebServlet(urlPatterns = {}, asyncSupported = true)
public final class AsyncServlet extends HttpServlet {
	private static final long serialVersionUID = -6887954111330131167L;
	private final static Logger log = LogManager.getLogger(AsyncServlet.class);
	public static final int CALLBACK_TIMEOUT = 10000; // ms 

	private ExecutorService executor;
	private final BlockingQueue<Object> queue = new LinkedBlockingQueue<Object>();
	private KdbService2 kdb;
	
	// SimpleDateFormat is not thread-safe, so give one to each thread
	private static final ThreadLocal<SimpleDateFormat> formatter = new ThreadLocal<SimpleDateFormat>() {
		@Override
		protected SimpleDateFormat initialValue() {
			return new SimpleDateFormat("HH:mm:ss.S");
		}
	};

	public String formatTime(Date date) {
		return formatter.get().format(date);
	}

	@Override
	public void init() throws ServletException {
		super.init();

		int size = Runtime.getRuntime().availableProcessors();
		executor = Executors.newFixedThreadPool(2*size);
		
		log.info("initialzing AsyncServlet: " + new Date());

		ClassLoader cl = ClassLoader.getSystemClassLoader();
		URL[] urls = ((URLClassLoader) cl).getURLs();

		for (URL url : urls) {
			log.error(";" + url.getFile());
		}

		String ROOT = this.getServletContext().getContextPath();
		log.info("HTTP ROOT: " + this.getServletContext().getContextPath());
		System.out.println("HTTP ROOT: " + this.getServletContext().getContextPath());
		
		File file = new File(ROOT);
		String rootPath = file.getAbsolutePath();
		
		log.info("HTTP ROOT absolutePath: " + rootPath);
		
		// load servlet configs
		ServletConfig config = this.getServletConfig();
		ServletContext context = getServletContext();
		
		String kdbConfig = rootPath+"/"+config.getInitParameter("kdbConfig");
		String sqlPath = rootPath+"/"+config.getInitParameter("sqlPath");
		
		System.out.println(String.format("loading Kdb config/sql dir : %s, %s", kdbConfig, sqlPath));
		
		try {
			log.info("starting Kdb service thread...");

			this.kdb = new KdbService2(kdbConfig, sqlPath, queue);
			
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@Override
	public void destroy() {
		// shutdown all threads
		executor.shutdown();		
		super.destroy();
	}

	@Override
	public void service(HttpServletRequest request, HttpServletResponse response) {
		log.info(String.format(
				"QQQQ:<-- req: %s, path: %s, uri: %s, protocol: %s", 
				request,
				request.getContextPath(), 
				request.getRequestURI(),
				response.getStatus()));

		final AsyncContext async = request.startAsync();
		final HttpSession session = request.getSession();
		
		async.setTimeout(CALLBACK_TIMEOUT);
		
		async.addListener(new AsyncListener() {

			@Override
			public void onComplete(AsyncEvent arg0) throws IOException {
				log.info("Async response complete.");
				
			}

			@Override
			public void onError(AsyncEvent arg0) throws IOException {
				log.error("Async onError.");				
			}

			@Override
			public void onStartAsync(AsyncEvent arg0) throws IOException {
				log.info("onStartAsync: ");
			}

			@Override
			public void onTimeout(AsyncEvent arg0) throws IOException {
				log.warn("onTimeout: ");
			}
			
		});
		
		processResponse(async, session);
	}
	
	private void processResponse(final AsyncContext async, final HttpSession session) {
		System.out.println("xxxx processing async update: " + new Date());
		
		executor.execute(new Runnable() {
			@Override
			public void run() {

				try {
					// get HTML table for UI update - 
					StringBuilder sb = new StringBuilder();
					sb.append("<style type=text/css> TD{font-family: Arial; font-size: 8pt;} table { border-collapse:collapse; } table, td, th { border:1px solid black; } th { background-color:green; color:white; } td {text-align:right;} </style>");
					sb.append("<br>");
					sb.append("<h><font face='Arial' size='-1'><em>Last update: ");
					sb.append(new Date());
					sb.append("<br/>");
					
					//String html = (String) queue.take();
					//sb.append(html);
					//System.out.println("xxxx sending data to ->UI: " + sb.toString());
					
					ServletResponse response = async.getResponse();
					//JSONArray data = new JSONArray();
					//data.put(sb.toString());
					JSONArray data = (JSONArray) queue.take();
					//System.out.println("xxxx sending data to ->UI: " + data.toString());
					System.out.println("xxxx sending data to ->UI: " + data.length());
										
					if (response != null) {
						//System.out.println("xxxx sending data to ->UI: " + sb.toString());
						log.info("UI---->" + sb.toString());
						response.getWriter().write(data.toString());
						async.complete();						
					} else {
						throw new IllegalStateException();  // this is caught below
					}
				
				} catch (IllegalStateException e) {
					log.error("Request Object from context is null! (nothing to worry about");
				}
				catch (Exception e) {
					log.error("ERROR in AsyncServlet", e);
				}

			}
			
		});
	}
}
