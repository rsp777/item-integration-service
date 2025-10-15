package com.pawar.item.integration.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.pawar.item.integration.entity.ItemData;


@Repository
public interface ItemDataRepository extends CrudRepository<ItemData, Integer>{
	ItemData findByItemName(String itemName);
}
