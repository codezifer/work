package de.carsten.spring_react.controller;

import de.carsten.spring_react.model.dto.TutorialDto;
import de.carsten.spring_react.model.request.TutorialAddRequest;
import de.carsten.spring_react.service.TutorialService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api")
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
public class TutorialController {

    TutorialService tutorialService;

    @GetMapping(
        value = "/tutorials",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Flux<TutorialDto> getTutorials() {
        return tutorialService.getTutorials();
    }

    @PostMapping(
        value = "/add/tutorial",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Mono<TutorialDto> addTutorial(@RequestBody TutorialAddRequest request) {
        return tutorialService.addTutorial(request);
    }
}
