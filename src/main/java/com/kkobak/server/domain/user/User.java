package com.kkobak.server.domain.user;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// @Getter: 필드마다 getXxx() 를 자동으로 만든다. (setter 는 일부러 만들지 않는다)
// @NoArgsConstructor(PROTECTED): JPA 가 DB 행을 객체로 만들 때 쓰는 빈 생성자. 우리 코드는 직접 부르지 않는다.
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String name;

    @Column(name = "google_sub", nullable = false)
    private String googleSub;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    // 생성 시각은 직접 꺼내지 않고 받는다. 부르는 쪽(Service)이 주입받은 Clock 으로 now 를 넘긴다.
    public User(String email, String name, String googleSub, Instant now) {
        this.email = email;
        this.name = name;
        this.googleSub = googleSub;
        this.createdAt = now;
    }
}
