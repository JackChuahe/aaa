package MyPcap4jModule.com.tank.aaa;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.tank.aaa.entity.ThreadPoolManager;
import com.tank.aaa.module.BaseModule;
import com.tank.aaa.util.AppContext;

/**
 * @author caihe
 * @since 2018.4.12
 *
 */
public class CoreMainApp {
	private static List<BaseModule> modules = new ArrayList<BaseModule>();
	private static Logger logger = LogManager.getLogger(LogManager.ROOT_LOGGER_NAME);

	private static AppContext ctx = null;
	private static ThreadPoolManager threadPoolService = null;

	public static void main(String[] args) {
		// loading module
		try {
			Constructor<AppContext> m = AppContext.class.getDeclaredConstructor();
			m.setAccessible(true);
			ctx = m.newInstance();
		} catch (InstantiationException | IllegalAccessException | NoSuchMethodException | SecurityException
				| IllegalArgumentException | InvocationTargetException e) {
			logger.catching(e);
		}
		if (ctx == null) {
			ctx.exit(-1);
		}
		loadConfigProperties();
		initModules();
		threadPoolService = ctx.getService(ThreadPoolManager.class);
		if(threadPoolService != null) {
			startupModules();
		}else {
			logger.error("ThreadPool create fialed!");
			ctx.exit(-1);
		}
	}

	// load properties to module class
	public static void loadConfigProperties() {
		logger.info("Loading Modules...");
		InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream("modules.config");
		BufferedReader frb = new BufferedReader(new InputStreamReader(in));
		String line = "";
		try {
			while ((line = frb.readLine()) != null && line != "") {
				Class cls = Class.forName(line);
				Object obj = cls.newInstance();
				if (obj instanceof BaseModule) {
					modules.add((BaseModule) obj);
					logger.info("Loaded Module " + line + " success.");
				} else {
					throw new RuntimeException("Illegal Module of: " + line);
				}
			}
		} catch (IOException | ClassNotFoundException | InstantiationException | IllegalAccessException e) {
			logger.catching(e);
			ctx.exit(-1);
		}
		logger.info("Load Modules finished.");
	}

	// initial modules
	public static void initModules() {
		for (BaseModule module : modules) {
			module.init(ctx);
		}
	}

	/**
	 * start up modules
	 */
	public static void startupModules() {
		logger.info("Starting up modules...");
		for(BaseModule module : modules) {
			threadPoolService.execute(module);
		logger.info("Start up "+module.getName()+" modules successful!");
		}
		logger.info("Start up modules finished.");
		logger.info("All Thread Num: "+threadPoolService.getAllThreadNum() + " Thread pool Num: "+threadPoolService.getPoolActiveThreadNum());
	}
}
