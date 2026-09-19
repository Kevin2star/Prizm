package com.prizm.repository;

import com.prizm.domain.Space;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpaceRepository extends JpaRepository<Space, Long> {

    Optional<Space> findByJoinCode(String joinCode);

    boolean existsByJoinCode(String joinCode);
}
