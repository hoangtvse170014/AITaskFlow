package com.taskflow.dto.request;

import com.taskflow.entity.KeyResult;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateKeyResultRequest {
    @NotBlank(message = "Key result title is required")
    private String title;

    private KeyResult.MetricType metricType;

    @PositiveOrZero(message = "Start value must be zero or positive")
    private Double startValue;

    @Positive(message = "Target value must be positive")
    private Double targetValue;

    private LocalDate dueDate;
    private UUID assigneeId;
}
