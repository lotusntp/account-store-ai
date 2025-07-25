package com.accountselling.platform.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.HashSet;
import java.util.Set;
import lombok.*;

/**
 * Category entity representing product categories with hierarchical structure. Supports
 * parent-child relationships for organizing products into categories and subcategories.
 *
 * <p>เอนทิตี้หมวดหมู่สินค้าที่มีโครงสร้างแบบลำดับชั้น รองรับความสัมพันธ์แบบพ่อแม่-ลูก
 * สำหรับจัดระเบียบสินค้าในหมวดหมู่และหมวดหมู่ย่อย
 */
@Entity
@Table(
    name = "categories",
    indexes = {
      @Index(name = "idx_category_name", columnList = "name"),
      @Index(name = "idx_category_parent", columnList = "parent_category_id")
    })
@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = {"parentCategory", "subCategories", "products"})
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class Category extends BaseEntity {

  @NotBlank(message = "Category name cannot be blank")
  @Size(max = 100, message = "Category name cannot exceed 100 characters")
  @Column(name = "name", nullable = false, length = 100)
  @EqualsAndHashCode.Include
  private String name;

  @Size(max = 500, message = "Category description cannot exceed 500 characters")
  @Column(name = "description", length = 500)
  private String description;

  @Column(name = "active", nullable = false)
  private Boolean active = true;

  @Column(name = "sort_order")
  private Integer sortOrder = 0;

  // Self-referencing relationship for hierarchical structure
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "parent_category_id")
  private Category parentCategory;

  @OneToMany(mappedBy = "parentCategory", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @OrderBy("sortOrder ASC, name ASC")
  private Set<Category> subCategories = new HashSet<>();

  @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  private Set<Product> products = new HashSet<>();

  // Constructor with name
  public Category(String name) {
    this.name = name;
  }

  // Constructor with name and description
  public Category(String name, String description) {
    this.name = name;
    this.description = description;
  }

  // Constructor with name, description, and parent
  public Category(String name, String description, Category parentCategory) {
    this.name = name;
    this.description = description;
    this.parentCategory = parentCategory;
  }

  // Helper methods for managing subcategories
  public void addSubCategory(Category subCategory) {
    subCategories.add(subCategory);
    subCategory.setParentCategory(this);
  }

  public void removeSubCategory(Category subCategory) {
    subCategories.remove(subCategory);
    subCategory.setParentCategory(null);
  }

  // Helper methods for managing products
  public void addProduct(Product product) {
    products.add(product);
    product.setCategory(this);
  }

  public void removeProduct(Product product) {
    products.remove(product);
    product.setCategory(null);
  }

  // Business logic methods
  public boolean isRootCategory() {
    return parentCategory == null;
  }

  public boolean hasSubCategories() {
    return !subCategories.isEmpty();
  }

  public boolean hasProducts() {
    return !products.isEmpty();
  }

  public int getLevel() {
    int level = 0;
    Category current = this.parentCategory;
    while (current != null) {
      level++;
      current = current.getParentCategory();
    }
    return level;
  }

  public String getFullPath() {
    if (parentCategory == null) {
      return name;
    }
    return parentCategory.getFullPath() + " > " + name;
  }

  // Get all descendant categories (recursive)
  public Set<Category> getAllDescendants() {
    Set<Category> descendants = new HashSet<>();
    for (Category subCategory : subCategories) {
      descendants.add(subCategory);
      descendants.addAll(subCategory.getAllDescendants());
    }
    return descendants;
  }

  // Get root category
  public Category getRootCategory() {
    Category current = this;
    while (current.getParentCategory() != null) {
      current = current.getParentCategory();
    }
    return current;
  }

  // Check if this category is ancestor of given category
  public boolean isAncestorOf(Category category) {
    Category current = category.getParentCategory();
    while (current != null) {
      if (current.equals(this)) {
        return true;
      }
      current = current.getParentCategory();
    }
    return false;
  }

  // Check if this category is descendant of given category
  public boolean isDescendantOf(Category category) {
    return category.isAncestorOf(this);
  }
}
