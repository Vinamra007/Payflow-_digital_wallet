package com.vinamra.payflow.service;

import com.vinamra.payflow.model.User;

public record AuthResult(String token, User user) {
}
