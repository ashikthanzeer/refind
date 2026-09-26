package com.refind.service;

import com.refind.model.Item;
import com.refind.model.User;
import com.refind.model.enums.ItemStatus;
import com.refind.model.enums.ItemType;

import java.util.List;
import java.util.Optional;

public interface ItemService {
    Item reportItem(Item item);
    Optional<Item> getItemById(Long id);
    List<Item> getAllItems();
    List<Item> getItemsByType(ItemType type);
    List<Item> getItemsByCategory(Long categoryId);
    List<Item> getItemsByCategoryAndType(Long categoryId, ItemType type);
    List<Item> getItemsByStatus(ItemStatus status);
    List<Item> getItemsByUser(Long userId);
    List<Item> searchItems(String keyword);
    List<Item> searchItemsByType(String keyword, ItemType type);
    Item updateItem(Item item);
    Item updateItem(Item item, User currentUser);
    boolean deleteItem(Long id);
    boolean deleteItem(Long id, User currentUser);
}
