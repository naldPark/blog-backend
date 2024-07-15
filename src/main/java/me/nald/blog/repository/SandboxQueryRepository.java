package me.nald.blog.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import me.nald.blog.data.dto.AccountDtoTest;
import me.nald.blog.data.entity.Account;
import me.nald.blog.data.entity.QAccount;
import me.nald.blog.data.entity.QSandbox;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class SandboxQueryRepository extends QuerydslRepositorySupport {
    private final JPAQueryFactory jpaQueryFactory;
    private final QSandbox sandbox; // QueryDSL을 위한 엔티티 Q클래스

    public SandboxQueryRepository(final JPAQueryFactory jpaQueryFactory) {
        super(Account.class);
        this.jpaQueryFactory = jpaQueryFactory;
        this.sandbox = QSandbox.sandbox;
    }

}