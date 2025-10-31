package com.pawar.item.integration.adapter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Objects;

import org.apache.hc.client5.http.ClientProtocolException;
import org.apache.hc.client5.http.HttpHostConnectException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import com.pawar.item.integration.constants.ValidationLogsConstants;
import com.pawar.item.integration.entity.ItemData;
import com.pawar.item.integration.service.ValidationLogsService;
import com.pawar.sop.http.exception.RestClientException;
import com.pawar.sop.http.service.HttpService;

import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Component
@Data // Generates setters for deps (required for no-args + manual setting)
@Slf4j
public class WmsAdapter {

	@Value("${wms.item.url}")
	private String wmsItemUrl;

	@Value("${wms.category.url}")
	private String wmsCategoryUrl;

	private ValidationLogsService validationLogsService;
	private final HttpService httpService;

	public WmsAdapter(HttpService httpService, ValidationLogsService validationLogsService) {
		this.httpService = httpService;
		this.validationLogsService = validationLogsService;
	}

	@PostConstruct // Runs after injection to validate deps
	public void validateDependencies() {
		Assert.notNull(httpService, "HttpService must be injected");
		Assert.notNull(validationLogsService, "ValidationLogsService must be injected");
		Assert.hasText(wmsItemUrl, "wms.item.url must be configured");
		Assert.hasText(wmsCategoryUrl, "wms.category.url must be configured");
		log.info("WmsAdapter initialized successfully with all dependencies");
	}

	public boolean isItemExists(String itemName){
		if (itemName == null || itemName.trim().isEmpty()) {
			String errorMsg = "itemName is null or empty – cannot check WMS existence";
			log.warn(errorMsg);
			LocalDateTime now = LocalDateTime.now();
			// Log as error (no itemData available here; handle in caller if needed)
			validationLogsService.logResult(null, "Unknown", errorMsg, ValidationLogsConstants.FAILED,
					ValidationLogsConstants.ITEM_VALIDATION, null, now, now, ValidationLogsConstants.ITEM_VALIDATION,
					ValidationLogsConstants.ITEM_VALIDATION);
			return false;
		}

		String url = getWmsItemUrl().replace("{itemName}", itemName);
		log.info("Checking Item if exists in WMS: URL = {}", url);

		try {
			ResponseEntity<String> response = httpService.restCall(null, url, HttpMethod.GET, null, null);
			log.info("WMS Response for {}: Status={}, Body={}", itemName, response.getStatusCodeValue(),
					response.getBody() != null
							? response.getBody().substring(0, Math.min(200, response.getBody().length())) + "..."
							: "null");

			if (response.getStatusCode().is2xxSuccessful()) {
				log.info("Item {} exists in WMS: result = true", itemName);
				return true; // Exists
			} else if (response.getStatusCode().value() == 404) {
				log.info("Item {} not found in WMS (404): result = false", itemName);
				return false; // Not exists (expected)
			} else {
				// Unexpected status (e.g., 400, 500)
				String errorMsg = String.format("Unexpected WMS response for item %s: Status=%d, Body=%s", itemName,
						response.getStatusCodeValue(), response.getBody());
				log.error(errorMsg);
				LocalDateTime now = LocalDateTime.now();
				// Log as failed (assume no payload here; pass from caller if needed)
				validationLogsService.logResult(null, itemName, errorMsg, ValidationLogsConstants.FAILED,
						ValidationLogsConstants.ITEM_VALIDATION, null, now, now,
						ValidationLogsConstants.ITEM_VALIDATION, ValidationLogsConstants.ITEM_VALIDATION);
				
				return false;
			}

		} 
//		catch (HttpHostConnectException e) {
//			// Specific connect error (e.g., refused)
//			String errorMsg = String.format("Connection refused to WMS for item %s: %s (URL: %s)", itemName,
//					e.getMessage(), url);
//			log.error(errorMsg, e);
//			LocalDateTime now = LocalDateTime.now();
//			// Log as failed
//			validationLogsService.logResult(null, itemName, errorMsg, ValidationLogsConstants.FAILED,
//					ValidationLogsConstants.ITEM_VALIDATION, null, now, now, ValidationLogsConstants.ITEM_VALIDATION,
//					ValidationLogsConstants.ITEM_VALIDATION);
//			//throw e; // Re-throw to let caller (validateItem) handle (set FAILED, don't post)
//
//		} 
		catch (HttpClientErrorException | HttpServerErrorException e) {
			// Client/Server HTTP errors (e.g., 4xx/5xx after connection)
			String errorMsg = String.format("HTTP error from WMS for item %s: Status=%d, Response=%s", itemName,
					e.getStatusCode().value(), e.getResponseBodyAsString());
			log.error(errorMsg, e);
			LocalDateTime now = LocalDateTime.now();
			// Log as failed
			validationLogsService.logResult(null, itemName, errorMsg, ValidationLogsConstants.FAILED,
					ValidationLogsConstants.ITEM_VALIDATION, null, now, now, ValidationLogsConstants.ITEM_VALIDATION,
					ValidationLogsConstants.ITEM_VALIDATION);
			
			return false;

		} catch (RestClientException e) {
			// Other RestClient issues (e.g., timeout, parse error)
			String errorMsg = String.format("RestClient error checking WMS for item %s: %s (URL: %s)", itemName,
					e.getMessage(), url);
			log.error(errorMsg, e);
			LocalDateTime now = LocalDateTime.now();
			// Log as failed
			validationLogsService.logResult(null, itemName, errorMsg, ValidationLogsConstants.FAILED,
					ValidationLogsConstants.ITEM_VALIDATION, null, now, now, ValidationLogsConstants.ITEM_VALIDATION,
					ValidationLogsConstants.ITEM_VALIDATION);
			return false;

		} catch (Exception e) {
			// Unexpected (e.g., NullPointer)
			String errorMsg = String.format("Unexpected error checking WMS for item %s: %s", itemName, e.getMessage());
			log.error(errorMsg, e);
			LocalDateTime now = LocalDateTime.now();
			// Log as failed
			validationLogsService.logResult(null, itemName, errorMsg, ValidationLogsConstants.FAILED,
					ValidationLogsConstants.ITEM_VALIDATION, null, now, now, ValidationLogsConstants.ITEM_VALIDATION,
					ValidationLogsConstants.ITEM_VALIDATION);
			return false;
		}
	}

