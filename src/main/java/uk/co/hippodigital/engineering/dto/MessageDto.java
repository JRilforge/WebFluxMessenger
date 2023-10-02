package uk.co.hippodigital.engineering.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.index.Indexed;

import java.util.Date;

@AllArgsConstructor
@Getter
public class MessageDto {

  private String id;

  private String fromUserId;

  private String toUserId;

  private String content;

  private long created;
}
