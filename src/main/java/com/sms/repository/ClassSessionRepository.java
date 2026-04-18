package com.sms.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sms.model.ClassSession;

@Repository
public interface ClassSessionRepository extends JpaRepository<ClassSession, Long> {
    List<ClassSession> findTop10ByStudentIdAndStartsAtAfterOrderByStartsAtAsc(String studentId, LocalDateTime now);

    List<ClassSession> findByStartsAtBetween(LocalDateTime fromInclusive, LocalDateTime toInclusive);

        @Query("""
                        SELECT COUNT(s) > 0
                        FROM ClassSession s
                        WHERE s.student.id = :studentId
                            AND s.id <> :sessionId
                            AND s.startsAt < :endsAt
                            AND s.endsAt > :startsAt
                        """)
        boolean existsOverlappingSession(
                        @Param("studentId") String studentId,
                        @Param("sessionId") Long sessionId,
                        @Param("startsAt") LocalDateTime startsAt,
                        @Param("endsAt") LocalDateTime endsAt
        );
}
