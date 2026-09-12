package com.health.assistant.support;

import com.health.assistant.entity.FamilyMember;
import com.health.assistant.entity.ExamIndicator;
import com.health.assistant.entity.MemberAllergy;
import com.health.assistant.entity.MemberFamilyHistory;
import com.health.assistant.entity.MemberLifestyle;
import com.health.assistant.entity.MemberMedicalHistory;
import com.health.assistant.entity.MemberMedication;
import com.health.assistant.common.constant.MemberConstants;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.StringJoiner;

@Component
public class HealthProfileFormatter {

    public String formatProfile(FamilyMember member) {
        StringJoiner joiner = new StringJoiner("；");
        joiner.add("姓名" + member.getName());
        joiner.add("年龄" + ageOf(member) + "岁");
        joiner.add(Integer.valueOf(2).equals(member.getGender()) ? "女" : "男");
        if (member.getHeightCm() != null) {
            joiner.add("身高" + trim(member.getHeightCm()) + "cm");
        }
        if (member.getWeightKg() != null) {
            joiner.add("体重" + trim(member.getWeightKg()) + "kg");
        }
        BigDecimal bmi = bmiOf(member);
        if (bmi != null) {
            joiner.add("BMI" + bmi);
        }
        if (StringUtils.hasText(member.getBloodType())) {
            joiner.add(member.getBloodType() + "型血");
        }
        return joiner.toString();
    }

    public String formatMedicalHistories(List<MemberMedicalHistory> histories) {
        if (histories == null || histories.isEmpty()) {
            return "无记录";
        }
        StringJoiner joiner = new StringJoiner("；");
        for (MemberMedicalHistory history : histories) {
            StringBuilder item = new StringBuilder(history.getDiseaseName());
            item.append("（").append(statusLabel(history.getDiseaseStatus())).append("）");
            if (history.getDiagnosedDate() != null) {
                item.append(" 确诊于").append(history.getDiagnosedDate());
            }
            if (StringUtils.hasText(history.getTreatmentDesc())) {
                item.append("，").append(history.getTreatmentDesc());
            }
            joiner.add(item.toString());
        }
        return joiner.toString();
    }

    public String formatAllergies(List<MemberAllergy> allergies) {
        if (allergies == null || allergies.isEmpty()) {
            return "无记录";
        }
        StringJoiner joiner = new StringJoiner("；");
        for (MemberAllergy allergy : allergies) {
            StringBuilder item = new StringBuilder(allergy.getAllergen());
            if (StringUtils.hasText(allergy.getAllergyType())) {
                item.append("（").append(allergy.getAllergyType()).append("）");
            }
            if (StringUtils.hasText(allergy.getReaction())) {
                item.append(" 反应:").append(allergy.getReaction());
            }
            if (StringUtils.hasText(allergy.getSeverity())) {
                item.append(" 程度:").append(allergy.getSeverity());
            }
            joiner.add(item.toString());
        }
        return joiner.toString();
    }

    public String formatMedications(List<MemberMedication> medications) {
        if (medications == null || medications.isEmpty()) {
            return "无记录";
        }
        StringJoiner joiner = new StringJoiner("；");
        for (MemberMedication medication : medications) {
            StringBuilder item = new StringBuilder(medication.getDrugName());
            if (StringUtils.hasText(medication.getDose())) {
                item.append(" ").append(medication.getDose());
            }
            if (StringUtils.hasText(medication.getFrequency())) {
                item.append(" ").append(medication.getFrequency());
            }
            if (StringUtils.hasText(medication.getPurpose())) {
                item.append("（").append(medication.getPurpose()).append("）");
            }
            if ("STOPPED".equals(medication.getStatus())) {
                item.append("[已停用]");
            }
            joiner.add(item.toString());
        }
        return joiner.toString();
    }

    public String formatFamilyHistories(List<MemberFamilyHistory> histories) {
        if (histories == null || histories.isEmpty()) {
            return "无记录";
        }
        StringJoiner joiner = new StringJoiner("；");
        for (MemberFamilyHistory history : histories) {
            StringBuilder item = new StringBuilder(relationshipLabel(history.getRelation()));
            item.append("-").append(history.getDiseaseName());
            if (history.getOnsetAge() != null) {
                item.append("（").append(history.getOnsetAge()).append("岁发病）");
            }
            joiner.add(item.toString());
        }
        return joiner.toString();
    }

