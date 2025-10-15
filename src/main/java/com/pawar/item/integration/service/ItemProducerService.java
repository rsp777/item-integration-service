package com.pawar.item.integration.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFutureCallback;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.util.concurrent.ListenableFuture;
import com.pawar.inventory.entity.ASNDto;
import com.pawar.inventory.entity.Category;
import com.pawar.inventory.entity.Item;
import com.pawar.inventory.entity.LpnDto;
import com.pawar.item.integration.constants.ValidationLogsConstants;
import com.pawar.item.integration.entity.ItemData;
import com.pawar.item.integration.events.ItemEvent;
import com.pawar.item.integration.exception.MessageNotSentException;

@Service
public class ItemProducerService {

	private static final Logger logger = LoggerFactory.getLogger(ItemProducerService.class);

	@Autowired
	private KafkaTemplate<String, String> kafkaTemplate;

	private final ObjectMapper objectMapper;
	private ValidationLogsService validationLogsService;

	public ItemProducerService(ValidationLogsService validationLogsService) {
		objectMapper = new ObjectMapper();
		objectMapper.registerModule(new JavaTimeModule());
		this.validationLogsService = validationLogsService;

	}

	public boolean sendMessage(String topic, ItemEvent itemEvent) throws MessageNotSentException {
		boolean published = false;
		String errorMessage = "";
		try {
			String itemDataJsonPayload = createJsonItemDataPayload(itemEvent);
			kafkaTemplate.send(topic, itemDataJsonPayload).get();
			published = true;
			logger.info("Message : {} sent to Topic : {}", itemEvent, topic);
			validationLogsService.logResult(itemEvent, itemEvent.getItemName(),
					String.format(ValidationLogsConstants.ITEM_SUCCESSFULLY_POSTED_WMS_TOPIC, itemEvent.getItemName(),
							topic),
					ValidationLogsConstants.SUCCESS, null, itemDataJsonPayload, LocalDateTime.now(),
					LocalDateTime.now(), ValidationLogsConstants.ITEM_PRODUCER, ValidationLogsConstants.ITEM_PRODUCER);
		} catch (Exception e) {
			e.printStackTrace();
			errorMessage = e.getMessage();
			published = false;
			logger.error("Error sending message", e.getMessage());
			validationLogsService.logResult(itemEvent, itemEvent.getItemName(),
					String.format(ValidationLogsConstants.ITEM_FAILED_TO_POST_TO_WMS_TOPIC, itemEvent.getItemName(),
							topic, errorMessage),
					ValidationLogsConstants.FAILED, null, null, LocalDateTime.now(), LocalDateTime.now(),
					ValidationLogsConstants.ITEM_PRODUCER, ValidationLogsConstants.ITEM_PRODUCER);
		}

		return published;
	}

	public String createJsonItemDataPayload(ItemEvent itemEvent) throws JsonProcessingException {
		Item item = new Item();
		Category category = new Category();
		category.setCategory_name(itemEvent.getCategory());
		item.setItemName(itemEvent.getItemName());
		item.setUnit_length(itemEvent.getUnit_length());
		item.setUnit_width(itemEvent.getUnit_width());
		item.setUnit_height(itemEvent.getUnit_height());
		item.setUnit_volume(itemEvent.getUnit_volume());
		item.setDescription(itemEvent.getItemDesc());
		item.setCategory(category);
		item.setCreated_source("Host");
		item.setLast_updated_source("Host");
		String jsonpayload = objectMapper.writeValueAsString(item);
		return jsonpayload;
	}

}