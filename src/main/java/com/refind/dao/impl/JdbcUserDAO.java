package com.refind.dao.impl;
import com.refind.dao.UserDAO;
import com.refind.database.DatabaseConnection;
import com.refind.exception.DatabaseException;
import com.refind.model.User;
import com.refind.model.enums.Role;
import java.sql.*; import java.util.*;
public class JdbcUserDAO implements UserDAO {
    public User save(User u) { String sql="INSERT INTO users(name,email,password_hash,role) VALUES(?,?,?,?)"; try(Connection c=DatabaseConnection.getConnection(); PreparedStatement p=c.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS)){p.setString(1,u.getName());p.setString(2,u.getEmail());p.setString(3,u.getPasswordHash());p.setString(4,(u.getRole()==null?Role.USER:u.getRole()).name());p.executeUpdate();try(ResultSet r=p.getGeneratedKeys()){if(r.next())u.setId(r.getLong(1));}return findById(u.getId()).orElse(u);}catch(SQLException e){throw new DatabaseException("Failed to save user.",e);}}
    public Optional<User> findById(Long id){return one("SELECT * FROM users WHERE id=?",id);}
    public Optional<User> findByEmail(String email){return one("SELECT * FROM users WHERE email=?",email);}
    private Optional<User> one(String sql,Object value){try(Connection c=DatabaseConnection.getConnection();PreparedStatement p=c.prepareStatement(sql)){p.setObject(1,value);try(ResultSet r=p.executeQuery()){return r.next()?Optional.of(JdbcMappers.user(r)):Optional.empty();}}catch(SQLException e){throw new DatabaseException("Failed to query user.",e);}}
    public List<User> findAll(){List<User> out=new ArrayList<>();try(Connection c=DatabaseConnection.getConnection();PreparedStatement p=c.prepareStatement("SELECT * FROM users ORDER BY id");ResultSet r=p.executeQuery()){while(r.next())out.add(JdbcMappers.user(r));return out;}catch(SQLException e){throw new DatabaseException("Failed to list users.",e);}}
    public User update(User u){String sql="UPDATE users SET name=?,email=?,password_hash=?,role=? WHERE id=?";try(Connection c=DatabaseConnection.getConnection();PreparedStatement p=c.prepareStatement(sql)){p.setString(1,u.getName());p.setString(2,u.getEmail());p.setString(3,u.getPasswordHash());p.setString(4,u.getRole().name());p.setLong(5,u.getId());if(p.executeUpdate()==0)throw new DatabaseException("User not found: "+u.getId());return findById(u.getId()).orElse(u);}catch(SQLException e){throw new DatabaseException("Failed to update user.",e);}}
    public boolean deleteById(Long id){try(Connection c=DatabaseConnection.getConnection();PreparedStatement p=c.prepareStatement("DELETE FROM users WHERE id=?")){p.setLong(1,id);return p.executeUpdate()>0;}catch(SQLException e){throw new DatabaseException("Failed to delete user.",e);}}
}
