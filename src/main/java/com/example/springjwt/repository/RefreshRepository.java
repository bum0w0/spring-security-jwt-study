package com.example.springjwt.repository;

import com.example.springjwt.entity.RefreshEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.transaction.annotation.Transactional;

public interface RefreshRepository extends CrudRepository<RefreshEntity, String> {

    boolean existsById(String refresh);  // refresh token 존재 여부 확인

    @Transactional
    void deleteById(String refresh);  // refresh token 삭제

}
