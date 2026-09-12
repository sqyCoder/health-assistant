package com.health.assistant.vo;

import com.health.assistant.entity.MemberAllergy;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AllergyVO {

    private final Long id;
    private final String allergen;
    private final String allergyType;
    private final String reaction;
    private final String severity;
    private final String remark;

    public static AllergyVO from(MemberAllergy entity) {
        return AllergyVO.builder()
                .id(entity.getId())
                .allergen(entity.getAllergen())
                .allergyType(entity.getAllergyType())
                .reaction(entity.getReaction())
                .severity(entity.getSeverity())
                .remark(entity.getRemark())
                .build();
    }
}
