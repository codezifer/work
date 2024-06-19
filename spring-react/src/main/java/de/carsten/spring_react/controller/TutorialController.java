package de.carsten.spring_react.controller;

import de.carsten.spring_react.model.Tutorial;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api")
public class TutorialController {

    @GetMapping("/tutorial")
    public Mono<Tutorial> getTutorial() {
        return Mono.empty();
    }
}
