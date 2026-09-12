package com.health.assistant.vo;

import com.health.assistant.entity.FamilyMember;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;

@Getter
@Builder
public class MemberVO {

    private final Long id;
    private final String name;
    private final String relation;
    private final Integer gender;
    private final LocalDate birthDate;
    private final Integer age;
    private final BigDecimal heightCm;
    private final BigDecimal weightKg;
    private final BigDecimal bmi;
    private final String bloodType;
    private final String phone;
    private final String avatarUrl;
    private final String remark;
    private final Integer sort;
    private final LocalDateTime createdAt;

    public static MemberVO from(FamilyMember member) {
        return MemberVO.builder()
                .id(member.getId())
                .name(member.getName())
                .relation(member.getRelation())
                .gender(member.getGender())
                .birthDate(member.getBirthDate())
                .age(calculateAge(member.getBirthDate()))
                .heightCm(member.getHeightCm())
                .weightKg(member.getWeightKg())
                .bmi(calculateBmi(member.getHeightCm(), member.getWeightKg()))
                .bloodType(member.getBloodType())
                .phone(member.getPhone())
                .avatarUrl(member.getAvatarUrl())
                .remark(member.getRemark())
                .sort(member.getSort())
                .createdAt(member.getCreatedAt())
                .build();
    }

    private static Integer calculateAge(LocalDate birthDate) {
        if (birthDate == null || birthDate.isAfter(LocalDate.now())) {
            return null;
        }
        return Period.between(birthDate, LocalDate.now()).getYears();
    }

    private static BigDecimal calculateBmi(BigDecimal heightCm, BigDecimal weightKg) {
        if (heightCm == null || weightKg == null || heightCm.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        BigDecimal heightM = heightCm.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        return weightKg.divide(heightM.multiply(heightM), 1, RoundingMode.HALF_UP);
    }
}
