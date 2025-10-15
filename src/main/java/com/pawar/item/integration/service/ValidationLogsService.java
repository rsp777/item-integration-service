package com.pawar.item.integration.service;

import java.io.IOException;
import java.net.Inet4Address;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.pawar.item.integration.adapter.WmsAdapter;
import com.pawar.item.integration.constants.ValidationLogsConstants;
import com.pawar.item.integration.entity.ItemData;
import com.pawar.item.integration.entity.ValidationLogs;
import com.pawar.item.integration.events.ItemEvent;
import com.pawar.item.integration.repository.ValidationLogsRepository;
import com.pawar.sop.http.service.HttpService;

import jakarta.transaction.Transactional;

@Service
public class ValidationLogsService {

	private static final Logger logger = LoggerFactory.getLogger(ValidationLogsService.class);

	@Autowired
	private ValidationLogsRepository validationLogsRepository;

	public void logResult(Object object, String itemName, String message, Integer status, String validationType,
			String payload, LocalDateTime createdDttm, LocalDateTime lastUpdatedDttm, String createdSource,
			String lastUpdatedSource) {

		if (object instanceof ItemData) {
			ItemData itemData = (ItemData) object;
			if (validationType == null) {
				logValidationResult(itemName, itemData.getItemDesc(), message, status,
						validationType, payload, LocalDateTime.now(), LocalDateTime.now(),
						createdSource, lastUpdatedSource);
			} else {
				logValidationResult(itemName, itemData.getItemDesc(), message, status, validationType, payload,
						createdDttm, lastUpdatedDttm, createdSource, lastUpdatedSource);
			}
		} else if (object instanceof ItemEvent) {
			ItemEvent itemEvent = (ItemEvent) object;
			if (validationType == null) {
				logger.info("message : {}",message);
				logValidationResult(itemName, itemEvent.getItemDesc(), message, status,
						validationType, payload, LocalDateTime.now(), LocalDateTime.now(),
						createdSource, lastUpdatedSource);
			} else {
				logValidationResult(itemName, itemEvent.getItemDesc(), message, status, validationType, payload,
						createdDttm, lastUpdatedDttm, createdSource, lastUpdatedSource);
			}
		}

	}

	@Transactional
	public void logValidationResult(String itemName, String itemDesc, String message, Integer status,
			String itemValidationType, String payload, LocalDateTime created_dttm, LocalDateTime last_updated_dttm,
			String created_source, String last_updated_source) {
		logger.info("itemName : {}",itemName);
		logger.info("itemDesc : {}",itemDesc);
		logger.info("message : {}",message);
		logger.info("status : {}",status);
		logger.info("itemValidationType : {}",itemValidationType);
		logger.info("payload : {}",payload);
		logger.info("created_dttm : {}",created_dttm);
		logger.info("last_updated_dttm : {}",last_updated_dttm);
		logger.info("created_source : {}",created_source);
		logger.info("last_updated_source : {}",last_updated_source);
		ValidationLogs log = new ValidationLogs(itemName, itemDesc, message, status, itemValidationType, payload,
				created_dttm, last_updated_dttm, created_source, last_updated_source);
		logger.info("Save Validation Log : {} ", log);
		validationLogsRepository.save(log);
	}

	@Transactional
	public void updateValidationResult(ValidationLogs validationLog) {
		validationLogsRepository.save(validationLog);
	}

	@Transactional
	public List<ValidationLogs> findByValidationTypeStatus(Integer failed, String validationType) {
		return validationLogsRepository.findByValidationTypeAndStatus(validationType, failed);
	}

	@Transactional
	public List<ValidationLogs> findDistinctItemNameByValidationTypeAndStatus(Integer failed, String validationType) {
		return validationLogsRepository.findDistinctItemNameByValidationTypeAndStatus(validationType, failed);
	}

	public List<ValidationLogs> findByItemName(String itemName) {
		// TODO Auto-generated method stub
		return validationLogsRepository.findByItemName(itemName);
	}

}
