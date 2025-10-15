package com.pawar.item.integration.service;

import java.io.IOException;
import java.time.LocalDateTime;

import org.apache.hc.client5.http.HttpHostConnectException;
import org.apache.http.client.ClientProtocolException;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pawar.item.integration.adapter.WmsAdapter;
import com.pawar.item.integration.appconfig.NewItemTopics;
import com.pawar.item.integration.constants.ValidationLogsConstants;
import com.pawar.item.integration.entity.ItemData;
import com.pawar.item.integration.events.ItemEvent;
import com.pawar.item.integration.exception.MessageNotSentException;
import com.pawar.item.integration.mapper.ItemEventMapper;
import com.pawar.item.integration.repository.ItemDataRepository;
import com.pawar.item.integration.service.ValidationLogsService;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
@Service
public class ItemListenerService {

    // Final injected fields (via constructor)
    private ValidationLogsService validationLogsService;
    private final NewItemTopics newItemTopics;
    private ItemProducerService itemProducerService;
    private ItemDataService itemDataService;
    private ItemEventMapper itemEventMapper;
    private WmsAdapter wmsAdapter;
    private ObjectMapper objectMapper;  // Global bean with JavaTimeModule

    // Instance fields for topics (non-static)
    public static String NEW_ITEM_DATA_INCOMING;
    public static String WMS_ITEM_DATA_INCOMING;

    @Autowired
    private ItemDataRepository itemDataRepository;

   
    
    public ItemListenerService(ValidationLogsService validationLogsService, NewItemTopics newItemTopics,
			ItemProducerService itemProducerService, ItemDataService itemDataService, ItemEventMapper itemEventMapper,
			 WmsAdapter wmsAdapter) {
		this.validationLogsService = validationLogsService;
		this.newItemTopics = newItemTopics; // Now non-null
		this.itemProducerService = itemProducerService;
		this.itemDataService = itemDataService;
		this.itemEventMapper = itemEventMapper;
		this.wmsAdapter = wmsAdapter;
		this.objectMapper = new ObjectMapper();
		this.objectMapper.registerModule(new JavaTimeModule());
	}


	@PostConstruct
	public void init() {
		ItemListenerService.NEW_ITEM_DATA_INCOMING = newItemTopics.getNewItemDataIncoming();
		ItemListenerService.WMS_ITEM_DATA_INCOMING = newItemTopics.getWmsItemDataIncoming();
		log.info("ItemListenerService initialized with topics: NEW={}, WMS={}", NEW_ITEM_DATA_INCOMING,
				WMS_ITEM_DATA_INCOMING);

	}

