package me.nald.blog.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import me.nald.blog.data.dto.AccountDtoTest;
import me.nald.blog.data.entity.AccountLog;
import me.nald.blog.data.entity.QAccount;
import me.nald.blog.data.entity.QAccountLog;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;
import org.springframework.stereotype.Repository;


@Repository
public class AccountLogQueryRepository extends QuerydslRepositorySupport {
    private final JPAQueryFactory jpaQueryFactory;
    private final QAccountLog accountLog; // QueryDSL을 위한 엔티티 Q클래스

    public AccountLogQueryRepository(final JPAQueryFactory jpaQueryFactory) {
        super(AccountLog.class);
        this.jpaQueryFactory = jpaQueryFactory;
        this.accountLog = QAccountLog.accountLog;
    }



}