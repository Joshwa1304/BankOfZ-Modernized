package com.bankofz.dto;

/** Plain MSG-OUT style response, e.g. "LOGOFF SUCCESSFUL". */
public record MessageResponse(String message) {
}
