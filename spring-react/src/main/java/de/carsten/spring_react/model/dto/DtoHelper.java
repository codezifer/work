package de.carsten.spring_react.model.dto;

import lombok.experimental.UtilityClass;

import java.time.*;

@UtilityClass
class DtoHelper {

    static Instant utc(OffsetDateTime offsetDateTime) {
        return offsetDateTime.toInstant();
    }

    static OffsetDateTime utc(Instant instant) {
        return OffsetDateTime.ofInstant(instant, ZoneId.of("UTC"));
    }

}
