package com.pawar.item.integration.entity;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@ToString
@NoArgsConstructor
@AllArgsConstructor
@Data
@Entity
@Table(name = "validation_logs")
public class ValidationLogs {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "validation_logs_id")
	private Integer id;

	@Column(name = "item_name")
	private String itemName;
	
	@Column(name = "item_desc")
	private String itemDesc;

	@Column(name = "message",columnDefinition = "TEXT")
	private String message;

	@Column(name = "status")
	private Integer status;

	@Column(name = "validation_type")
	private String validationType;

	@Column(name = "payload", columnDefinition = "LONGTEXT")
	private String payload;

//	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
	@JsonProperty("created_dttm")
	@Column(name = "created_dttm")
	private LocalDateTime created_dttm;

//	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
	@JsonProperty("last_updated_dttm")
	@Column(name = "last_updated_dttm")
	private LocalDateTime last_updated_dttm;

	@Column(name = "created_source")
	private String created_source;

	@Column(name = "last_updated_source")
	private String last_updated_source;

	public ValidationLogs(String itemName, String itemDesc, String message, Integer status, String validationType,
			String payload, LocalDateTime created_dttm, LocalDateTime last_updated_dttm, String created_source,
			String last_updated_source) {
		super();
		this.itemName = itemName;
		this.itemDesc = itemDesc;
		this.message = message;
		this.status = status;
		this.validationType = validationType;
		this.payload = payload;
		this.created_dttm = created_dttm;
		this.last_updated_dttm = last_updated_dttm;
		this.created_source = created_source;
		this.last_updated_source = last_updated_source;
	}
}