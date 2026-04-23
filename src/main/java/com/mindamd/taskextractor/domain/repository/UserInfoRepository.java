package com.mindamd.taskextractor.domain.repository;

import com.mindamd.taskextractor.domain.entity.UserInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserInfoRepository extends JpaRepository<UserInfo, Long> {

    // Discord snowflake ID로 유저 조회 (주 lookup 경로)
    Optional<UserInfo> findByDiscordUserId(String discordUserId);

    boolean existsByDiscordUserId(String discordUserId);
}
