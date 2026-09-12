package com.health.assistant.vo;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class MemberProfileVO {

    private final MemberVO member;
    private final List<MedicalHistoryVO> medicalHistories;
    private final List<AllergyVO> allergies;
    private final List<MedicationVO> medications;
    private final List<FamilyHistoryVO> familyHistories;
    private final LifestyleVO lifestyle;
}
