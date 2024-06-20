package de.carsten.spring_react.model.dao;

import de.carsten.spring_react.model.dto.TutorialDto;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.jooq.DSLContext;
import org.jooq.Record1;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import static de.carsten.spring_react.model.jooq.tables.Tutorial.TUTORIAL;

@Component
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
public class TutorialDao {

    DSLContext dslCtx;


    public Flux<Record1<TutorialDto>> getAllTutorials() {
        return Flux.from(dslCtx.select(TUTORIAL.convertFrom(TutorialDto::from)).from(TUTORIAL));
    }
}
