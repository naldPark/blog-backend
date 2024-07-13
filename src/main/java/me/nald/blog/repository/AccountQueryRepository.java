package me.nald.blog.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import me.nald.blog.data.dto.AccountDtoTest;
import me.nald.blog.data.entity.Account;
import me.nald.blog.data.entity.QAccount;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class AccountQueryRepository extends QuerydslRepositorySupport {
    private final JPAQueryFactory jpaQueryFactory;
    private final QAccount account; // QueryDSL을 위한 엔티티 Q클래스

    public AccountQueryRepository(final JPAQueryFactory jpaQueryFactory) {
        super(Account.class);
        this.jpaQueryFactory = jpaQueryFactory;
        this.account = QAccount.account;
    }

    public List<AccountDtoTest> findByTest() {
        return jpaQueryFactory
                //Projections.map ,Projections.fields 이런것도 있음
                .select(Projections.constructor(
                        AccountDtoTest.class,
                        account.accountId,
                        account.accountName
                ))
                .from(account)
                .fetch();
    }
}