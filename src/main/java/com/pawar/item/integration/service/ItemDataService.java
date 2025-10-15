package com.pawar.item.integration.service;

import java.io.IOException;
import java.time.LocalDateTime;

import org.apache.http.client.ClientProtocolException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pawar.item.integration.adapter.WmsAdapter;
import com.pawar.item.integration.constants.ValidationLogsConstants;
import com.pawar.item.integration.entity.ItemData;
import com.pawar.item.integration.repository.ItemDataRepository;

@Component
@Service
public class ItemDataService {

	private static final Logger logger = LoggerFactory.getLogger(ItemDataService.class);
	
	private final ValidationLogsService validationLogsService;
	
	@Autowired
	private ItemDataRepository itemDataRepository;
	
	private final WmsAdapter wmsAdapter;

	public ItemDataService(ValidationLogsService validationLogsService,WmsAdapter wmsAdapter) {
		this.validationLogsService = validationLogsService;
		this.wmsAdapter = wmsAdapter;
	}
	
	@Transactional
	public void saveItem(ItemData itemData,String payload) {

		String itemName = itemData.getItemName();
		validationLogsService.logResult(itemData, itemName, ValidationLogsConstants.SUCCESS_MESSAGE_CREATED, ValidationLogsConstants.SUCCESS,
				ValidationLogsConstants.ITEM_VALIDATION,payload,LocalDateTime.now(), LocalDateTime.now(), ValidationLogsConstants.HOST,
				ValidationLogsConstants.HOST);
		itemData.setStatus(ValidationLogsConstants.CREATED);
		itemDataRepository.save(itemData);
		logger.info("Item Data saved to database : {}", itemData);
	}
	
	@Transactional
	public void updateItem(ItemData itemData) {
		itemData.setStatus(itemData.getStatus());
		itemDataRepository.save(itemData);
	}

	@Transactional
	public boolean isItemExists(String itemName) throws ClientProtocolException, IOException {

		if (itemName != null) {
			String item = "";
			if (itemName.contains(" ")) {
				item = itemName.replaceAll(" ", "%20");
				logger.info("Item with %20: " + itemName);
				return wmsAdapter.isItemExists(item);

			} else {
				return wmsAdapter.isItemExists(itemName);
			}

		} else {
			logger.error("Item Name is null : {}", itemName);
			return false;
		}
	}
}
