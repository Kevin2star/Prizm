package com.prizm.service;

import com.prizm.api.dto.JoinSpaceResponse;
import com.prizm.api.dto.SpaceDetailResponse;
import com.prizm.api.dto.SpaceResponse;
import com.prizm.api.error.ApiException;
import com.prizm.domain.JoinCodeGenerator;
import com.prizm.domain.Member;
import com.prizm.domain.Space;
import com.prizm.realtime.SpaceEventPublisher;
import com.prizm.repository.MemberRepository;
import com.prizm.repository.SpaceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SpaceService {

    static final int JOIN_CODE_MAX_ATTEMPTS = 8;

    private final SpaceRepository spaceRepository;
    private final MemberRepository memberRepository;
    private final JoinCodeGenerator joinCodeGenerator;
    private final SpaceEventPublisher spaceEventPublisher;

    public SpaceService(
            SpaceRepository spaceRepository,
            MemberRepository memberRepository,
            JoinCodeGenerator joinCodeGenerator,
            SpaceEventPublisher spaceEventPublisher
    ) {
        this.spaceRepository = spaceRepository;
        this.memberRepository = memberRepository;
        this.joinCodeGenerator = joinCodeGenerator;
        this.spaceEventPublisher = spaceEventPublisher;
    }

    @Transactional
    public SpaceResponse create(String name) {
        String trimmed = requireText(name, "SPACE_NAME_REQUIRED", "스페이스 이름을 입력하세요.");
        Space space = spaceRepository.save(new Space(trimmed, uniqueJoinCode()));
        return new SpaceResponse(space.getId(), space.getName(), space.getJoinCode());
    }

    @Transactional
    public JoinSpaceResponse join(String code, String nickname, String school, String major) {
        String normalized = code == null ? "" : code.trim().toUpperCase();
        Space space = spaceRepository.findByJoinCode(normalized)
                .orElseThrow(() -> ApiException.notFound("SPACE_NOT_FOUND", "참여 코드를 찾을 수 없습니다."));
        Member member = memberRepository.save(new Member(
                space,
                requireText(nickname, "NICKNAME_REQUIRED", "닉네임을 입력하세요."),
                requireText(school, "SCHOOL_REQUIRED", "학교를 입력하세요."),
                requireText(major, "MAJOR_REQUIRED", "전공을 입력하세요.")
        ));
        spaceEventPublisher.memberJoined(space.getId(), member.getNickname(), member.getMajor());
        return new JoinSpaceResponse(
                member.getId(),
                space.getId(),
                member.getNickname(),
                member.getSchool(),
                member.getMajor()
        );
    }

    @Transactional(readOnly = true)
    public SpaceDetailResponse get(Long id) {
        Space space = spaceRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("SPACE_NOT_FOUND", "스페이스를 찾을 수 없습니다."));
        long memberCount = memberRepository.countBySpaceId(space.getId());
        return new SpaceDetailResponse(space.getId(), space.getName(), space.getJoinCode(), memberCount);
    }

    String uniqueJoinCode() {
        for (int attempt = 0; attempt < JOIN_CODE_MAX_ATTEMPTS; attempt++) {
            String code = joinCodeGenerator.nextCode();
            if (!spaceRepository.existsByJoinCode(code)) {
                return code;
            }
        }
        throw ApiException.badRequest("JOIN_CODE_EXHAUSTED", "참여 코드를 발급하지 못했습니다. 다시 시도하세요.");
    }

    private String requireText(String value, String error, String message) {
        if (value == null || value.isBlank()) {
            throw ApiException.badRequest(error, message);
        }
        return value.trim();
    }

}
