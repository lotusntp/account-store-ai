package com.accountselling.platform.exception;

/** Exception thrown when a product is out of stock. */
public class OutOfStockException extends StockException {

  public OutOfStockException(String message) {
    super(message);
  }

  public OutOfStockException(String message, Throwable cause) {
    super(message, cause);
  }

  public static OutOfStockException forProduct(String productName) {
    return new OutOfStockException(String.format("Product '%s' is out of stock", productName));
  }
}
