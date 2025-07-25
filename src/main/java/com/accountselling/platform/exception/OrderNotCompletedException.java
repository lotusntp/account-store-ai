package com.accountselling.platform.exception;

/** Exception thrown when attempting to download data from an incomplete order. */
public class OrderNotCompletedException extends BaseException {

  public OrderNotCompletedException(String orderId) {
    super(String.format("Order %s must be completed before downloading account data", orderId));
  }
}
