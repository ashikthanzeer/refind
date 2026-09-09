package com.refind.service;

import com.refind.model.FoundItem;
import com.refind.model.LostItem;

import java.util.List;
import java.util.Optional;

public interface ItemService {

    LostItem reportLostItem(LostItem lostItem);

    FoundItem reportFoundItem(FoundItem foundItem);

    Optional<LostItem> getLostItemById(Long id);

    Optional<FoundItem> getFoundItemById(Long id);

    List<LostItem> getAllLostItems();

    List<FoundItem> getAllFoundItems();

    List<LostItem> searchLostItems(String keyword);

    List<FoundItem> searchFoundItems(String keyword);

    LostItem updateLostItem(LostItem lostItem);

    FoundItem updateFoundItem(FoundItem foundItem);

    boolean deleteLostItem(Long id);

    boolean deleteFoundItem(Long id);
}
