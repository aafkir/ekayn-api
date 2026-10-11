package com.ekayn.api.service;

public class EmailNotVerifiedException extends RuntimeException {
  public EmailNotVerifiedException() {
    super("Vérifiez votre adresse email avant de payer.");
  }
}
