package com.refind.dao;

import com.refind.model.Item;
import com.refind.model.enums.ItemStatus;
import com.refind.model.enums.ItemType;
import java.util.List;
import java.util.Optional;

public interface ItemDAO {
    Item save(Item item);
    Optional<Item> findById(Long id);
    List<Item> findAll();
    List<Item> findByType(ItemType type);
    List<Item> findByCategory(Long categoryId);
    List<Item> findByStatus(ItemStatus status);
    List<Item> findByUser(Long userId);
    List<Item> search(String keyword);
    Item update(Item item);
    boolean deleteById(Long id);
}
