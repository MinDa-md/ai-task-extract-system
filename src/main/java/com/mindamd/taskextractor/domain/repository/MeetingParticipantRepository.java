package com.mindamd.taskextractor.domain.repository;

import com.mindamd.taskextractor.domain.entity.MeetingParticipant;
import com.mindamd.taskextractor.domain.entity.Summary;
import com.mindamd.taskextractor.domain.entity.UserInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface MeetingParticipantRepository extends JpaRepository<MeetingParticipant, Long> {

    List<MeetingParticipant> findAllBySummary(Summary summary);

    List<MeetingParticipant> findAllByUserInfo(UserInfo userInfo);

    @Modifying
    @Query("DELETE FROM MeetingParticipant mp WHERE mp.userInfo = :userInfo")
    void deleteAllByUserInfo(UserInfo userInfo);
}
