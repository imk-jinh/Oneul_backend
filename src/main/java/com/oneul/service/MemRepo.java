package com.oneul.service;
import javax.persistence.EntityManager;

import org.springframework.stereotype.Repository;

import com.oneul.model.Member;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class MemRepo {
    private final EntityManager em;
    
    public void setName(Member member){
        em.persist(member);
    }
}
