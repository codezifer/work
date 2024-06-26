package de.carsten.spring_react.model.dto;

import de.carsten.spring_react.model.jooq.tables.records.TutorialRecord;

import java.time.Instant;

import static de.carsten.spring_react.model.dto.DtoHelper.utc;

public record TutorialDto(Long id, String title, String description, boolean published, Instant created, Instant modified) {

    public static TutorialDto from(TutorialRecord t) {
        return new TutorialDto(
                t.getId(),
                t.getTitle(),
                t.getDescription(),
                t.getPublished(),
                utc(t.getCreated()),
                utc(t.getModified())
        );
    }

    public TutorialRecord to() {
        return new TutorialRecord(
                this.id(),
                this.title(),
                this.description(),
                this.published(),
                utc(this.created()),
                utc(this.modified())
        );
    }
}
