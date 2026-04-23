package com.mindamd.taskextractor.domain.repository;

import com.mindamd.taskextractor.domain.entity.MeetingParticipant;
import com.mindamd.taskextractor.domain.entity.Summary;
import com.mindamd.taskextractor.domain.entity.UserInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface MeetingParticipantRepository extends JpaRepository<MeetingParticipant, Long> {

    // 특정 요약본에 참여한 유저 목록 (가명 → 실제 유저 매핑용)
    List<MeetingParticipant> findAllBySummary(Summary summary);

    // 특정 유저가 참여한 모든 요약본 (개인정보 추적 / 이용 내역 조회)
    List<MeetingParticipant> findAllByUserInfo(UserInfo userInfo);

    // 개인정보보호법 삭제권: 유저 삭제 시 참여 기록도 함께 제거
    @Modifying
    @Query("DELETE FROM MeetingParticipant mp WHERE mp.userInfo = :userInfo")
    void deleteAllByUserInfo(UserInfo userInfo);
}
