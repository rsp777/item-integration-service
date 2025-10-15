package com.pawar.item.integration.entity;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
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
@Table(name = "item_data")
public class ItemData {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "item_data_id")
	private Integer id;

	@Column(name = "item_name")
	private String itemName;

	@Column(name = "item_desc")
	private String itemDesc;

	@Column(name = "unit_length")
	private Float unit_length;

	@Column(name = "unit_width")
	private Float unit_width;

	@Column(name = "unit_height")
	private Float unit_height;

	@Column(name = "unit_volume")
	private Float unit_volume;
	
	@Column(name = "category")
	private String category;

	@Column(name = "status")
	private Integer status;

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
}