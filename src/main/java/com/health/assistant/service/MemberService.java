package com.health.assistant.service;

import com.health.assistant.dto.member.AllergyRequest;
import com.health.assistant.dto.member.FamilyHistoryRequest;
import com.health.assistant.dto.member.LifestyleRequest;
import com.health.assistant.dto.member.MedicalHistoryRequest;
import com.health.assistant.dto.member.MedicationRequest;
import com.health.assistant.dto.member.MemberSaveRequest;
import com.health.assistant.vo.AllergyVO;
import com.health.assistant.vo.FamilyHistoryVO;
import com.health.assistant.vo.LifestyleVO;
import com.health.assistant.vo.MedicalHistoryVO;
import com.health.assistant.vo.MedicationVO;
import com.health.assistant.vo.MemberProfileVO;
import com.health.assistant.vo.MemberVO;

import java.util.List;

public interface MemberService {

    List<MemberVO> listMembers(Long userId);

    MemberVO createMember(Long userId, MemberSaveRequest request);

    MemberVO getMember(Long userId, Long memberId);

    MemberVO updateMember(Long userId, Long memberId, MemberSaveRequest request);

    void deleteMember(Long userId, Long memberId);

    MemberProfileVO getProfile(Long userId, Long memberId);

    List<MedicalHistoryVO> listMedicalHistories(Long userId, Long memberId);

    MedicalHistoryVO addMedicalHistory(Long userId, Long memberId, MedicalHistoryRequest request);

    MedicalHistoryVO updateMedicalHistory(Long userId, Long memberId, Long id, MedicalHistoryRequest request);

    void deleteMedicalHistory(Long userId, Long memberId, Long id);

    List<AllergyVO> listAllergies(Long userId, Long memberId);

    AllergyVO addAllergy(Long userId, Long memberId, AllergyRequest request);

    AllergyVO updateAllergy(Long userId, Long memberId, Long id, AllergyRequest request);

    void deleteAllergy(Long userId, Long memberId, Long id);

    List<MedicationVO> listMedications(Long userId, Long memberId);

    MedicationVO addMedication(Long userId, Long memberId, MedicationRequest request);

    MedicationVO updateMedication(Long userId, Long memberId, Long id, MedicationRequest request);

    void deleteMedication(Long userId, Long memberId, Long id);

    List<FamilyHistoryVO> listFamilyHistories(Long userId, Long memberId);

    FamilyHistoryVO addFamilyHistory(Long userId, Long memberId, FamilyHistoryRequest request);

    FamilyHistoryVO updateFamilyHistory(Long userId, Long memberId, Long id, FamilyHistoryRequest request);

    void deleteFamilyHistory(Long userId, Long memberId, Long id);

    LifestyleVO getLifestyle(Long userId, Long memberId);

    LifestyleVO saveLifestyle(Long userId, Long memberId, LifestyleRequest request);
}
