package com.accountselling.platform.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

/**
 * User entity representing system users.
 * Contains user account information including credentials and profile data.
 * 
 * เอนทิตี้ผู้ใช้ที่เก็บข้อมูลบัญชีผู้ใช้ รวมถึงข้อมูลการเข้าสู่ระบบและข้อมูลส่วนตัว
 */
@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_user_username", columnList = "username"),
    @Index(name = "idx_user_email", columnList = "email")
})
@Getter
@Setter
@NoArgsConstructor
@ToString(callSuper = true, exclude = "roles")
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class User extends BaseEntity {

    @NotBlank(message = "Username cannot be blank")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    @Column(name = "username", nullable = false, unique = true, length = 50)
    @EqualsAndHashCode.Include
    private String username;

    @NotBlank(message = "Password cannot be blank")
    @Size(min = 60, max = 100, message = "Password hash must be between 60 and 100 characters")
    @Column(name = "password", nullable = false, length = 100)
    private String password;

    @Email(message = "Email should be valid")
    @Size(max = 100, message = "Email cannot exceed 100 characters")
    @Column(name = "email", unique = true, length = 100)
    private String email;

    @Size(max = 100, message = "First name cannot exceed 100 characters")
    @Column(name = "first_name", length = 100)
    private String firstName;

    @Size(max = 100, message = "Last name cannot exceed 100 characters")
    @Column(name = "last_name", length = 100)
    private String lastName;

    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;

    @Column(name = "account_non_expired", nullable = false)
    private Boolean accountNonExpired = true;

    @Column(name = "account_non_locked", nullable = false)
    private Boolean accountNonLocked = true;

    @Column(name = "credentials_non_expired", nullable = false)
    private Boolean credentialsNonExpired = true;

    @ManyToMany(fetch = FetchType.EAGER, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
        name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id"),
        indexes = {
            @Index(name = "idx_user_roles_user_id", columnList = "user_id"),
            @Index(name = "idx_user_roles_role_id", columnList = "role_id")
        }
    )
    private Set<Role> roles = new HashSet<>();

    // One-to-many relationship with Orders
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Order> orders = new HashSet<>();

    // Constructor with username and password
    @Builder
    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }

    // Constructor with username, password, and email
    @Builder
    public User(String username, String password, String email) {
        this.username = username;
        this.password = password;
        this.email = email;
    }



    // Helper methods for role management
    public void addRole(Role role) {
        roles.add(role);
        role.getUsers().add(this);
    }

    public void removeRole(Role role) {
        roles.remove(role);
        role.getUsers().remove(this);
    }

    public boolean hasRole(String roleName) {
        return roles.stream()
                .anyMatch(role -> role.getName().equals(roleName));
    }

    public boolean hasAnyRole(String... roleNames) {
        if (roleNames == null) {
            return false;
        }
        for (String roleName : roleNames) {
            if (hasRole(roleName)) {
                return true;
            }
        }
        return false;
    }

    // Convenience methods
    public String getFullName() {
        if (firstName != null && lastName != null) {
            return firstName + " " + lastName;
        } else if (firstName != null) {
            return firstName;
        } else if (lastName != null) {
            return lastName;
        }
        return username;
    }

    public boolean isAccountNonExpired() {
        return accountNonExpired;
    }

    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }

    public boolean isCredentialsNonExpired() {
        return credentialsNonExpired;
    }

    public boolean isEnabled() {
        return enabled;
    }

    // Helper methods for order management
    public void addOrder(Order order) {
        orders.add(order);
        order.setUser(this);
    }

    public void removeOrder(Order order) {
        orders.remove(order);
        order.setUser(null);
    }

    public boolean hasOrders() {
        return !orders.isEmpty();
    }

    public int getOrderCount() {
        return orders.size();
    }

    public Set<Order> getCompletedOrders() {
        return orders.stream()
                .filter(Order::isCompleted)
                .collect(java.util.stream.Collectors.toSet());
    }

    public Set<Order> getPendingOrders() {
        return orders.stream()
                .filter(Order::isPending)
                .collect(java.util.stream.Collectors.toSet());
    }


}