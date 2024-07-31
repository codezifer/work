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
import jakarta.validation.constraints.NotNull;

@RestController
@RequestMapping("/api")
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
public class TutorialController {
    public static final String TUTORIALS = "/tutorials";

    TutorialService tutorialService;

    @GetMapping(
            value = TUTORIALS,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Flux<TutorialDto> getTutorials() {
        return tutorialService.getTutorials();
    }

    @GetMapping(
            value = TUTORIALS + "/{id}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Mono<TutorialDto> getTutorial(@NotNull @PathVariable String id) {
        return tutorialService.getTutorial(Long.parseLong(id));
    }

    @GetMapping(
            value =  TUTORIALS + "/title/{title}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Mono<TutorialDto> getTutorialByTitle(@NotNull @PathVariable String title) {
        return tutorialService.getTutorialByTitle(title);
    }

    @PostMapping(
            value = TUTORIALS + "/add/tutorial",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Mono<TutorialDto> addTutorial(@RequestBody TutorialAddRequest request) {
        return tutorialService.addTutorial(request);
    }
}
