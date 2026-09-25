package com.refind.service.impl;

import com.refind.dao.ItemDAO;
import com.refind.exception.ValidationException;
import com.refind.model.Item;
import com.refind.model.User;
import com.refind.model.enums.ItemStatus;
import com.refind.model.enums.ItemType;
import com.refind.model.enums.Role;
import com.refind.service.ItemService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class JdbcItemService implements ItemService {

    private final ItemDAO dao;

    public JdbcItemService(ItemDAO dao) {
        this.dao = dao;
    }

    private void validate(Item i) {
        if (i == null) {
            throw new ValidationException("Item cannot be null.");
        }
        if (i.getTitle() == null || i.getTitle().isBlank()) {
            throw new ValidationException("Item title is required.");
        }
        if (i.getType() == null) {
            throw new ValidationException("Item type is required.");
        }
        if (i.getCategory() == null || i.getCategory().getId() == null) {
            throw new ValidationException("Category is required.");
        }
        if (i.getReportedBy() == null || i.getReportedBy().getId() == null) {
            throw new ValidationException("Reporter is required.");
        }
        if (i.getStatus() == null) {
            i.setStatus(ItemStatus.OPEN);
        }
    }

    @Override
    public Item reportItem(Item i) {
        if (i == null) {
            throw new ValidationException("Item cannot be null.");
        }
        if (i.getStatus() == null) {
            i.setStatus(ItemStatus.OPEN);
        }
        if (i.getReportedAt() == null) {
            i.setReportedAt(LocalDateTime.now());
        }
        validate(i);
        return dao.save(i);
    }

    @Override
    public Optional<Item> getItemById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return dao.findById(id);
    }

    @Override
    public List<Item> getAllItems() {
        return dao.findAll();
    }

    @Override
    public List<Item> getItemsByType(ItemType t) {
        if (t == null) {
            return dao.findAll();
        }
        return dao.findByType(t);
    }

    @Override
    public List<Item> getItemsByCategory(Long categoryId) {
        if (categoryId == null) {
            return dao.findAll();
        }
        return dao.findByCategory(categoryId);
    }

    @Override
    public List<Item> getItemsByCategoryAndType(Long categoryId, ItemType type) {
        if (categoryId == null && type == null) {
            return dao.findAll();
        }
        if (categoryId == null) {
            return dao.findByType(type);
        }
        if (type == null) {
            return dao.findByCategory(categoryId);
        }
        return dao.findByCategoryAndType(categoryId, type);
    }

    @Override
    public List<Item> getItemsByStatus(ItemStatus s) {
        if (s == null) {
            return dao.findAll();
        }
        return dao.findByStatus(s);
    }

    @Override
    public List<Item> getItemsByUser(Long id) {
        if (id == null) {
            return List.of();
        }
        return dao.findByUser(id);
    }

    @Override
    public List<Item> searchItems(String k) {
        if (k == null || k.isBlank()) {
            return dao.findAll();
        }
        return dao.search(k);
    }

    @Override
    public List<Item> searchItemsByType(String keyword, ItemType type) {
        if (type == null) {
            return searchItems(keyword);
        }
        if (keyword == null || keyword.isBlank()) {
            return dao.findByType(type);
        }
        return dao.searchByType(keyword, type);
    }

    @Override
    public Item updateItem(Item i) {
        return updateItem(i, null);
    }

    @Override
    public Item updateItem(Item i, User currentUser) {
        validate(i);
        if (i.getId() == null) {
            throw new ValidationException("Item id is required for update.");
        }
        if (currentUser != null && currentUser.getId() != null) {
            Item existing = dao.findById(i.getId())
                    .orElseThrow(() -> new ValidationException("Item not found: " + i.getId()));
            boolean isOwner = existing.getReportedBy() != null
                    && currentUser.getId().equals(existing.getReportedBy().getId());
            boolean isAdmin = currentUser.getRole() == Role.ADMIN;
            if (!isOwner && !isAdmin) {
                throw new ValidationException("Permission denied: You can only edit items that you reported.");
            }
        }
        return dao.update(i);
    }

    @Override
    public boolean deleteItem(Long id) {
        if (id == null) {
            throw new ValidationException("Item id is required for deletion.");
        }
        return dao.deleteById(id);
    }

    @Override
    public boolean deleteItem(Long id, User currentUser) {
        if (id == null) {
            throw new ValidationException("Item id is required for deletion.");
        }
        if (currentUser == null || currentUser.getId() == null) {
            throw new ValidationException("Authentication required: Current user is missing.");
        }
        Item existing = dao.findById(id)
                .orElseThrow(() -> new ValidationException("Item not found: " + id));
        boolean isOwner = existing.getReportedBy() != null
                && currentUser.getId().equals(existing.getReportedBy().getId());
        boolean isAdmin = currentUser.getRole() == Role.ADMIN;
        if (!isOwner && !isAdmin) {
            throw new ValidationException("Permission denied: You can only delete items that you reported.");
        }
        return dao.deleteById(id);
    }
}
