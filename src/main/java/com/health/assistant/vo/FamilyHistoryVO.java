package com.health.assistant.vo;

import com.health.assistant.entity.MemberFamilyHistory;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FamilyHistoryVO {

    private final Long id;
    private final String relation;
    private final String diseaseName;
    private final Integer onsetAge;
    private final String remark;

    public static FamilyHistoryVO from(MemberFamilyHistory entity) {
        return FamilyHistoryVO.builder()
                .id(entity.getId())
                .relation(entity.getRelation())
                .diseaseName(entity.getDiseaseName())
                .onsetAge(entity.getOnsetAge())
                .remark(entity.getRemark())
                .build();
    }
}
