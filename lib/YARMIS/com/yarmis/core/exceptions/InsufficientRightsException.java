package com.yarmis.core.exceptions;

import java.util.List;

public class InsufficientRightsException extends RuntimeException {

    public InsufficientRightsException(List<String> insufficient) {
        super(InsufficientRightsException.convertToText(insufficient));
    }

    public InsufficientRightsException(String message) {
        super(message);
    }

    private static String convertToText(List<String> insufficient) {
        StringBuilder sb = new StringBuilder();
        sb.append("Device is missing the following right(s):");
        for (String right : insufficient)
            sb.append(" ").append(right);

        return sb.toString();
    }

}
