package com.esmartit.seendevicesdatastore.importdata.porterhead.integration.writer;

import com.esmartit.seendevicesdatastore.dataimport.MerakiPayload;
import com.esmartit.seendevicesdatastore.importdata.porterhead.integration.configuration.ApplicationConfiguration;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.messaging.Sink;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.file.FileNameGenerator;
import org.springframework.integration.handler.LoggingHandler;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class MessageProcessingIntegrationFlow {

    public static final String OUTBOUND_FILENAME_GENERATOR = "outboundFilenameGenerator";
    public static final String FILE_WRITING_MESSAGE_HANDLER = "fileWritingMessageHandler";
    @Autowired
    public File inboundOutDirectory;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private Sink consumer;

    /**
     * Reverse the contents of the string and write it out using a filename generator to name the file
     *
     * @param fileWritingMessageHandler
     * @return
     */
    @Bean
    public IntegrationFlow writeToFile(@Qualifier("fileWritingMessageHandler") MessageHandler fileWritingMessageHandler) {
        return IntegrationFlows.from(ApplicationConfiguration.INBOUND_CHANNEL)
                .transform(m -> {
                    System.out.println("Starting file...");
                    return getMerakiPayload((String) m);
                })
                .handle(fileWritingMessageHandler)
                .get();
    }

    private MerakiPayload getMerakiPayload(String m) {
        try {
            return objectMapper.readValue(m, MerakiPayload.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }


    @Bean(name = FILE_WRITING_MESSAGE_HANDLER)
    public MessageHandler fileWritingMessageHandler() {
        return message -> {
            MerakiPayload payload = (MerakiPayload) message.getPayload();
            payload.getData().mapToDeviceSeenEvents()
                    .forEach(x -> consumer.input().send(MessageBuilder.withPayload(x).build()));
            System.out.println("Finished file" + message.getHeaders().get("file_name"));
        };
    }

    @Bean(name = OUTBOUND_FILENAME_GENERATOR)
    public FileNameGenerator outboundFileName(@Value("${out.filename.dateFormat}") String dateFormat, @Value("${out.filename.suffix}") String filenameSuffix) {
        return message -> DateTimeFormatter.ofPattern(dateFormat).format(LocalDateTime.now()) + filenameSuffix;
    }

}
