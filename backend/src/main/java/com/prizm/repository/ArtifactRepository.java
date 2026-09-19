package com.prizm.repository;

import com.prizm.domain.Artifact;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ArtifactRepository extends JpaRepository<Artifact, Long> {

    @EntityGraph(attributePaths = {"member", "group", "space"})
    List<Artifact> findBySpaceIdOrderByCreatedAtDesc(Long spaceId);

    @EntityGraph(attributePaths = {"member", "group", "space"})
    List<Artifact> findBySpaceId(Long spaceId);

    @EntityGraph(attributePaths = {"member", "space"})
    List<Artifact> findByGroupId(Long groupId);

    @Query("select a from Artifact a join fetch a.member left join fetch a.group where a.space.id = :spaceId")
    List<Artifact> findGraphBySpaceId(@Param("spaceId") Long spaceId);

    @Query("select a from Artifact a join fetch a.member left join fetch a.group join fetch a.space where a.id = :id")
    Optional<Artifact> findDetailById(@Param("id") Long id);
}
