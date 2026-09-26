package com.refind.dao.impl;

import com.refind.dao.ItemDAO;
import com.refind.database.DatabaseConnection;
import com.refind.exception.DatabaseException;
import com.refind.model.Item;
import com.refind.model.enums.ItemStatus;
import com.refind.model.enums.ItemType;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcItemDAO implements ItemDAO {

    private static final String BASE = "SELECT i.*, c.name AS category_name, l.campus, l.building, l.room, "
            + "u.name AS reporter_name, u.email AS reporter_email, u.role AS reporter_role "
            + "FROM items i "
            + "LEFT JOIN categories c ON i.category_id = c.id "
            + "LEFT JOIN locations l ON i.location_id = l.id "
            + "LEFT JOIN users u ON i.reporter_id = u.id";

    @Override
    public Item save(Item i) {
        String sql = "INSERT INTO items(title, description, type, status, category_id, location_id, reporter_id, image_path, reported_at) "
                + "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement p = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            set(p, i);
            if (i.getReportedAt() != null) {
                p.setTimestamp(9, Timestamp.valueOf(i.getReportedAt()));
            } else {
                p.setTimestamp(9, Timestamp.valueOf(LocalDateTime.now()));
            }
            p.executeUpdate();
            try (ResultSet r = p.getGeneratedKeys()) {
                if (r.next()) {
                    i.setId(r.getLong(1));
                }
            }
            return findById(i.getId()).orElse(i);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to save item.", e);
        }
    }

    private void set(PreparedStatement p, Item i) throws SQLException {
        p.setString(1, i.getTitle());
        p.setString(2, i.getDescription());
        p.setString(3, i.getType().name());
        p.setString(4, i.getStatus().name());
        p.setLong(5, i.getCategory().getId());
        if (i.getLocation() == null || i.getLocation().getId() == null) {
            p.setNull(6, Types.INTEGER);
        } else {
            p.setLong(6, i.getLocation().getId());
        }
        p.setLong(7, i.getReportedBy().getId());
        p.setString(8, i.getImagePath());
    }

    @Override
    public Optional<Item> findById(Long id) {
        return one(BASE + " WHERE i.id = ?", id);
    }

    @Override
    public List<Item> findAll() {
        return list(BASE + " ORDER BY i.reported_at DESC", null);
    }

    @Override
    public List<Item> findByType(ItemType t) {
        return list(BASE + " WHERE i.type = ? ORDER BY i.reported_at DESC", t.name());
    }

    @Override
    public List<Item> findByCategory(Long id) {
        return list(BASE + " WHERE i.category_id = ? ORDER BY i.reported_at DESC", id);
    }

    @Override
    public List<Item> findByCategoryAndType(Long categoryId, ItemType type) {
        String sql = BASE + " WHERE i.category_id = ? AND i.type = ? ORDER BY i.reported_at DESC";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {
            p.setLong(1, categoryId);
            p.setString(2, type.name());
            try (ResultSet r = p.executeQuery()) {
                List<Item> a = new ArrayList<>();
                while (r.next()) {
                    a.add(JdbcMappers.item(r));
                }
                return a;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find items by category and type.", e);
        }
    }

    @Override
    public List<Item> findByStatus(ItemStatus s) {
        return list(BASE + " WHERE i.status = ? ORDER BY i.reported_at DESC", s.name());
    }

    @Override
    public List<Item> findByUser(Long id) {
        return list(BASE + " WHERE i.reporter_id = ? ORDER BY i.reported_at DESC", id);
    }

    @Override
    public List<Item> search(String k) {
        String q = "%" + (k == null ? "" : k.trim()) + "%";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement p = c.prepareStatement(BASE + " WHERE i.title LIKE ? OR i.description LIKE ? ORDER BY i.reported_at DESC")) {
            p.setString(1, q);
            p.setString(2, q);
            try (ResultSet r = p.executeQuery()) {
                List<Item> a = new ArrayList<>();
                while (r.next()) {
                    a.add(JdbcMappers.item(r));
                }
                return a;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to search items.", e);
        }
    }

    @Override
    public List<Item> searchByType(String keyword, ItemType type) {
        String q = "%" + (keyword == null ? "" : keyword.trim()) + "%";
        String sql = BASE + " WHERE i.type = ? AND (i.title LIKE ? OR i.description LIKE ?) ORDER BY i.reported_at DESC";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, type.name());
            p.setString(2, q);
            p.setString(3, q);
            try (ResultSet r = p.executeQuery()) {
                List<Item> a = new ArrayList<>();
                while (r.next()) {
                    a.add(JdbcMappers.item(r));
                }
                return a;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to search items by type.", e);
        }
    }

    private Optional<Item> one(String s, Object v) {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement p = c.prepareStatement(s)) {
            p.setObject(1, v);
            try (ResultSet r = p.executeQuery()) {
                return r.next() ? Optional.of(JdbcMappers.item(r)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to query item.", e);
        }
    }

    private List<Item> list(String s, Object v) {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement p = c.prepareStatement(s)) {
            if (v != null) {
                p.setObject(1, v);
            }
            try (ResultSet r = p.executeQuery()) {
                List<Item> a = new ArrayList<>();
                while (r.next()) {
                    a.add(JdbcMappers.item(r));
                }
                return a;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to list items.", e);
        }
    }

    @Override
    public Item update(Item i) {
        String sql = "UPDATE items SET title = ?, description = ?, type = ?, status = ?, "
                + "category_id = ?, location_id = ?, reporter_id = ?, image_path = ? WHERE id = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {
            set(p, i);
            p.setLong(9, i.getId());
            if (p.executeUpdate() == 0) {
                throw new DatabaseException("Item not found: " + i.getId());
            }
            return findById(i.getId()).orElse(i);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update item.", e);
        }
    }

    @Override
    public boolean deleteById(Long id) {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement p = c.prepareStatement("DELETE FROM items WHERE id = ?")) {
            p.setLong(1, id);
            return p.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to delete item.", e);
        }
    }
}