	public boolean hasValidDimensions(ItemData itemData) {

		Float length = itemData.getUnit_length();
		Float width = itemData.getUnit_width();
		Float height = itemData.getUnit_height();
		Float volume = length * width * height;

		if (itemData == null) {
			log.info("itemData is null");
			return false;
		}
		if (Objects.isNull(length) && Objects.isNull(width) && Objects.isNull(height) && Objects.isNull(volume)) {
			log.info("All Item Dims are null");
			String.format("All Item Dims are null", null);
			validationLogsService.logResult(itemData, itemData.getItemName(),
					String.format("All Item Dims are null", null), ValidationLogsConstants.FAILED,
					ValidationLogsConstants.ITEM_VALIDATION, null, LocalDateTime.now(), LocalDateTime.now(),
					ValidationLogsConstants.ITEM_VALIDATION, ValidationLogsConstants.ITEM_VALIDATION);
			return false;
		}
		if (Objects.isNull(length) || Objects.isNull(width) || Objects.isNull(height) || Objects.isNull(volume)) {
			log.warn(
					"Mixed dimensions: some null, some provided - invalid configuration, (length={}, width={}, height={}, volume={})",
					length, width, height, volume);
			validationLogsService.logResult(itemData, itemData.getItemName(), String.format(
					"Mixed dimensions: some null, some provided - invalid configuration, (length=%f, width=%f, height=%f, volume=%f)",
					length, width, height, volume), ValidationLogsConstants.FAILED,
					ValidationLogsConstants.ITEM_VALIDATION, null, LocalDateTime.now(), LocalDateTime.now(),
					ValidationLogsConstants.ITEM_VALIDATION, ValidationLogsConstants.ITEM_VALIDATION);

		}
		if (length == 0 || width == 0 || height == 0 || volume == 0) {
			log.warn("Invalid dimensions: one or more values are zero (length={}, width={}, height={}, volume={})",
					length, width, height, volume);
			validationLogsService.logResult(itemData, itemData.getItemName(), String.format(
					"Invalid dimensions: one or more values are zero (length=%f, width=%f, height=%f, volume=%f)",
					length, width, height, volume), ValidationLogsConstants.FAILED,
					ValidationLogsConstants.ITEM_VALIDATION, null, LocalDateTime.now(), LocalDateTime.now(),
					ValidationLogsConstants.ITEM_VALIDATION, ValidationLogsConstants.ITEM_VALIDATION);

			return false;
		}
		if (length < 0 || width < 0 || height < 0 || volume < 0) {
			log.warn("Invalid dimensions: one or more values are negative (length={}, width={}, height={}, volume={})",
					length, width, height, volume);
			validationLogsService.logResult(itemData, itemData.getItemName(), String.format(
					"Invalid dimensions: one or more values are negative (length=%f, width=%f, height=%f, volume=%f)",
					length, width, height, volume), ValidationLogsConstants.FAILED,
					ValidationLogsConstants.ITEM_VALIDATION, null, LocalDateTime.now(), LocalDateTime.now(),
					ValidationLogsConstants.ITEM_VALIDATION, ValidationLogsConstants.ITEM_VALIDATION);

			return false;
		}

		log.info("All dimensions provided and valid (non-negative)");
		validationLogsService.logResult(itemData, itemData.getItemName(),
				String.format("All dimensions provided and valid (non-negative)", length, width, height, volume),
				ValidationLogsConstants.SUCCESS, ValidationLogsConstants.ITEM_VALIDATION, null, LocalDateTime.now(),
				LocalDateTime.now(), ValidationLogsConstants.ITEM_VALIDATION, ValidationLogsConstants.ITEM_VALIDATION);
		return true;
	}

	public boolean isCategoryExists(String category) {
		if (category == null) {
			log.info("category is null");
			return false;
		} else {
			log.info("Checking category if exists in WMS");
			ResponseEntity<String> response = httpService.restCall(null,
					getWmsCategoryUrl().replace("{category}", category), HttpMethod.GET, null, null);
			boolean result = response.getStatusCode().is2xxSuccessful();
			log.info("result : {}", result);
			return result;
		}

	}

}