package com.pawar.item.integration.constants;

public interface ValidationLogsConstants {
	public static final String RETRIGGER_ITEM_INTEGRATION = "retrigger-item-integration";
	public static final String ITEM_VALIDATION = "ITEM-VALIDATION";
	public static final String ITEM_INTEGRATION_SERVICE = "item-integration-service";
	public static final String HOST = "Host";
	public static final Integer CREATED = 10;
	public static final Integer IN_PROCESS = 30;
	public static final Integer SUCCESS = 90;
	public static final Integer FAILED = 96;
	public static final Integer CANCELLED = 99;
	public static final Integer VALIDATION_PARTIALLY_FAILED = 97;
	public static final String SUCCESS_MESSAGE_CREATED = "Item received to item-integration Service Database";
	public static final String ITEM_IS_NOT_PRESENT_IN_WMS = "Item {} is not present in WMS, Proceeding with dimensions check";
	public static final String ITEM_IS_PRESENT_IN_WMS = "Item {} is present in WMS, aborting item-integration";
	public static final String ITEM_HAS_VALID_DIMENSIONS = "Item Has Valid Dimensions";
	public static final String FAILED_ITEM_VALIDATION = "Item Validation Failed, Item {} is not present in WMS";
	public static final String POST_ITEM_TO_WMS_TOPIC_FLAG = "Post Item %s to WMS Topic %s flag : %s";
	public static final String ITEM_SUCCESSFULLY_POSTED_WMS_TOPIC = "Item %s Sucessfully posted to WMS Topic %s";
	public static final String ITEM_FAILED_TO_POST_TO_WMS_TOPIC = "Item %s failed to posted to WMS Topic %s : %s";
	public static final String ITEM_LISTENER = "ITEM-LISTENER";
	public static final String ITEM_PRODUCER = "ITEM-PRODUCER";


	

}