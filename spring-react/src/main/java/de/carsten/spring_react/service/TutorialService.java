package de.carsten.spring_react.service;

import de.carsten.spring_react.model.dao.TutorialDao;
import de.carsten.spring_react.model.dto.TutorialDto;
import de.carsten.spring_react.model.request.TutorialAddRequest;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Service
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
public class TutorialService {

    TutorialDao tutorialDao;

    public Flux<TutorialDto> getTutorials() {
        return tutorialDao
                .getAllTutorials()
                .map(TutorialDto::from);
    }

    public Mono<TutorialDto> addTutorial(TutorialAddRequest request) {
        Instant now = Instant.now();
        var dto = new TutorialDto(null, request.title(), request.description(), false, now, now);
        return tutorialDao
                .insertTutorial(dto)
                .then(Mono.fromSupplier(() -> dto));
    }

    public Mono<TutorialDto> getTutorial(long id) {
        return tutorialDao
                .findById(id)
                .map(TutorialDto::from);
    }
}
