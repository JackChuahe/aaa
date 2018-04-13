package com.tank.aaa.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class PropertiesLoader {
	private PropertiesLoader() {
	}

	public Map loadProperties(String path) {
		Properties pties = new Properties();
		InputStream in = null;
		Map map = new HashMap<>();
		try {
			in = Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
			pties.load(in);
			for (Object obj : pties.keySet()) {
				map.put(obj, pties.get(obj));
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (in != null) {
					in.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return map;
	}
}
