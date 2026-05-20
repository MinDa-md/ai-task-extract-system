package com.mindamd.taskextractor.step.db;

import com.mindamd.taskextractor.domain.entity.Summary;
import com.mindamd.taskextractor.domain.repository.SummaryRepository;
import com.mindamd.taskextractor.pipeline.spec.Step;
import com.mindamd.taskextractor.pipeline.spec.StepResult;
import com.mindamd.taskextractor.step.ai.AiSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DbStep implements Step<AiSummary, SavedSummary> {

    private final SummaryRepository summaryRepository;

    @Override
    public Integer getStepOrder() {
        return 30;
    }

    @Override
    public StepResult<SavedSummary> execute(AiSummary input) {
        if (summaryRepository.existsByRequestKey(input.requestKey())) {
            return StepResult.of(new SavedSummary(input.summary()));
        }
        summaryRepository.save(new Summary(
                input.requestKey(),
                input.summary().meetingTime(),
                input.summary().location(),
                input.summary().participantsInfo()
        ));
        return StepResult.of(new SavedSummary(input.summary()));
    }


}
