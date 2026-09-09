package com.refind.dao;

import com.refind.model.FoundItem;
import com.refind.model.enums.ItemStatus;

import java.util.List;
import java.util.Optional;

public interface FoundItemDAO {

    FoundItem save(FoundItem foundItem);

    Optional<FoundItem> findById(Long id);

    List<FoundItem> findAll();

    List<FoundItem> findByCategory(Long categoryId);

    List<FoundItem> findByStatus(ItemStatus status);

    List<FoundItem> findByUser(Long userId);

    List<FoundItem> search(String keyword);

    FoundItem update(FoundItem foundItem);

    boolean deleteById(Long id);
}
