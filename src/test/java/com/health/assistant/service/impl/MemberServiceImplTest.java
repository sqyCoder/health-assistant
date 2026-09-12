package com.health.assistant.service.impl;

import com.health.assistant.common.exception.BizException;
import com.health.assistant.common.exception.ErrorCode;
import com.health.assistant.dto.member.LifestyleRequest;
import com.health.assistant.dto.member.MemberSaveRequest;
import com.health.assistant.entity.FamilyMember;
import com.health.assistant.entity.MemberLifestyle;
import com.health.assistant.mapper.FamilyMemberMapper;
import com.health.assistant.mapper.MemberAllergyMapper;
import com.health.assistant.mapper.MemberFamilyHistoryMapper;
import com.health.assistant.mapper.MemberLifestyleMapper;
import com.health.assistant.mapper.MemberMedicalHistoryMapper;
import com.health.assistant.mapper.MemberMedicationMapper;
import com.health.assistant.support.MemberAccessChecker;
import com.health.assistant.vo.LifestyleVO;
import com.health.assistant.vo.MemberVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberServiceImplTest {

    @Mock
    private FamilyMemberMapper familyMemberMapper;

    @Mock
    private MemberMedicalHistoryMapper medicalHistoryMapper;

    @Mock
    private MemberAllergyMapper allergyMapper;

    @Mock
    private MemberMedicationMapper medicationMapper;

    @Mock
    private MemberFamilyHistoryMapper familyHistoryMapper;

    @Mock
    private MemberLifestyleMapper lifestyleMapper;

    @Mock
    private MemberAccessChecker memberAccessChecker;

    @InjectMocks
    private MemberServiceImpl memberService;

    @Test
    void createMemberShouldInsertWithUserIdAndComputeBmi() {
        MemberSaveRequest request = buildMemberRequest("张三", "SELF");

        MemberVO result = memberService.createMember(100L, request);

        ArgumentCaptor<FamilyMember> captor = ArgumentCaptor.forClass(FamilyMember.class);
        verify(familyMemberMapper).insert(captor.capture());
        FamilyMember saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(100L);
        assertThat(saved.getSort()).isZero();
        assertThat(result.getName()).isEqualTo("张三");
        assertThat(result.getBmi()).isEqualByComparingTo(new BigDecimal("22.5"));
    }

    @Test
    void createMemberShouldRejectInvalidRelation() {
        MemberSaveRequest request = buildMemberRequest("张三", "BROTHER");

        assertThatThrownBy(() -> memberService.createMember(100L, request))
                .isInstanceOfSatisfying(BizException.class, e ->
                        assertThat(e.getCode()).isEqualTo(ErrorCode.PARAM_ERROR.getCode()));
        verify(familyMemberMapper, never()).insert(any(FamilyMember.class));
    }

    @Test
    void deleteMemberShouldCascadeLogicalDelete() {
        FamilyMember member = new FamilyMember();
        member.setId(10L);
        member.setUserId(100L);
        when(memberAccessChecker.check(100L, 10L)).thenReturn(member);

        memberService.deleteMember(100L, 10L);

        verify(medicalHistoryMapper).delete(any());
        verify(allergyMapper).delete(any());
        verify(medicationMapper).delete(any());
        verify(familyHistoryMapper).delete(any());
        verify(lifestyleMapper).delete(any());
        verify(familyMemberMapper).deleteById(10L);
    }

    @Test
    void saveLifestyleShouldInsertWhenAbsent() {
        FamilyMember member = new FamilyMember();
        member.setId(10L);
        member.setUserId(100L);
        when(memberAccessChecker.check(100L, 10L)).thenReturn(member);
        when(lifestyleMapper.selectOne(any())).thenReturn(null);
        when(lifestyleMapper.insert(any(MemberLifestyle.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, MemberLifestyle.class).setId(99L);
            return 1;
        });

        LifestyleRequest request = new LifestyleRequest();
        request.setSmoking("NEVER");
        request.setSleepHours(new BigDecimal("7.5"));

        LifestyleVO result = memberService.saveLifestyle(100L, 10L, request);

        ArgumentCaptor<MemberLifestyle> captor = ArgumentCaptor.forClass(MemberLifestyle.class);
        verify(lifestyleMapper).insert(captor.capture());
        assertThat(captor.getValue().getMemberId()).isEqualTo(10L);
        assertThat(captor.getValue().getSmoking()).isEqualTo("NEVER");
        assertThat(result.getSleepHours()).isEqualByComparingTo(new BigDecimal("7.5"));
    }

    @Test
    void saveLifestyleShouldRejectInvalidSmokingValue() {
        FamilyMember member = new FamilyMember();
        member.setId(10L);
        member.setUserId(100L);
        when(memberAccessChecker.check(100L, 10L)).thenReturn(member);

        LifestyleRequest request = new LifestyleRequest();
        request.setSmoking("SOMETIMES");

        assertThatThrownBy(() -> memberService.saveLifestyle(100L, 10L, request))
                .isInstanceOfSatisfying(BizException.class, e ->
                        assertThat(e.getCode()).isEqualTo(ErrorCode.PARAM_ERROR.getCode()));
        verify(lifestyleMapper, never()).insert(any(MemberLifestyle.class));
    }

    private MemberSaveRequest buildMemberRequest(String name, String relation) {
        MemberSaveRequest request = new MemberSaveRequest();
        request.setName(name);
        request.setRelation(relation);
        request.setGender(1);
        request.setHeightCm(new BigDecimal("172.0"));
        request.setWeightKg(new BigDecimal("66.5"));
        return request;
    }
}
