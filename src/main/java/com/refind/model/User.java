package com.refind.model;

import com.refind.model.enums.Role;

import java.time.LocalDateTime;

public class User {

    private Long id;
    private String name;
    private String email;
    private String password_hash;
    private Role role;
    private LocalDateTime created_at;

    public User() {
    }

    public User(String name, String email, String password_hash,
                Role role, LocalDateTime created_at) {
        this.name = name;
        this.email = email;
        this.password_hash = password_hash;
        this.role = role;
        this.created_at = created_at;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getpassword_hash() {
        return password_hash;
    }

    public void setpassword_hash(String password_hash) {
        this.password_hash = password_hash;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public LocalDateTime getcreated_at() {
        return created_at;
    }

    public void setcreated_at(LocalDateTime created_at) {
        this.created_at = created_at;
    }
}
