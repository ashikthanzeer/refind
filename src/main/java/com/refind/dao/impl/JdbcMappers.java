package com.refind.dao.impl;

import com.refind.model.*;
import com.refind.model.enums.*;
import java.sql.ResultSet;
import java.sql.SQLException;

final class JdbcMappers {
    private JdbcMappers() {}
    static User user(ResultSet rs) throws SQLException {
        User u = new User(rs.getString("name"), rs.getString("email"), rs.getString("password_hash"),
                Role.valueOf(rs.getString("role")), rs.getTimestamp("created_at").toLocalDateTime());
        u.setId(rs.getLong("id"));
        return u;
    }
    static Category category(ResultSet rs) throws SQLException {
        Category c = new Category(rs.getString("name")); c.setId(rs.getLong("id")); return c;
    }
    static Location location(ResultSet rs) throws SQLException {
        Location l = new Location(rs.getString("campus"), rs.getString("building"), rs.getString("room")); l.setId(rs.getLong("id")); return l;
    }
    static Item item(ResultSet rs) throws SQLException {
        Item i = new Item();
        i.setId(rs.getLong("id")); i.setTitle(rs.getString("title")); i.setDescription(rs.getString("description"));
        i.setType(ItemType.valueOf(rs.getString("type"))); i.setStatus(ItemStatus.valueOf(rs.getString("status")));
        i.setCategory(new Category()); i.getCategory().setId(rs.getLong("category_id"));
        long locationId = rs.getLong("location_id"); if (!rs.wasNull()) { Location l = new Location(); l.setId(locationId); i.setLocation(l); }
        i.setReportedBy(new User()); i.getReportedBy().setId(rs.getLong("reporter_id"));
        var ts = rs.getTimestamp("reported_at"); if (ts != null) i.setReportedAt(ts.toLocalDateTime());
        i.setImagePath(rs.getString("image_path")); return i;
    }
    static Claim claim(ResultSet rs) throws SQLException {
        Claim c = new Claim(); c.setId(rs.getLong("id"));
        User u = new User(); u.setId(rs.getLong("user_id")); c.setClaimant(u);
        Item i = new Item(); i.setId(rs.getLong("item_id")); c.setItem(i);
        c.setMessage(rs.getString("message")); c.setStatus(ClaimStatus.valueOf(rs.getString("status")));
        var s = rs.getTimestamp("submitted_at"); if (s != null) c.setSubmittedAt(s.toLocalDateTime());
        var d = rs.getTimestamp("decided_at"); if (d != null) c.setDecidedAt(d.toLocalDateTime()); return c;
    }
    static Notification notification(ResultSet rs) throws SQLException {
        Notification n = new Notification(); n.setId(rs.getLong("id")); User u = new User(); u.setId(rs.getLong("user_id")); n.setUser(u);
        n.setText(rs.getString("text")); n.setReadFlag(rs.getBoolean("read_flag"));
        var ts = rs.getTimestamp("created_at"); if (ts != null) n.setCreatedAt(ts.toLocalDateTime()); return n;
    }
}
