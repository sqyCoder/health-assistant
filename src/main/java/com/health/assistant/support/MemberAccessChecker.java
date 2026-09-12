package com.health.assistant.support;

import com.health.assistant.common.exception.BizException;
import com.health.assistant.common.exception.ErrorCode;
import com.health.assistant.entity.FamilyMember;
import com.health.assistant.mapper.FamilyMemberMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MemberAccessChecker {

    private final FamilyMemberMapper familyMemberMapper;

    public FamilyMember check(Long userId, Long memberId) {
        FamilyMember member = familyMemberMapper.selectById(memberId);
        if (member == null || !member.getUserId().equals(userId)) {
            throw new BizException(ErrorCode.MEMBER_NOT_FOUND);
        }
        return member;
    }
}
