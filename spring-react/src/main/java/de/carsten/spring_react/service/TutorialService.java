package de.carsten.spring_react.service;

import de.carsten.spring_react.model.dao.TutorialDao;
import de.carsten.spring_react.model.dto.TutorialDto;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

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

}
