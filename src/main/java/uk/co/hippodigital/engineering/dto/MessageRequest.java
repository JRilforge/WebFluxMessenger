package uk.co.hippodigital.engineering.dto;

import java.util.Date;

public record MessageRequest(String fromUserId, String toUserId, String content) {
}
