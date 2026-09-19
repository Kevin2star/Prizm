package com.prizm.repository;

import com.prizm.domain.Member;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Long> {

    List<Member> findBySpaceId(Long spaceId);

    long countBySpaceId(Long spaceId);

    Optional<Member> findByIdAndSpaceId(Long id, Long spaceId);
}
