package com.health.assistant.controller;

import com.health.assistant.common.api.Result;
import com.health.assistant.dto.member.AllergyRequest;
import com.health.assistant.dto.member.FamilyHistoryRequest;
import com.health.assistant.dto.member.LifestyleRequest;
import com.health.assistant.dto.member.MedicalHistoryRequest;
import com.health.assistant.dto.member.MedicationRequest;
import com.health.assistant.dto.member.MemberSaveRequest;
import com.health.assistant.security.SecurityUtils;
import com.health.assistant.service.MemberService;
import com.health.assistant.vo.AllergyVO;
import com.health.assistant.vo.FamilyHistoryVO;
import com.health.assistant.vo.LifestyleVO;
import com.health.assistant.vo.MedicalHistoryVO;
import com.health.assistant.vo.MedicationVO;
import com.health.assistant.vo.MemberProfileVO;
import com.health.assistant.vo.MemberVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "成员档案")
@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @Operation(summary = "成员列表")
    @GetMapping
    public Result<List<MemberVO>> list() {
        return Result.success(memberService.listMembers(SecurityUtils.getCurrentUserId()));
    }

    @Operation(summary = "新增成员")
    @PostMapping
    public Result<MemberVO> create(@Valid @RequestBody MemberSaveRequest request) {
        return Result.success(memberService.createMember(SecurityUtils.getCurrentUserId(), request));
    }

    @Operation(summary = "成员详情")
    @GetMapping("/{id}")
    public Result<MemberVO> get(@PathVariable Long id) {
        return Result.success(memberService.getMember(SecurityUtils.getCurrentUserId(), id));
    }

    @Operation(summary = "修改成员")
    @PutMapping("/{id}")
    public Result<MemberVO> update(@PathVariable Long id, @Valid @RequestBody MemberSaveRequest request) {
        return Result.success(memberService.updateMember(SecurityUtils.getCurrentUserId(), id, request));
    }

    @Operation(summary = "删除成员（含子档案）")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        memberService.deleteMember(SecurityUtils.getCurrentUserId(), id);
        return Result.success();
    }

    @Operation(summary = "聚合档案（主档 + 病史/过敏/用药/家族史/生活习惯）")
    @GetMapping("/{id}/profile")
    public Result<MemberProfileVO> profile(@PathVariable Long id) {
        return Result.success(memberService.getProfile(SecurityUtils.getCurrentUserId(), id));
    }

    @Operation(summary = "既往病史列表")
    @GetMapping("/{id}/medical-histories")
    public Result<List<MedicalHistoryVO>> listMedicalHistories(@PathVariable Long id) {
        return Result.success(memberService.listMedicalHistories(SecurityUtils.getCurrentUserId(), id));
    }

    @Operation(summary = "新增既往病史")
    @PostMapping("/{id}/medical-histories")
    public Result<MedicalHistoryVO> addMedicalHistory(@PathVariable Long id,
                                                      @Valid @RequestBody MedicalHistoryRequest request) {
        return Result.success(memberService.addMedicalHistory(SecurityUtils.getCurrentUserId(), id, request));
    }

    @Operation(summary = "修改既往病史")
    @PutMapping("/{id}/medical-histories/{recordId}")
    public Result<MedicalHistoryVO> updateMedicalHistory(@PathVariable Long id, @PathVariable Long recordId,
                                                         @Valid @RequestBody MedicalHistoryRequest request) {
        return Result.success(memberService.updateMedicalHistory(
                SecurityUtils.getCurrentUserId(), id, recordId, request));
    }

    @Operation(summary = "删除既往病史")
    @DeleteMapping("/{id}/medical-histories/{recordId}")
    public Result<Void> deleteMedicalHistory(@PathVariable Long id, @PathVariable Long recordId) {
        memberService.deleteMedicalHistory(SecurityUtils.getCurrentUserId(), id, recordId);
        return Result.success();
    }

    @Operation(summary = "过敏史列表")
    @GetMapping("/{id}/allergies")
    public Result<List<AllergyVO>> listAllergies(@PathVariable Long id) {
        return Result.success(memberService.listAllergies(SecurityUtils.getCurrentUserId(), id));
    }

    @Operation(summary = "新增过敏史")
    @PostMapping("/{id}/allergies")
    public Result<AllergyVO> addAllergy(@PathVariable Long id, @Valid @RequestBody AllergyRequest request) {
        return Result.success(memberService.addAllergy(SecurityUtils.getCurrentUserId(), id, request));
    }

    @Operation(summary = "修改过敏史")
    @PutMapping("/{id}/allergies/{recordId}")
    public Result<AllergyVO> updateAllergy(@PathVariable Long id, @PathVariable Long recordId,
                                           @Valid @RequestBody AllergyRequest request) {
        return Result.success(memberService.updateAllergy(SecurityUtils.getCurrentUserId(), id, recordId, request));
    }

    @Operation(summary = "删除过敏史")
    @DeleteMapping("/{id}/allergies/{recordId}")
    public Result<Void> deleteAllergy(@PathVariable Long id, @PathVariable Long recordId) {
        memberService.deleteAllergy(SecurityUtils.getCurrentUserId(), id, recordId);
        return Result.success();
    }

    @Operation(summary = "长期用药列表")
    @GetMapping("/{id}/medications")
    public Result<List<MedicationVO>> listMedications(@PathVariable Long id) {
        return Result.success(memberService.listMedications(SecurityUtils.getCurrentUserId(), id));
    }

    @Operation(summary = "新增长期用药")
    @PostMapping("/{id}/medications")
    public Result<MedicationVO> addMedication(@PathVariable Long id, @Valid @RequestBody MedicationRequest request) {
        return Result.success(memberService.addMedication(SecurityUtils.getCurrentUserId(), id, request));
    }

    @Operation(summary = "修改长期用药")
    @PutMapping("/{id}/medications/{recordId}")
    public Result<MedicationVO> updateMedication(@PathVariable Long id, @PathVariable Long recordId,
                                                 @Valid @RequestBody MedicationRequest request) {
        return Result.success(memberService.updateMedication(
                SecurityUtils.getCurrentUserId(), id, recordId, request));
    }

    @Operation(summary = "删除长期用药")
    @DeleteMapping("/{id}/medications/{recordId}")
    public Result<Void> deleteMedication(@PathVariable Long id, @PathVariable Long recordId) {
        memberService.deleteMedication(SecurityUtils.getCurrentUserId(), id, recordId);
        return Result.success();
    }

    @Operation(summary = "家族史列表")
    @GetMapping("/{id}/family-histories")
    public Result<List<FamilyHistoryVO>> listFamilyHistories(@PathVariable Long id) {
        return Result.success(memberService.listFamilyHistories(SecurityUtils.getCurrentUserId(), id));
    }

    @Operation(summary = "新增家族史")
    @PostMapping("/{id}/family-histories")
    public Result<FamilyHistoryVO> addFamilyHistory(@PathVariable Long id,
                                                    @Valid @RequestBody FamilyHistoryRequest request) {
        return Result.success(memberService.addFamilyHistory(SecurityUtils.getCurrentUserId(), id, request));
    }

    @Operation(summary = "修改家族史")
    @PutMapping("/{id}/family-histories/{recordId}")
    public Result<FamilyHistoryVO> updateFamilyHistory(@PathVariable Long id, @PathVariable Long recordId,
                                                       @Valid @RequestBody FamilyHistoryRequest request) {
        return Result.success(memberService.updateFamilyHistory(
                SecurityUtils.getCurrentUserId(), id, recordId, request));
    }

    @Operation(summary = "删除家族史")
    @DeleteMapping("/{id}/family-histories/{recordId}")
    public Result<Void> deleteFamilyHistory(@PathVariable Long id, @PathVariable Long recordId) {
        memberService.deleteFamilyHistory(SecurityUtils.getCurrentUserId(), id, recordId);
        return Result.success();
    }

    @Operation(summary = "生活习惯查询")
    @GetMapping("/{id}/lifestyle")
    public Result<LifestyleVO> getLifestyle(@PathVariable Long id) {
        return Result.success(memberService.getLifestyle(SecurityUtils.getCurrentUserId(), id));
    }

    @Operation(summary = "生活习惯保存（upsert）")
    @PutMapping("/{id}/lifestyle")
    public Result<LifestyleVO> saveLifestyle(@PathVariable Long id, @Valid @RequestBody LifestyleRequest request) {
        return Result.success(memberService.saveLifestyle(SecurityUtils.getCurrentUserId(), id, request));
    }
}
