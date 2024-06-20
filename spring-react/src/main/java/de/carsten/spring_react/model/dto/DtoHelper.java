package de.carsten.spring_react.model.dto;

import lombok.experimental.UtilityClass;

import java.time.Instant;
import java.time.OffsetDateTime;

@UtilityClass
class DtoHelper {

    static Instant utc(OffsetDateTime offsetDateTime) {
        return offsetDateTime.toInstant();
    }
}
