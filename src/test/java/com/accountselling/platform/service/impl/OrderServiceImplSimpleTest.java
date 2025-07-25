package com.accountselling.platform.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.accountselling.platform.model.*;
import com.accountselling.platform.repository.*;
import com.accountselling.platform.service.StockService;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplSimpleTest {

  @Mock private OrderRepository orderRepository;

  @Mock private UserRepository userRepository;

  @Mock private ProductRepository productRepository;

  @Mock private StockRepository stockRepository;

  @Mock private StockService stockService;

  @InjectMocks private OrderServiceImpl orderService;

  private User testUser;
  private Product testProduct;
  private Order testOrder;

  @BeforeEach
  void setUp() {
    testUser = new User();
    testUser.setId(UUID.randomUUID());
    testUser.setUsername("testuser");

    testProduct = new Product();
    testProduct.setId(UUID.randomUUID());
    testProduct.setName("Test Product");
    testProduct.setPrice(BigDecimal.valueOf(100.00));

    testOrder = new Order(testUser, BigDecimal.valueOf(100.00));
    testOrder.setId(UUID.randomUUID());
  }

  @Test
  void findById_WithValidId_ShouldReturnOrder() {
    // Arrange
    UUID orderId = testOrder.getId();
    when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));

    // Act
    Order result = orderService.findById(orderId);

    // Assert
    assertNotNull(result);
    assertEquals(testOrder, result);
    verify(orderRepository).findById(orderId);
  }

  @Test
  void canUserCreateOrders_WithValidUser_ShouldReturnTrue() {
    // Act
    boolean result = orderService.canUserCreateOrders(testUser);

    // Assert
    assertTrue(result);
  }

  @Test
  void canUserCreateOrders_WithNullUser_ShouldReturnFalse() {
    // Act
    boolean result = orderService.canUserCreateOrders(null);

    // Assert
    assertFalse(result);
  }
}
