package com.example.quick;
public record ApiError(String code, String message, Object requestId) {}
