package com.github.richardwilly98.elasticsearch;

import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.ESLoggerFactory;

public class MyDummyIndexer implements Runnable {

    private final ESLogger logger = ESLoggerFactory.getLogger(this.getClass().getName());
	
    public void run() {
		while (true) {
			try {
				logger.info("Running {}", Thread.currentThread().getName());
				Thread.sleep(500);
			} catch (Exception e) {
				e.printStackTrace();
				Thread.currentThread().interrupt();
				return;
			}
		}

	}

}
