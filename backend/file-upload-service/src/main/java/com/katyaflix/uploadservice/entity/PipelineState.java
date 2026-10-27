package com.katyaflix.uploadservice.entity;

import com.katyaflix.uploadservice.dto.enums.PipelineStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class PipelineState {

    @Field("status")
    private PipelineStatus status = PipelineStatus.PENDING;

    @Field("message")
    private String message;

    @Field("errors")
    private List<String> errors = new ArrayList<>();

    @Field("updated_at")
    private Instant updatedAt;

    public static PipelineState of(PipelineStatus status, String message) {
        PipelineState s = new PipelineState();
        s.status = status;
        s.message = message;
        s.updatedAt = Instant.now();
        return s;
    }
}