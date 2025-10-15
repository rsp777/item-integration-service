package com.pawar.item.integration.appconfig;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.Data;

@Component
public class NewItemTopics {

	@Value("${new.item.data.incoming}")
	private String newItemDataIncoming;
	
	@Value("${wms.item.data.incoming}")
	private String wmsItemDataIncoming;

	public String getNewItemDataIncoming() {
		return newItemDataIncoming;
	}

	public void setNewItemDataIncoming(String newItemDataIncoming) {
		this.newItemDataIncoming = newItemDataIncoming;
	}

	public String getWmsItemDataIncoming() {
		return wmsItemDataIncoming;
	}

	public void setWmsItemDataIncoming(String wmsItemDataIncoming) {
		this.wmsItemDataIncoming = wmsItemDataIncoming;
	}
}