    @Transactional
    @KafkaListener(topics = "#{@itemListenerService.NEW_ITEM_DATA_INCOMING}", groupId = "consumer_group7")
    public void consumeIncomingItem(ConsumerRecord<String, String> consumerRecord, Acknowledgment ack) {
        String key = consumerRecord.key();
        String value = consumerRecord.value();  // Raw JSON string
        int partition = consumerRecord.partition();
        ItemEvent itemEvent = null;
        LocalDateTime now = LocalDateTime.now();

        try {
            log.info("value: {}", value);
            if (value == null || value.trim().isEmpty()) {
                String errorMsg = "Empty or null Kafka payload received from partition " + partition;
                log.error(errorMsg);
                // Log error (object=null, itemName="Unknown")
                validationLogsService.logResult(null, "Unknown", errorMsg, ValidationLogsConstants.FAILED,
                        ValidationLogsConstants.ITEM_LISTENER, value, now, now, ValidationLogsConstants.ITEM_LISTENER, ValidationLogsConstants.ITEM_LISTENER);
                return;  // Don't ack - retry
            }

            itemEvent = objectMapper.readValue(value, ItemEvent.class);
            log.info("Consumed message: {} with key: {} from partition: {}", itemEvent, key, partition);

            // Process: Validate and publish
            publishItemToWMS(itemEvent, value);
            ack.acknowledge();  // Ack only on full success (no exception thrown)

        } catch (JsonProcessingException e) {
            String errorMsg = String.format("JSON parsing failed for payload '%s' from partition %d: %s (%s)", 
                    value != null ? value.substring(0, Math.min(100, value.length())) + "..." : "null", partition, e.getMessage(), e.getClass().getSimpleName());
            log.error(errorMsg, e);
            // Log error (object=null, itemName="Unknown")
            validationLogsService.logResult(null, "Unknown", errorMsg, ValidationLogsConstants.FAILED,
                    ValidationLogsConstants.ITEM_LISTENER, value, now, now, ValidationLogsConstants.ITEM_LISTENER, ValidationLogsConstants.ITEM_LISTENER);
            // Don't ack - retry/DLQ

        } catch (MessageNotSentException e) {
            String errorMsg = String.format("Message not sent to Kafka from partition %d: %s (%s)", partition, e.getMessage(), e.getClass().getSimpleName());
            log.error(errorMsg, e);
            // Log error (use itemEvent if available)
            String itemName = itemEvent != null ? itemEvent.getItemName() : "Unknown";
            validationLogsService.logResult(itemEvent, itemName, errorMsg, ValidationLogsConstants.FAILED,
                    ValidationLogsConstants.ITEM_LISTENER, value, now, now, ValidationLogsConstants.ITEM_LISTENER, ValidationLogsConstants.ITEM_LISTENER);
            // Don't ack - retry

        } catch (Exception e) {
            String errorMsg = String.format("Unexpected error processing Kafka message '%s' from partition %d: %s (%s)", 
                    value != null ? value.substring(0, Math.min(100, value.length())) + "..." : "null", partition, e.getMessage(), e.getClass().getSimpleName());
            log.error(errorMsg, e);
            // Log error (use itemEvent if available)
            String itemName = itemEvent != null ? itemEvent.getItemName() : "Unknown";
            validationLogsService.logResult(itemEvent, itemName, errorMsg, ValidationLogsConstants.FAILED,
                    ValidationLogsConstants.ITEM_LISTENER, value, now, now, ValidationLogsConstants.ITEM_LISTENER, ValidationLogsConstants.ITEM_LISTENER);
            // Don't ack - retry/DLQ
        }
    }

