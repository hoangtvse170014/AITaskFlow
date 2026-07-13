package com.taskflow.dto.request;

import com.taskflow.entity.Goal;
import com.taskflow.entity.KeyResult;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateGoalRequest {
    @NotBlank(message = "Goal title is required")
    @Size(min = 1, max = 255, message = "Title must be between 1 and 255 characters")
    private String title;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    private Goal.GoalType type;
    private Goal.GoalStatus status;
    private Goal.GoalPeriod period;
    private LocalDate startDate;
    private LocalDate dueDate;
    private Integer targetValue;
}
