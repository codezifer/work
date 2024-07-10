package de.carsten.spring_react.model.dao;

import de.carsten.spring_react.model.dto.TutorialDto;
import de.carsten.spring_react.model.jooq.tables.records.TutorialRecord;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static de.carsten.spring_react.model.jooq.tables.Tutorial.TUTORIAL;

@Component
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
public class TutorialDao {

    DSLContext dslCtx;

    public Flux<TutorialRecord> getAllTutorials() {
        return Flux.from(dslCtx.selectFrom(TUTORIAL));
    }

    public Mono<TutorialRecord> findById(long id) {
        return Mono.from(dslCtx.selectFrom(TUTORIAL).where(TUTORIAL.ID.eq(id)));
    }

    public Mono<Integer> insertTutorial(TutorialDto dto) {
        return Mono.fromCompletionStage(dslCtx.insertInto(TUTORIAL)
                .set(dto.to())
                .executeAsync()
        );
    }
}