    @Transactional
    public void publishItemToWMS(ItemEvent itemEvent, String value) throws Exception {
        if (itemEvent == null) {
            LocalDateTime now = LocalDateTime.now();
            String errorMsg = "Null ItemEvent received in publishItemToWMS";
            log.warn(errorMsg);
            // Log error (object=null, itemName="Unknown")
            validationLogsService.logResult(null, "Unknown", errorMsg, ValidationLogsConstants.FAILED,
                    ValidationLogsConstants.ITEM_LISTENER, value, now, now, ValidationLogsConstants.ITEM_LISTENER, ValidationLogsConstants.ITEM_LISTENER);
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        
        ItemData itemData = itemEventMapper.mapItemEventToItemData(itemEvent);
        String itemName = itemData.getItemName();
        try {
            log.info("Item Event: {}", itemEvent);
           
            itemDataService.saveItem(itemData, value);  // Save to local DB

            boolean postItemToTopic = validateItem(itemData, value);
            ItemEvent newItemEvent = itemEventMapper.mapItemDataToItemEvent(itemData);
           
            boolean published = publishItemToTopic(postItemToTopic, newItemEvent);
            if (published) {
                itemData.setStatus(ValidationLogsConstants.SUCCESS);
                log.info("Item {} successfully published to WMS topic", itemName);
            } else {
                itemData.setStatus(ValidationLogsConstants.FAILED);
                log.warn("Item {} failed to publish to WMS topic", itemName);
            }
            itemData.setLast_updated_dttm(now);
            itemData.setLast_updated_source(ValidationLogsConstants.ITEM_LISTENER);
            itemDataService.updateItem(itemData);  // Update status

        } catch (Exception e) {
            String errorMsg = String.format("Error in publishItemToWMS for item %s: %s (%s)", itemName, e.getMessage(), e.getClass().getSimpleName());
            log.error(errorMsg, e);
            // Log error
            validationLogsService.logResult(itemEvent, itemName, errorMsg, ValidationLogsConstants.FAILED,
                    ValidationLogsConstants.ITEM_LISTENER, value, now, now, ValidationLogsConstants.ITEM_LISTENER, ValidationLogsConstants.ITEM_LISTENER);
            throw e;  // Re-throw for transaction rollback/retry
        }
    }

    @Transactional
    public boolean publishItemToTopic(boolean postItemToTopic, ItemEvent itemEvent) throws MessageNotSentException {
        String itemName = itemEvent.getItemName();
        String logMessage = String.format(ValidationLogsConstants.POST_ITEM_TO_WMS_TOPIC_FLAG, itemName, WMS_ITEM_DATA_INCOMING, postItemToTopic);
        LocalDateTime now = LocalDateTime.now();

        try {
            if (postItemToTopic) {
                log.info("Post Item To Topic {}: {}", WMS_ITEM_DATA_INCOMING, postItemToTopic);
                // Success log
                validationLogsService.logResult(itemEvent, itemName, logMessage, ValidationLogsConstants.SUCCESS,
                        ValidationLogsConstants.ITEM_VALIDATION, null, now, now, ValidationLogsConstants.ITEM_VALIDATION, ValidationLogsConstants.ITEM_VALIDATION);
                return itemProducerService.sendMessage(WMS_ITEM_DATA_INCOMING, itemEvent);
            } else {
                log.info("Do not post Item To Topic {}: {}", WMS_ITEM_DATA_INCOMING, postItemToTopic);
                // Success log (even if not posting)
                validationLogsService.logResult(itemEvent, itemName, logMessage, ValidationLogsConstants.SUCCESS,
                        ValidationLogsConstants.ITEM_VALIDATION, null, now, now, ValidationLogsConstants.ITEM_VALIDATION, ValidationLogsConstants.ITEM_VALIDATION);
                return false;
            }
        } catch (MessageNotSentException e) {
            String errorMsg = String.format("Failed to publish item %s to topic %s: %s (%s)", itemName, WMS_ITEM_DATA_INCOMING, e.getMessage(), e.getClass().getSimpleName());
            log.error(errorMsg, e);
            // Log error
            validationLogsService.logResult(itemEvent, itemName, errorMsg, ValidationLogsConstants.FAILED,
                    ValidationLogsConstants.ITEM_LISTENER, null, now, now, ValidationLogsConstants.ITEM_LISTENER, ValidationLogsConstants.ITEM_LISTENER);
            throw e;  // Re-throw
        } catch (Exception e) {
            String errorMsg = String.format("Unexpected error publishing item %s: %s (%s)", itemName, e.getMessage(), e.getClass().getSimpleName());
            log.error(errorMsg, e);
            // Log error
            validationLogsService.logResult(itemEvent, itemName, errorMsg, ValidationLogsConstants.FAILED,
                    ValidationLogsConstants.ITEM_LISTENER, null, now, now, ValidationLogsConstants.ITEM_LISTENER, ValidationLogsConstants.ITEM_LISTENER);
            throw new MessageNotSentException(errorMsg, e);
        }
    }

    @Transactional
    public boolean validateItem(ItemData itemData, String payload) throws ClientProtocolException, IOException {
        String itemName = itemData.getItemName();
        Integer status = ValidationLogsConstants.IN_PROCESS;
        boolean postItemToTopic = false;
        LocalDateTime now = LocalDateTime.now();

        try {
            log.info("validateItem method: {}", itemData);

            // Step 1: Check if item exists in WMS (API call)
            boolean isItemExistInWMS;
            try {
                isItemExistInWMS = wmsAdapter.isItemExists(itemName);  // Throws I/O on network/API error
                log.info("WMS API check for item {}: exists = {}", itemName, isItemExistInWMS);
            } catch (IOException e) {
                String errorMsg = String.format("API I/O Error in WMS existence check for item %s: %s (%s)", itemName, e.getMessage(), e.getClass().getSimpleName());
                log.error(errorMsg, e);
                // Log error
                validationLogsService.logResult(itemData, itemName, errorMsg, ValidationLogsConstants.FAILED,
                        ValidationLogsConstants.ITEM_VALIDATION, payload, now, now, ValidationLogsConstants.ITEM_VALIDATION, ValidationLogsConstants.ITEM_VALIDATION);
                // Update item status and save
                itemData.setStatus(ValidationLogsConstants.FAILED);
                itemData.setLast_updated_dttm(now);
                itemData.setLast_updated_source(ValidationLogsConstants.ITEM_VALIDATION);
                itemDataRepository.save(itemData);
                return false;  // Don't post on API error
            }

            if (isItemExistInWMS) {
                log.info("Item {} is present in WMS", itemName);
                String logMessage = ValidationLogsConstants.ITEM_IS_PRESENT_IN_WMS.replace("{}", itemName);
                // Success log
                validationLogsService.logResult(itemData, itemName, logMessage, ValidationLogsConstants.SUCCESS,
                        ValidationLogsConstants.ITEM_VALIDATION, payload, now, now, ValidationLogsConstants.ITEM_VALIDATION, ValidationLogsConstants.ITEM_VALIDATION);
                postItemToTopic = false;
            } else {
                log.info("Item {} is not present in WMS, proceeding with dimensions check", itemName);
                String logMessage = ValidationLogsConstants.ITEM_IS_NOT_PRESENT_IN_WMS.replace("{}", itemName);
                // Success log
                validationLogsService.logResult(itemData, itemName, logMessage, ValidationLogsConstants.SUCCESS,
                        ValidationLogsConstants.ITEM_VALIDATION, payload, now, now, ValidationLogsConstants.ITEM_VALIDATION, ValidationLogsConstants.ITEM_VALIDATION);

                // Step 2: Validate dimensions (API or local check)
                boolean dimensionsValid;
                try {
                    dimensionsValid = wmsAdapter.hasValidDimensions(itemData);  // Throws I/O if API
                    log.info("Dimensions validation for item {}: valid = {}", itemName, dimensionsValid);
                } catch (Exception e) {
                    String errorMsg = String.format("Error in dimensions validation for item %s: %s (%s)", itemName, e.getMessage(), e.getClass().getSimpleName());
                    log.error(errorMsg, e);
                    // Log error
                    validationLogsService.logResult(itemData, itemName, errorMsg, ValidationLogsConstants.FAILED,
                            ValidationLogsConstants.ITEM_VALIDATION, payload, now, now, ValidationLogsConstants.ITEM_VALIDATION, ValidationLogsConstants.ITEM_VALIDATION);
                    // Update item status and save
                    itemData.setStatus(ValidationLogsConstants.FAILED);
                    itemData.setLast_updated_dttm(now);
                    itemData.setLast_updated_source(ValidationLogsConstants.ITEM_VALIDATION);
                    itemDataRepository.save(itemData);
                    return false;  // Don't post on API error
                }

                if (dimensionsValid) {
                    String dimLogMessage = "All dimensions provided and valid (non-negative)";
                    // Success log
                    validationLogsService.logResult(itemData, itemName, dimLogMessage, ValidationLogsConstants.SUCCESS,
                            ValidationLogsConstants.ITEM_VALIDATION, null, now, now, ValidationLogsConstants.ITEM_VALIDATION, ValidationLogsConstants.ITEM_VALIDATION);
                    postItemToTopic = true;
                } else {
                    String dimLogMessage = "Invalid dimensions for item " + itemName;
                    // Failed log for dimensions
                    validationLogsService.logResult(itemData, itemName, dimLogMessage, ValidationLogsConstants.FAILED,
                            ValidationLogsConstants.ITEM_VALIDATION, payload, now, now, ValidationLogsConstants.ITEM_VALIDATION, ValidationLogsConstants.ITEM_VALIDATION);
                    postItemToTopic = false;
                }
            }

            // Update status and save (final step)
            status = postItemToTopic ? ValidationLogsConstants.SUCCESS : ValidationLogsConstants.FAILED;
            itemData.setStatus(status);
            itemData.setLast_updated_source(ValidationLogsConstants.ITEM_VALIDATION);
            itemData.setLast_updated_dttm(now);
            itemDataRepository.save(itemData);
            return postItemToTopic;

        } catch (Exception e) {
            String errorMsg = String.format("Unexpected error in validateItem for %s: %s (%s)", itemName, e.getMessage(), e.getClass().getSimpleName());
            log.error(errorMsg, e);
            // Log error
            validationLogsService.logResult(itemData, itemName, errorMsg, ValidationLogsConstants.FAILED,
                    ValidationLogsConstants.ITEM_VALIDATION, payload, now, now, ValidationLogsConstants.ITEM_VALIDATION, ValidationLogsConstants.ITEM_VALIDATION);
            // Update item status and save
            itemData.setStatus(ValidationLogsConstants.FAILED);
            itemData.setLast_updated_dttm(now);
            itemData.setLast_updated_source(ValidationLogsConstants.ITEM_VALIDATION);
            itemDataRepository.save(itemData);
            throw e;  // Re-throw for rollback/retry
        }
    }
}