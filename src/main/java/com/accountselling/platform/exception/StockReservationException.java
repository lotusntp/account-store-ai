package com.accountselling.platform.exception;

/** Exception thrown when stock reservation fails. */
public class StockReservationException extends StockException {

  public StockReservationException(String message) {
    super(message);
  }

  public StockReservationException(String message, Throwable cause) {
    super(message, cause);
  }
}
