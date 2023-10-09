package uk.co.hippodigital.engineering.service;

import io.opentracing.Span;
import io.opentracing.log.Fields;
import io.opentracing.tag.Tags;
import io.opentracing.util.GlobalTracer;
import lombok.AllArgsConstructor;
import org.springframework.data.mongodb.core.ChangeStreamEvent;
import org.springframework.data.mongodb.core.ChangeStreamOptions;
import org.springframework.data.mongodb.core.ReactiveMongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uk.co.hippodigital.engineering.domain.UserMessage;
import uk.co.hippodigital.engineering.dto.MessageDto;
import uk.co.hippodigital.engineering.dto.MessageRequest;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.match;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.newAggregation;

@AllArgsConstructor
@Service
public class MessagingService {

  private final ReactiveMongoOperations mongoOperations;

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

  public Flux<MessageDto> messageStream(String myUserId) {
    return mongoOperations.changeStream(ChangeStreamOptions.builder()
                    .filter(newAggregation(match(Criteria.where("toUserId").is(myUserId))))
                    .build(), UserMessage.class)
            .map(this::toMessageDto);
  }

  public Flux<MessageDto> getMessagesBetween(String aUserId, String bUserId) throws Exception {
    if (Objects.equals(aUserId, bUserId)) {
      // Manual instrumentation necessary for error tracking
      // https://docs.datadoghq.com/tracing/trace_collection/custom_instrumentation/java/#set-errors-on-a-span

      var ex = new RuntimeException("'a' can't equal 'b'");

      final Span span = GlobalTracer.get().activeSpan();
      if (span != null) {
        span.setTag(Tags.ERROR, true);
        span.log(Collections.singletonMap(Fields.ERROR_OBJECT, ex));
      }

      return Flux.error(ex);
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
