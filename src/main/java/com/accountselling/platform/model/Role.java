package com.accountselling.platform.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

/**
 * Role entity representing user roles in the system.
 * Defines different permission levels for users (e.g., USER, ADMIN).
 * 
 * บทบาทของผู้ใช้ในระบบ เช่น ผู้ใช้ทั่วไป หรือ ผู้ดูแลระบบ
 */
@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = "users")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotBlank(message = "Role name cannot be blank")
    @Size(max = 50, message = "Role name cannot exceed 50 characters")
    @Column(name = "name", nullable = false, unique = true, length = 50)
    @EqualsAndHashCode.Include
    private String name;

    @Size(max = 255, message = "Role description cannot exceed 255 characters")
    @Column(name = "description", length = 255)
    private String description;

    @ManyToMany(mappedBy = "roles", fetch = FetchType.LAZY)
    private Set<User> users = new HashSet<>();

    // Constructor with name
    public Role(String name) {
        this.name = name;
    }

    // Constructor with name and description
    public Role(String name, String description) {
        this.name = name;
        this.description = description;
    }



    // Helper methods
    public void addUser(User user) {
        users.add(user);
        user.getRoles().add(this);
    }

    public void removeUser(User user) {
        users.remove(user);
        user.getRoles().remove(this);
    }


}