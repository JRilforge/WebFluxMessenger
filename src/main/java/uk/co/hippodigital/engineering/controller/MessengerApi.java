package uk.co.hippodigital.engineering.controller;

import lombok.AllArgsConstructor;
import org.springframework.data.mongodb.core.ChangeStreamEvent;
import org.springframework.data.mongodb.core.ChangeStreamOptions;
import org.springframework.data.mongodb.core.ReactiveMongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uk.co.hippodigital.engineering.domain.UserMessage;
import uk.co.hippodigital.engineering.dto.MessageDto;
import uk.co.hippodigital.engineering.dto.MessageRequest;

import java.util.Date;
import java.util.List;
import java.util.Objects;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.match;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.newAggregation;

@AllArgsConstructor
@RestController
public class MessengerApi {

  private final ReactiveMongoOperations mongoOperations;

  @PostMapping("/send-message")
  public Mono<MessageDto> sendMessage(@RequestBody MessageRequest messageRequest) {
    if (Objects.equals(messageRequest.fromUserId(), messageRequest.toUserId())) {
      return Mono.error(new RuntimeException("fromUserId can't equal toUserId"));
    }

    var userMessage = new UserMessage();
    userMessage.setFromUserId(messageRequest.fromUserId());
    userMessage.setToUserId(messageRequest.toUserId());
    userMessage.setContent(messageRequest.content());
    userMessage.setCreated(new Date());

    return mongoOperations.insert(userMessage).map(this::toMessageDto);
  }

  @GetMapping(value = "/consume/{myUserId}/messaging-event-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public Flux<MessageDto> messageStream(ServerWebExchange exchange, @PathVariable String myUserId) {
    exchange.getResponse().getHeaders().add("Access-Control-Allow-Origin", "*");

    return mongoOperations.changeStream(ChangeStreamOptions.builder()
            .filter(newAggregation(match(Criteria.where("toUserId").is(myUserId))))
            .build(), UserMessage.class)
            .map(this::toMessageDto);
  }

  @GetMapping("/messages-between")
  public Flux<MessageDto> getMessagesBetween(@RequestParam("a") String aUserId, @RequestParam("b") String bUserId) {
    if (Objects.equals(aUserId, bUserId)) {
      return Flux.error(new RuntimeException("'a' can't equal 'b'"));
    }

    var users = List.of(aUserId, bUserId);

    return mongoOperations.find(new Query(Criteria.where("toUserId").in(users)
                    .and("fromUserId").in(users)), UserMessage.class)
            .map(this::toMessageDto);
  }

  private MessageDto toMessageDto(ChangeStreamEvent<UserMessage> event) {
    return toMessageDto(Objects.requireNonNull(event.getBody()));
  }

  private MessageDto toMessageDto(UserMessage userMsg) {
    return new MessageDto(userMsg.getId().toHexString(), userMsg.getFromUserId(),
            userMsg.getToUserId(), userMsg.getContent(), userMsg.getCreated().getTime());
  }
}
