package com.github.richardwilly98.elasticsearch;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.util.concurrent.EsExecutors;
import org.elasticsearch.common.xcontent.support.XContentMapValues;
import org.elasticsearch.river.AbstractRiverComponent;
import org.elasticsearch.river.River;
import org.elasticsearch.river.RiverName;
import org.elasticsearch.river.RiverSettings;

public class MyDummyRiver extends AbstractRiverComponent implements River {

	private final Client client;
	private volatile Thread indexer;
	
	@Inject
	protected MyDummyRiver(RiverName riverName, RiverSettings settings, Client client) {
		super(riverName, settings);
		this.client = client;
	}

	public void start() {
		logger.info("start: {} - {}", riverName.getName(), this.hashCode());
		if (!client.admin().indices().prepareExists(getIndexName()).get().isExists()) {
			if (client.admin().indices().prepareCreate(getIndexName()).get().isAcknowledged()) {
				logger.info("Index {} has been succesfully created", getIndexName());
			} else {
				logger.warn("Could not create index {}", getIndexName());
			}
		} else {
			logger.debug("Index {} already exists", getIndexName());
		}
		indexer = EsExecutors.daemonThreadFactory(settings.globalSettings(), "indexer_" + this.hashCode()).newThread(
                new MyDummyIndexer());
		indexer.start();
	}

	public void close() {
		logger.info("close: {}", riverName.getName());
		if (client.admin().indices().prepareExists(getIndexName()).get().isExists()) {
			if (client.admin().indices().prepareDelete(getIndexName()).get().isAcknowledged()) {
				logger.info("Index {} has been succesfully delete", getIndexName());
			} else {
				logger.warn("Could not delete index {}", getIndexName());
				
			}
		} else {
			logger.debug("Index {} already deleted", getIndexName());
		}
		indexer.interrupt();
		indexer = null;
	}

	private String getIndexName() {
		return XContentMapValues.extractValue("index.name", settings.settings()).toString();
	}
}
