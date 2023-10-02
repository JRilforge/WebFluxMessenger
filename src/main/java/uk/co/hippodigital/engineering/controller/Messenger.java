package uk.co.hippodigital.engineering.controller;

import lombok.AllArgsConstructor;
import org.springframework.data.mongodb.core.ReactiveMongoOperations;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.match;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.newAggregation;

@Controller
public class Messenger {

  /**
   * A simple messenger app to simple chat
   *
   * @param model to store data for page rendering
   * @param mid my userId
   * @param oid other userId
   * @return
   */
  @GetMapping("/messenger")
  public String openMessenger(Model model, @RequestParam String mid, @RequestParam String oid) {
    model.addAttribute("myUserId", mid);
    model.addAttribute("otherUserId", oid);
    return "index";
  }
}
