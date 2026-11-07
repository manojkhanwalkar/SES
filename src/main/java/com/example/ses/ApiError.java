package com.example.ses;

import java.util.List;

/** JSON error body returned for every API failure. */
public record ApiError(String error, String message, List<String> details) {
}
