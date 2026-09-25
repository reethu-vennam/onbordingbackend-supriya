package com.sabbpe.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Configuration
public class AppConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    // spring.jackson.date-format only applies to java.util.Date; LocalDateTime is
    // serialized
    // by JavaTimeModule's own ISO formatter regardless of that property. Every
    // LocalDateTime in
    // this codebase is written as UTC wall-clock (see BaseEntity), so append 'Z'
    // explicitly to
    // match Node's toISOString() output instead of emitting an ambiguous
    // offset-less string.
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer utcLocalDateTimeSerializer() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        return builder -> builder.serializerByType(LocalDateTime.class, new JsonSerializer<LocalDateTime>() {
            @Override
            public void serialize(LocalDateTime value, JsonGenerator gen, SerializerProvider serializers)
                    throws IOException {
                gen.writeString(value.format(fmt) + "Z");
            }
        });
    }
}
