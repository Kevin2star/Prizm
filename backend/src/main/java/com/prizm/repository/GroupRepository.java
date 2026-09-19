package com.prizm.repository;

import com.prizm.domain.Group;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupRepository extends JpaRepository<Group, Long> {

    List<Group> findBySpaceId(Long spaceId);
}
