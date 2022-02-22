package com.example.demo.controller;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DemoController {
	
	private Environment env;
	
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	@Autowired
	public DemoController(Environment env) {
		this.env = env;
	}
	
	@GetMapping("/status")
	public String status() {		
		if (logger.isInfoEnabled()) {
			logger.info("Connect GET /status");
			logger.info("server.port : " + env.getProperty("server.port")
					+ "\nlocal.server.port : " + env.getProperty("local.server.port")
					+ "\nTest INFO LOG");
			logger.warn("TEST WARN LOG");
			logger.error("TEST ERR LOG");
		}
		return String.format("Hello DemoController, this sever started on PORT : " + env.getProperty("server.port")
				+ ", local server PORT : " + env.getProperty("local.server.port"));
	}
	
	@GetMapping("/err")
	public String errtest() throws IOException {
		String result = "Error Test";
		
		BufferedReader br;
		
		br = new BufferedReader(new FileReader("nothing"));
		br.readLine();
		br.close();
		
		
		return result;
	}
	
	@PostMapping("put")
	public HashMap<String, Object> putItem(@RequestBody HashMap<String, Object> item) {	
		
		logger.info("Request Item : " + item);
		
		for (Map.Entry<String, Object> entry : item.entrySet()) {
			String key = entry.getKey();
			Object value = entry.getValue();
			
			logger.info("key : " + key);
			logger.info("value : " + value);
		}
		
		return item;
	}
	
}
