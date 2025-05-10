package com.example.springjwt.entity;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

@Getter
@Setter
@RedisHash(value = "refresh", timeToLive = 86400)
public class RefreshEntity {

    // @RedisHash 어노테이션은 Redis Lettuce를 사용하기 위해 작성, value는 redis key 값으로 사용
    // Redis 저장소에 key는 {value}:{@Id 어노테이션이 붙은 값} 형식으로 저장됨

    @Id
    private String refresh;

    private String username;

}
