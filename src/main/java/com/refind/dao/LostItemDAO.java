package com.refind.dao;

import com.refind.model.LostItem;

import java.util.List;
import java.util.Optional;

public interface LostItemDAO {

    LostItem save(LostItem lostItem);

    Optional<LostItem> findById(Long id);

    List<LostItem> findAll();

    List<LostItem> findByCategory(Long categoryId);

    List<LostItem> findByUser(Long userId);

    List<LostItem> search(String keyword);

    LostItem update(LostItem lostItem);

    boolean deleteById(Long id);
}
