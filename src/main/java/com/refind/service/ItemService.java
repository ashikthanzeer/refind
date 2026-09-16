package com.refind.service;

import com.refind.model.Item;
import com.refind.model.enums.ItemStatus;
import java.util.List;
import java.util.Optional;

public interface ItemService {
    Item reportItem(Item item);
    Optional<Item> getItemById(Long id);
    List<Item> getAllItems();
    List<Item> getItemsByCategory(Long categoryId);
    List<Item> getItemsByStatus(ItemStatus status);
    List<Item> getItemsByUser(Long userId);
    List<Item> searchItems(String keyword);
    Item updateItem(Item item);
    boolean deleteItem(Long id);
}
