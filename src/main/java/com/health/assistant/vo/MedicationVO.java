package com.health.assistant.vo;

import com.health.assistant.entity.MemberMedication;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class MedicationVO {

    private final Long id;
    private final String drugName;
    private final String dose;
    private final String frequency;
    private final String route;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final String purpose;
    private final String status;
    private final String doctorAdvice;
    private final String remark;

    public static MedicationVO from(MemberMedication entity) {
        return MedicationVO.builder()
                .id(entity.getId())
                .drugName(entity.getDrugName())
                .dose(entity.getDose())
                .frequency(entity.getFrequency())
                .route(entity.getRoute())
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .purpose(entity.getPurpose())
                .status(entity.getStatus())
                .doctorAdvice(entity.getDoctorAdvice())
                .remark(entity.getRemark())
                .build();
    }
}
