package com.pawar.item.integration.events;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import lombok.Data;

@Data
@Component
public class ItemEvent {

	private Integer id;
	private String itemName;
	private String itemDesc;
	private float unit_length;
	private float unit_width;
	private float unit_height;
	private float unit_volume;
	private String category;

//	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
	private LocalDateTime created_dttm;
	
//	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
	private LocalDateTime last_updated_dttm;
	private String created_source;
	private String last_updated_source;
}