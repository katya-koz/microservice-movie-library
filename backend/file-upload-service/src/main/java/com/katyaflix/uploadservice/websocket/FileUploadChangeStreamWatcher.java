package com.katyaflix.uploadservice.websocket;

import com.katyaflix.uploadservice.entity.FileUpload;
import com.mongodb.client.model.changestream.ChangeStreamDocument;
import com.mongodb.client.model.changestream.FullDocument;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.messaging.ChangeStreamRequest;
import org.springframework.data.mongodb.core.messaging.DefaultMessageListenerContainer;
import org.springframework.data.mongodb.core.messaging.MessageListener;
import org.springframework.data.mongodb.core.messaging.MessageListenerContainer;
import org.springframework.stereotype.Component;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.match;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.newAggregation;
import static org.springframework.data.mongodb.core.query.Criteria.where;

/**
 * Watches file_uploads and pushes every insert/update to the websocket subscribers of that file's job
 */
@Component
@RequiredArgsConstructor
public class FileUploadChangeStreamWatcher {

    private final MongoTemplate mongo;
    private final FileStatusWebSocketHandler handler;
    private MessageListenerContainer container;

    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        container = new DefaultMessageListenerContainer(mongo);
        container.start();

        MessageListener<ChangeStreamDocument<Document>, FileUpload> listener = message -> {
            FileUpload file = message.getBody();   // full document (UPDATE_LOOKUP)
            if (file != null) handler.broadcast(file);
        };

        ChangeStreamRequest<FileUpload> request = ChangeStreamRequest.builder(listener)
                .collection("file_uploads")
                .filter(newAggregation(match(where("operationType").in("insert", "update", "replace"))))
                .fullDocumentLookup(FullDocument.UPDATE_LOOKUP)
                .build();

        container.register(request, FileUpload.class);
    }

    @PreDestroy
    public void stop() {
        if (container != null) container.stop();
    }
}