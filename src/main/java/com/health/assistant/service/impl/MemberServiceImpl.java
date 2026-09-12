package com.health.assistant.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.health.assistant.common.constant.MemberConstants;
import com.health.assistant.common.exception.BizException;
import com.health.assistant.common.exception.ErrorCode;
import com.health.assistant.dto.member.AllergyRequest;
import com.health.assistant.dto.member.FamilyHistoryRequest;
import com.health.assistant.dto.member.LifestyleRequest;
import com.health.assistant.dto.member.MedicalHistoryRequest;
import com.health.assistant.dto.member.MedicationRequest;
import com.health.assistant.dto.member.MemberSaveRequest;
import com.health.assistant.entity.FamilyMember;
import com.health.assistant.entity.MemberAllergy;
import com.health.assistant.entity.MemberFamilyHistory;
import com.health.assistant.entity.MemberLifestyle;
import com.health.assistant.entity.MemberMedicalHistory;
import com.health.assistant.entity.MemberMedication;
import com.health.assistant.mapper.FamilyMemberMapper;
import com.health.assistant.mapper.MemberAllergyMapper;
import com.health.assistant.mapper.MemberFamilyHistoryMapper;
import com.health.assistant.mapper.MemberLifestyleMapper;
import com.health.assistant.mapper.MemberMedicalHistoryMapper;
import com.health.assistant.mapper.MemberMedicationMapper;
import com.health.assistant.service.MemberService;
import com.health.assistant.support.MemberAccessChecker;
import com.health.assistant.vo.AllergyVO;
import com.health.assistant.vo.FamilyHistoryVO;
import com.health.assistant.vo.LifestyleVO;
import com.health.assistant.vo.MedicalHistoryVO;
import com.health.assistant.vo.MedicationVO;
import com.health.assistant.vo.MemberProfileVO;
import com.health.assistant.vo.MemberVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService {

    private final FamilyMemberMapper familyMemberMapper;
    private final MemberMedicalHistoryMapper medicalHistoryMapper;
    private final MemberAllergyMapper allergyMapper;
    private final MemberMedicationMapper medicationMapper;
    private final MemberFamilyHistoryMapper familyHistoryMapper;
    private final MemberLifestyleMapper lifestyleMapper;
    private final MemberAccessChecker memberAccessChecker;

    @Override
    public List<MemberVO> listMembers(Long userId) {
        return familyMemberMapper.selectList(Wrappers.<FamilyMember>lambdaQuery()
                        .eq(FamilyMember::getUserId, userId)
                        .orderByAsc(FamilyMember::getSort)
                        .orderByAsc(FamilyMember::getCreatedAt))
                .stream()
                .map(MemberVO::from)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MemberVO createMember(Long userId, MemberSaveRequest request) {
        requireIn(MemberConstants.RELATIONS, request.getRelation(), "与账号主人的关系");
        FamilyMember member = new FamilyMember();
        member.setUserId(userId);
        applyMember(member, request);
        member.setSort(request.getSort() == null ? 0 : request.getSort());
        familyMemberMapper.insert(member);
        return MemberVO.from(member);
    }

    @Override
    public MemberVO getMember(Long userId, Long memberId) {
        return MemberVO.from(memberAccessChecker.check(userId, memberId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MemberVO updateMember(Long userId, Long memberId, MemberSaveRequest request) {
        FamilyMember member = memberAccessChecker.check(userId, memberId);
        requireIn(MemberConstants.RELATIONS, request.getRelation(), "与账号主人的关系");
        applyMember(member, request);
        if (request.getSort() != null) {
            member.setSort(request.getSort());
        }
        familyMemberMapper.updateById(member);
        return MemberVO.from(member);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteMember(Long userId, Long memberId) {
        FamilyMember member = memberAccessChecker.check(userId, memberId);
        medicalHistoryMapper.delete(Wrappers.<MemberMedicalHistory>lambdaQuery()
                .eq(MemberMedicalHistory::getMemberId, member.getId()));
        allergyMapper.delete(Wrappers.<MemberAllergy>lambdaQuery()
                .eq(MemberAllergy::getMemberId, member.getId()));
        medicationMapper.delete(Wrappers.<MemberMedication>lambdaQuery()
                .eq(MemberMedication::getMemberId, member.getId()));
        familyHistoryMapper.delete(Wrappers.<MemberFamilyHistory>lambdaQuery()
                .eq(MemberFamilyHistory::getMemberId, member.getId()));
        lifestyleMapper.delete(Wrappers.<MemberLifestyle>lambdaQuery()
                .eq(MemberLifestyle::getMemberId, member.getId()));
        familyMemberMapper.deleteById(member.getId());
    }

    @Override
    public MemberProfileVO getProfile(Long userId, Long memberId) {
        FamilyMember member = memberAccessChecker.check(userId, memberId);
        return MemberProfileVO.builder()
                .member(MemberVO.from(member))
                .medicalHistories(listMedicalHistories(userId, memberId))
                .allergies(listAllergies(userId, memberId))
                .medications(listMedications(userId, memberId))
                .familyHistories(listFamilyHistories(userId, memberId))
                .lifestyle(getLifestyle(userId, memberId))
                .build();
    }

    @Override
    public List<MedicalHistoryVO> listMedicalHistories(Long userId, Long memberId) {
        FamilyMember member = memberAccessChecker.check(userId, memberId);
        return medicalHistoryMapper.selectList(Wrappers.<MemberMedicalHistory>lambdaQuery()
                        .eq(MemberMedicalHistory::getMemberId, member.getId())
                        .orderByDesc(MemberMedicalHistory::getDiagnosedDate)
                        .orderByDesc(MemberMedicalHistory::getCreatedAt))
                .stream()
                .map(MedicalHistoryVO::from)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MedicalHistoryVO addMedicalHistory(Long userId, Long memberId, MedicalHistoryRequest request) {
        FamilyMember member = memberAccessChecker.check(userId, memberId);
        requireIn(MemberConstants.DISEASE_STATUSES, request.getDiseaseStatus(), "疾病状态");
        MemberMedicalHistory entity = new MemberMedicalHistory();
        entity.setMemberId(member.getId());
        applyMedicalHistory(entity, request);
        medicalHistoryMapper.insert(entity);
        return MedicalHistoryVO.from(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MedicalHistoryVO updateMedicalHistory(Long userId, Long memberId, Long id, MedicalHistoryRequest request) {
        FamilyMember member = memberAccessChecker.check(userId, memberId);
        requireIn(MemberConstants.DISEASE_STATUSES, request.getDiseaseStatus(), "疾病状态");
        MemberMedicalHistory entity = medicalHistoryMapper.selectById(id);
        if (entity == null || !entity.getMemberId().equals(member.getId())) {
            throw new BizException(ErrorCode.NOT_FOUND, "病史记录不存在");
        }
        applyMedicalHistory(entity, request);
        medicalHistoryMapper.updateById(entity);
        return MedicalHistoryVO.from(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteMedicalHistory(Long userId, Long memberId, Long id) {
        FamilyMember member = memberAccessChecker.check(userId, memberId);
        MemberMedicalHistory entity = medicalHistoryMapper.selectById(id);
        if (entity == null || !entity.getMemberId().equals(member.getId())) {
            throw new BizException(ErrorCode.NOT_FOUND, "病史记录不存在");
        }
        medicalHistoryMapper.deleteById(id);
    }

    @Override
    public List<AllergyVO> listAllergies(Long userId, Long memberId) {
        FamilyMember member = memberAccessChecker.check(userId, memberId);
        return allergyMapper.selectList(Wrappers.<MemberAllergy>lambdaQuery()
                        .eq(MemberAllergy::getMemberId, member.getId())
                        .orderByAsc(MemberAllergy::getCreatedAt))
                .stream()
                .map(AllergyVO::from)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AllergyVO addAllergy(Long userId, Long memberId, AllergyRequest request) {
        FamilyMember member = memberAccessChecker.check(userId, memberId);
        validateAllergy(request);
        MemberAllergy entity = new MemberAllergy();
        entity.setMemberId(member.getId());
        applyAllergy(entity, request);
        allergyMapper.insert(entity);
        return AllergyVO.from(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AllergyVO updateAllergy(Long userId, Long memberId, Long id, AllergyRequest request) {
        FamilyMember member = memberAccessChecker.check(userId, memberId);
        validateAllergy(request);
        MemberAllergy entity = allergyMapper.selectById(id);
        if (entity == null || !entity.getMemberId().equals(member.getId())) {
            throw new BizException(ErrorCode.NOT_FOUND, "过敏记录不存在");
        }
        applyAllergy(entity, request);
        allergyMapper.updateById(entity);
        return AllergyVO.from(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAllergy(Long userId, Long memberId, Long id) {
        FamilyMember member = memberAccessChecker.check(userId, memberId);
        MemberAllergy entity = allergyMapper.selectById(id);
        if (entity == null || !entity.getMemberId().equals(member.getId())) {
            throw new BizException(ErrorCode.NOT_FOUND, "过敏记录不存在");
        }
        allergyMapper.deleteById(id);
    }

    @Override
    public List<MedicationVO> listMedications(Long userId, Long memberId) {
        FamilyMember member = memberAccessChecker.check(userId, memberId);
        return medicationMapper.selectList(Wrappers.<MemberMedication>lambdaQuery()
                        .eq(MemberMedication::getMemberId, member.getId())
                        .orderByAsc(MemberMedication::getStatus)
                        .orderByDesc(MemberMedication::getStartDate))
                .stream()
                .map(MedicationVO::from)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MedicationVO addMedication(Long userId, Long memberId, MedicationRequest request) {
        FamilyMember member = memberAccessChecker.check(userId, memberId);
        validateMedication(request);
        MemberMedication entity = new MemberMedication();
        entity.setMemberId(member.getId());
        applyMedication(entity, request);
        medicationMapper.insert(entity);
        return MedicationVO.from(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MedicationVO updateMedication(Long userId, Long memberId, Long id, MedicationRequest request) {
        FamilyMember member = memberAccessChecker.check(userId, memberId);
        validateMedication(request);
        MemberMedication entity = medicationMapper.selectById(id);
        if (entity == null || !entity.getMemberId().equals(member.getId())) {
            throw new BizException(ErrorCode.NOT_FOUND, "用药记录不存在");
        }
        applyMedication(entity, request);
        medicationMapper.updateById(entity);
        return MedicationVO.from(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteMedication(Long userId, Long memberId, Long id) {
        FamilyMember member = memberAccessChecker.check(userId, memberId);
        MemberMedication entity = medicationMapper.selectById(id);
        if (entity == null || !entity.getMemberId().equals(member.getId())) {
            throw new BizException(ErrorCode.NOT_FOUND, "用药记录不存在");
        }
        medicationMapper.deleteById(id);
    }

    @Override
    public List<FamilyHistoryVO> listFamilyHistories(Long userId, Long memberId) {
        FamilyMember member = memberAccessChecker.check(userId, memberId);
        return familyHistoryMapper.selectList(Wrappers.<MemberFamilyHistory>lambdaQuery()
                        .eq(MemberFamilyHistory::getMemberId, member.getId())
                        .orderByAsc(MemberFamilyHistory::getCreatedAt))
                .stream()
                .map(FamilyHistoryVO::from)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FamilyHistoryVO addFamilyHistory(Long userId, Long memberId, FamilyHistoryRequest request) {
        FamilyMember member = memberAccessChecker.check(userId, memberId);
        requireIn(MemberConstants.FAMILY_RELATIONS, request.getRelation(), "亲属关系");
        MemberFamilyHistory entity = new MemberFamilyHistory();
        entity.setMemberId(member.getId());
        applyFamilyHistory(entity, request);
        familyHistoryMapper.insert(entity);
        return FamilyHistoryVO.from(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FamilyHistoryVO updateFamilyHistory(Long userId, Long memberId, Long id, FamilyHistoryRequest request) {
        FamilyMember member = memberAccessChecker.check(userId, memberId);
        requireIn(MemberConstants.FAMILY_RELATIONS, request.getRelation(), "亲属关系");
        MemberFamilyHistory entity = familyHistoryMapper.selectById(id);
        if (entity == null || !entity.getMemberId().equals(member.getId())) {
            throw new BizException(ErrorCode.NOT_FOUND, "家族史记录不存在");
        }
        applyFamilyHistory(entity, request);
        familyHistoryMapper.updateById(entity);
        return FamilyHistoryVO.from(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFamilyHistory(Long userId, Long memberId, Long id) {
        FamilyMember member = memberAccessChecker.check(userId, memberId);
        MemberFamilyHistory entity = familyHistoryMapper.selectById(id);
        if (entity == null || !entity.getMemberId().equals(member.getId())) {
            throw new BizException(ErrorCode.NOT_FOUND, "家族史记录不存在");
        }
        familyHistoryMapper.deleteById(id);
    }

    @Override
    public LifestyleVO getLifestyle(Long userId, Long memberId) {
        FamilyMember member = memberAccessChecker.check(userId, memberId);
        MemberLifestyle entity = lifestyleMapper.selectOne(Wrappers.<MemberLifestyle>lambdaQuery()
                .eq(MemberLifestyle::getMemberId, member.getId()));
        return LifestyleVO.from(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LifestyleVO saveLifestyle(Long userId, Long memberId, LifestyleRequest request) {
        FamilyMember member = memberAccessChecker.check(userId, memberId);
        validateLifestyle(request);
        MemberLifestyle entity = lifestyleMapper.selectOne(Wrappers.<MemberLifestyle>lambdaQuery()
                .eq(MemberLifestyle::getMemberId, member.getId()));
        if (entity == null) {
            entity = new MemberLifestyle();
            entity.setMemberId(member.getId());
            applyLifestyle(entity, request);
            lifestyleMapper.insert(entity);
        } else {
            applyLifestyle(entity, request);
            lifestyleMapper.updateById(entity);
        }
        return LifestyleVO.from(entity);
    }

    private void applyMember(FamilyMember member, MemberSaveRequest request) {
        member.setName(request.getName());
        member.setRelation(request.getRelation());
        member.setGender(request.getGender());
        member.setBirthDate(request.getBirthDate());
        member.setHeightCm(request.getHeightCm());
        member.setWeightKg(request.getWeightKg());
        member.setBloodType(request.getBloodType());
        member.setPhone(request.getPhone());
        member.setAvatarUrl(request.getAvatarUrl());
        member.setRemark(request.getRemark());
    }

    private void applyMedicalHistory(MemberMedicalHistory entity, MedicalHistoryRequest request) {
        entity.setDiseaseName(request.getDiseaseName());
        entity.setDiagnosedDate(request.getDiagnosedDate());
        entity.setDiseaseStatus(request.getDiseaseStatus());
        entity.setTreatmentDesc(request.getTreatmentDesc());
        entity.setHospital(request.getHospital());
        entity.setRemark(request.getRemark());
    }

    private void applyAllergy(MemberAllergy entity, AllergyRequest request) {
        entity.setAllergen(request.getAllergen());
        entity.setAllergyType(request.getAllergyType());
        entity.setReaction(request.getReaction());
        entity.setSeverity(request.getSeverity());
        entity.setRemark(request.getRemark());
    }

    private void applyMedication(MemberMedication entity, MedicationRequest request) {
        entity.setDrugName(request.getDrugName());
        entity.setDose(request.getDose());
        entity.setFrequency(request.getFrequency());
        entity.setRoute(request.getRoute());
        entity.setStartDate(request.getStartDate());
        entity.setEndDate(request.getEndDate());
        entity.setPurpose(request.getPurpose());
        entity.setStatus(StringUtils.hasText(request.getStatus()) ? request.getStatus() : "TAKING");
        entity.setDoctorAdvice(request.getDoctorAdvice());
        entity.setRemark(request.getRemark());
    }

    private void applyFamilyHistory(MemberFamilyHistory entity, FamilyHistoryRequest request) {
        entity.setRelation(request.getRelation());
        entity.setDiseaseName(request.getDiseaseName());
        entity.setOnsetAge(request.getOnsetAge());
        entity.setRemark(request.getRemark());
    }

    private void applyLifestyle(MemberLifestyle entity, LifestyleRequest request) {
        entity.setSmoking(request.getSmoking());
        entity.setSmokingAmount(request.getSmokingAmount());
        entity.setDrinking(request.getDrinking());
        entity.setDrinkingAmount(request.getDrinkingAmount());
        entity.setSleepHours(request.getSleepHours());
        entity.setSleepQuality(request.getSleepQuality());
        entity.setExerciseFrequency(request.getExerciseFrequency());
        entity.setSedentaryHours(request.getSedentaryHours());
        entity.setDietHabit(request.getDietHabit());
        entity.setStressLevel(request.getStressLevel());
    }

    private void validateAllergy(AllergyRequest request) {
        requireIn(MemberConstants.ALLERGY_TYPES, request.getAllergyType(), "过敏类型");
        validateOptional(MemberConstants.ALLERGY_SEVERITIES, request.getSeverity(), "严重程度");
    }

    private void validateMedication(MedicationRequest request) {
        validateOptional(MemberConstants.MEDICATION_ROUTES, request.getRoute(), "用药途径");
        validateOptional(MemberConstants.MEDICATION_STATUSES, request.getStatus(), "用药状态");
    }

    private void validateLifestyle(LifestyleRequest request) {
        validateOptional(MemberConstants.SMOKING_OPTIONS, request.getSmoking(), "吸烟情况");
        validateOptional(MemberConstants.DRINKING_OPTIONS, request.getDrinking(), "饮酒情况");
        validateOptional(MemberConstants.SLEEP_QUALITIES, request.getSleepQuality(), "睡眠质量");
        validateOptional(MemberConstants.STRESS_LEVELS, request.getStressLevel(), "压力水平");
    }

    private void requireIn(Set<String> allowed, String value, String fieldName) {
        if (!allowed.contains(value)) {
            throw new BizException(ErrorCode.PARAM_ERROR, fieldName + "取值非法: " + value);
        }
    }

    private void validateOptional(Set<String> allowed, String value, String fieldName) {
        if (StringUtils.hasText(value) && !allowed.contains(value)) {
            throw new BizException(ErrorCode.PARAM_ERROR, fieldName + "取值非法: " + value);
        }
    }
}
