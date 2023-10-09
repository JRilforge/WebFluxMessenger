package uk.co.hippodigital.engineering.controller;

import io.opentracing.Span;
import io.opentracing.log.Fields;
import io.opentracing.tag.Tags;
import io.opentracing.util.GlobalTracer;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.ChangeStreamEvent;
import org.springframework.data.mongodb.core.ChangeStreamOptions;
import org.springframework.data.mongodb.core.ReactiveMongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uk.co.hippodigital.engineering.domain.UserMessage;
import uk.co.hippodigital.engineering.dto.MessageDto;
import uk.co.hippodigital.engineering.dto.MessageRequest;
import uk.co.hippodigital.engineering.service.MessagingService;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.match;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.newAggregation;

@RestController
public class MessengerApi {

  @Autowired
  private MessagingService messagingService;

  @PostMapping("/send-message")
  public Mono<MessageDto> sendMessage(@RequestBody MessageRequest messageRequest) {
    return messagingService.sendMessage(messageRequest);
  }

  @GetMapping(value = "/consume/{myUserId}/messaging-event-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public Flux<MessageDto> messageStream(ServerWebExchange exchange, @PathVariable String myUserId) {
    exchange.getResponse().getHeaders().add("Access-Control-Allow-Origin", "*");

    return messagingService.messageStream(myUserId);
  }

  @GetMapping("/messages-between")
  public Flux<MessageDto> getMessagesBetween(@RequestParam("a") String aUserId, @RequestParam("b") String bUserId) throws Exception {
    return messagingService.getMessagesBetween(aUserId, bUserId);
  }
}
