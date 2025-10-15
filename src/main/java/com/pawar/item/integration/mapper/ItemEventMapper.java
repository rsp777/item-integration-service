package com.pawar.item.integration.mapper;


import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import com.pawar.item.integration.entity.ItemData;
import com.pawar.item.integration.events.ItemEvent;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Component
@Data
@Slf4j
public class ItemEventMapper {

	public ItemData mapItemEventToItemData(ItemEvent itemEvent) {
		ItemData itemData = new ItemData();
		itemData.setItemName(createItemName(itemEvent.getItemDesc()));
		itemData.setItemDesc(itemEvent.getItemDesc());
		itemData.setUnit_length(itemEvent.getUnit_length());
		itemData.setUnit_width(itemEvent.getUnit_width());
		itemData.setUnit_height(itemEvent.getUnit_height());
		itemData.setUnit_volume(itemEvent.getUnit_volume());
		itemData.setCategory(itemEvent.getCategory());
		itemData.setStatus(null);
		itemData.setCreated_dttm(LocalDateTime.now());
		itemData.setLast_updated_dttm(LocalDateTime.now());
		itemData.setCreated_source(itemEvent.getCreated_source());
		itemData.setLast_updated_source(itemEvent.getLast_updated_source());
		
		return itemData;
	}
	
	public String createItemName(String raw_item__name_description) {

		String brand = null;
		String model = null;
		String variant = null;
		String brandCode = null;
		String modelCode = null;
		String digits = null;

		if (raw_item__name_description.contains(" ")) {
			String[] parts = raw_item__name_description.split(" ", 3);
			brand = parts[0];
			model = parts[1];
			variant = (parts.length > 2) ? parts[2] : "";
			brandCode = brand.substring(0, Math.min(brand.length(), 4)).toUpperCase();

			// Keep only the first character of each word in the model name
			modelCode = model.replaceAll("(\\p{Alnum})\\p{Alnum}*", "$1");
			digits = raw_item__name_description.replaceAll("\\D", "");
			if (!digits.isEmpty()) {

				modelCode += digits;// .substring(0, 0);
			}
			// If a variant exists, append its first character to the model code
			if (!variant.isEmpty()) {
				modelCode += variant.substring(0, 1);
			}

			// Append the first digit in the input to the model code
			String item_name = brandCode + "-" + modelCode;
			log.info("Item Name  : {}", item_name);
			return item_name;
		}
		return raw_item__name_description.toUpperCase();

	}

	public ItemEvent mapItemDataToItemEvent(ItemData itemData) {
		ItemEvent itemEvent = new ItemEvent();
		itemEvent.setItemName(itemData.getItemName());
		itemEvent.setItemDesc(itemData.getItemDesc());
		itemEvent.setUnit_length(itemData.getUnit_length());
		itemEvent.setUnit_width(itemData.getUnit_width());
		itemEvent.setUnit_height(itemData.getUnit_height());
		itemEvent.setUnit_volume(itemData.getUnit_volume());
		itemEvent.setCategory(itemData.getCategory());
		itemEvent.setCreated_dttm(LocalDateTime.now());
		itemEvent.setLast_updated_dttm(LocalDateTime.now());
		itemEvent.setCreated_source(itemData.getCreated_source());
		itemEvent.setLast_updated_source(itemData.getLast_updated_source());
		
		return itemEvent;
	}
}
