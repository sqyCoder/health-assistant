package com.health.assistant.support;

import com.health.assistant.common.exception.BizException;
import com.health.assistant.common.exception.ErrorCode;
import com.health.assistant.entity.FamilyMember;
import com.health.assistant.mapper.FamilyMemberMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberAccessCheckerTest {

    @Mock
    private FamilyMemberMapper familyMemberMapper;

    @InjectMocks
    private MemberAccessChecker checker;

    @Test
    void shouldReturnMemberWhenOwnedByUser() {
        FamilyMember member = memberOf(1L);
        when(familyMemberMapper.selectById(10L)).thenReturn(member);

        assertThat(checker.check(1L, 10L)).isSameAs(member);
    }

    @Test
    void shouldThrowWhenMemberNotFound() {
        when(familyMemberMapper.selectById(10L)).thenReturn(null);

        assertThatThrownBy(() -> checker.check(1L, 10L))
                .isInstanceOfSatisfying(BizException.class, e ->
                        assertThat(e.getCode()).isEqualTo(ErrorCode.MEMBER_NOT_FOUND.getCode()));
    }

    @Test
    void shouldThrowWhenMemberBelongsToAnotherUser() {
        when(familyMemberMapper.selectById(10L)).thenReturn(memberOf(2L));

        assertThatThrownBy(() -> checker.check(1L, 10L))
                .isInstanceOfSatisfying(BizException.class, e ->
                        assertThat(e.getCode()).isEqualTo(ErrorCode.MEMBER_NOT_FOUND.getCode()));
    }

    private FamilyMember memberOf(Long userId) {
        FamilyMember member = new FamilyMember();
        member.setId(10L);
        member.setUserId(userId);
        member.setName("测试成员");
        return member;
    }
}
