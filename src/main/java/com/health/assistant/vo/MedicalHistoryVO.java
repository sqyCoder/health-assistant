package com.health.assistant.vo;

import com.health.assistant.entity.MemberMedicalHistory;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class MedicalHistoryVO {

    private final Long id;
    private final String diseaseName;
    private final LocalDate diagnosedDate;
    private final String diseaseStatus;
    private final String treatmentDesc;
    private final String hospital;
    private final String remark;

    public static MedicalHistoryVO from(MemberMedicalHistory entity) {
        return MedicalHistoryVO.builder()
                .id(entity.getId())
                .diseaseName(entity.getDiseaseName())
                .diagnosedDate(entity.getDiagnosedDate())
                .diseaseStatus(entity.getDiseaseStatus())
                .treatmentDesc(entity.getTreatmentDesc())
                .hospital(entity.getHospital())
                .remark(entity.getRemark())
                .build();
    }
}
