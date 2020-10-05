package com.alanm.attendant;

import java.io.IOException;
import java.time.Instant;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.http.HttpHost;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;

import com.google.gson.JsonObject;

import jdk.jfr.consumer.RecordedEvent;

public class JfrEventConsumer implements AutoCloseable {
	private static Logger logger = Logger.getLogger(JfrEventConsumer.class.getName());
	
	Consumer<JsonObject> eventConsumer;
	Runnable cleanup;
	
	// Use one of the static construction methods below
	private JfrEventConsumer(Consumer<JsonObject> jsonConsumer, Runnable cleanup) {
		this.eventConsumer = jsonConsumer;
		this.cleanup = cleanup;
	}
	
	public void onCPULoad(RecordedEvent event) {
		JsonObject object = new JsonObject();
		object.addProperty("event", event.getEventType().getName());
		addDouble(object, event, "machineTotal");
		addDouble(object, event, "jvmSystem");
		addDouble(object, event, "jvmUser");
		eventConsumer.accept(object);
	}
	
	
	public void onJavaThreadStatistics(RecordedEvent event) {
		JsonObject object = new JsonObject();
		object.addProperty("event", event.getEventType().getName());
		addDouble(object, event, "activeCount");
		addDouble(object, event, "peakCount");
		eventConsumer.accept(object);
	}
		
	public void onJavaError(RecordedEvent event) {
		JsonObject object = new JsonObject();
		object.addProperty("event", event.getEventType().getName());
		addString(object, event, "eventThread");
		addString(object, event, "class");
		addString(object, event, "message");
		eventConsumer.accept(object);
	}
	
	
	public void onInitialSystemProperty(RecordedEvent event) {
		JsonObject object = new JsonObject();
		object.addProperty("event", event.getEventType().getName());
		addString(object, event, "key");
		addString(object, event, "value");
		eventConsumer.accept(object);
	}
	
	
	public void onInitialEnvironmentVariable(RecordedEvent event) {
		JsonObject object = new JsonObject();
		object.addProperty("event", event.getEventType().getName());
		addString(object, event, "key");
		addString(object, event, "value");
		eventConsumer.accept(object);
	}
	
	private JsonObject addDouble(JsonObject object, RecordedEvent event, String name) {
		object.addProperty(name, event.getDouble(name));
		return object;
	}
	
	private JsonObject addString(JsonObject object, RecordedEvent event, String name) {
		object.addProperty(name, event.getString(name));
		return object;
	}

	@Override
	public void close() throws Exception {
		cleanup.run();
	}
	
	public static JfrEventConsumer createLoggingConsumer() {
		return new JfrEventConsumer(json -> logger.info(json.toString()),() -> {});
	}
	
	public static JfrEventConsumer createElasticConsumer() {
		RestHighLevelClient client = new RestHighLevelClient(
		        RestClient.builder(
		                new HttpHost("localhost", 9200, "http"),
		                new HttpHost("localhost", 9201, "http")));
		
		Runnable cleanup = () -> {
			try {
				client.close();
			} catch (IOException e) {
				logger.log(Level.SEVERE, "Failed to cleanup", e);
			}
		};
		
		Consumer<JsonObject> sendIndexRequest = json -> {
			json.addProperty("time", Instant.now().toString());
			String data = json.toString();
			logger.info(data);
			IndexRequest request = new IndexRequest("jfr");
			request.source(data, XContentType.JSON);
			try {
				client.index(request, RequestOptions.DEFAULT);
			} catch (IOException e) {
				logger.log(Level.SEVERE, "Failed to index", e);
			}
			
		};
		
		return new JfrEventConsumer(sendIndexRequest, cleanup);
	}
}