    public String formatLifestyle(MemberLifestyle lifestyle) {
        if (lifestyle == null) {
            return "无记录";
        }
        StringBuilder builder = new StringBuilder();
        if (StringUtils.hasText(lifestyle.getSmoking())) {
            appendIfPresent(builder, "吸烟情况" + smokingLabel(lifestyle.getSmoking())
                    + (StringUtils.hasText(lifestyle.getSmokingAmount()) ? "(" + lifestyle.getSmokingAmount() + ")" : ""), null);
        }
        if (StringUtils.hasText(lifestyle.getDrinking())) {
            appendIfPresent(builder, "饮酒情况" + drinkingLabel(lifestyle.getDrinking()), null);
        }
        if (lifestyle.getSleepHours() != null) {
            appendIfPresent(builder, "睡眠" + trim(lifestyle.getSleepHours()) + "小时", null);
        }
        if (StringUtils.hasText(lifestyle.getSleepQuality())) {
            appendIfPresent(builder, "睡眠质量" + lifestyle.getSleepQuality(), null);
        }
        if (StringUtils.hasText(lifestyle.getExerciseFrequency())) {
            appendIfPresent(builder, "运动频率" + lifestyle.getExerciseFrequency(), null);
        }
        if (lifestyle.getSedentaryHours() != null) {
            appendIfPresent(builder, "日均久坐" + trim(lifestyle.getSedentaryHours()) + "小时", null);
        }
        if (StringUtils.hasText(lifestyle.getDietHabit())) {
            appendIfPresent(builder, "饮食" + lifestyle.getDietHabit(), null);
        }
        if (StringUtils.hasText(lifestyle.getStressLevel())) {
            appendIfPresent(builder, "压力水平" + lifestyle.getStressLevel(), null);
        }
        return builder.length() == 0 ? "无记录" : builder.toString();
    }

    public String formatIndicators(List<ExamIndicator> indicators) {
        if (indicators == null || indicators.isEmpty()) {
            return "无指标";
        }
        StringJoiner joiner = new StringJoiner("\n");
        for (ExamIndicator indicator : indicators) {
            StringBuilder item = new StringBuilder("- ");
            item.append(indicator.getMetricName());
            item.append(" ").append(indicator.getValueText() == null ? "" : indicator.getValueText());
            if (StringUtils.hasText(indicator.getUnit())) {
                item.append(" ").append(indicator.getUnit());
            }
            item.append("（参考范围 ").append(StringUtils.hasText(indicator.getRefRangeText())
                    ? indicator.getRefRangeText() : "未标注").append("）");
            item.append(" 系统判定:").append(indicator.getJudgeStatus());
            if (StringUtils.hasText(indicator.getJudgeNote())) {
                item.append(" ").append(indicator.getJudgeNote());
            }
            joiner.add(item.toString());
        }
        return joiner.toString();
    }

    public Integer ageOf(FamilyMember member) {
        if (member.getBirthDate() == null) {
            return null;
        }
        return Period.between(member.getBirthDate(), LocalDate.now()).getYears();
    }

    private BigDecimal bmiOf(FamilyMember member) {
        if (member.getHeightCm() == null || member.getWeightKg() == null
                || member.getHeightCm().compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        BigDecimal heightM = member.getHeightCm().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        return member.getWeightKg().divide(heightM.multiply(heightM), 1, RoundingMode.HALF_UP);
    }

    private void appendIfPresent(StringBuilder builder, String value, String fallback) {
        String text = StringUtils.hasText(value) ? value : fallback;
        if (!StringUtils.hasText(text)) {
            return;
        }
        if (builder.length() > 0) {
            builder.append("；");
        }
        builder.append(text);
    }

    private String statusLabel(String status) {
        return switch (status == null ? "" : status) {
            case "CURED" -> "已治愈";
            case "TREATING" -> "治疗中";
            case "MANAGING" -> "长期管理";
            default -> status;
        };
    }

    private String relationshipLabel(String relation) {
        return switch (relation == null ? "" : relation) {
            case "FATHER" -> "父亲";
            case "MOTHER" -> "母亲";
            case "SIBLING" -> "兄弟姐妹";
            case "GRANDPARENT" -> "祖父母";
            default -> relation;
        };
    }

    private String smokingLabel(String smoking) {
        return switch (smoking) {
            case "NEVER" -> "从不吸烟";
            case "QUIT" -> "已戒烟";
            case "SMOKING" -> "吸烟";
            default -> smoking;
        };
    }

    private String drinkingLabel(String drinking) {
        return switch (drinking) {
            case "NEVER" -> "从不饮酒";
            case "OCCASIONAL" -> "偶尔饮酒";
            case "FREQUENT" -> "经常饮酒";
            case "QUIT" -> "已戒酒";
            default -> drinking;
        };
    }

    private String trim(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }
}
