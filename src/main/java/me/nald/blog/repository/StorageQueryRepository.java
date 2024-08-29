package me.nald.blog.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import me.nald.blog.data.entity.Account;
import me.nald.blog.data.entity.QSandbox;
import me.nald.blog.data.entity.QStorage;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;
import org.springframework.stereotype.Repository;

@Repository
public class StorageQueryRepository extends QuerydslRepositorySupport {
    private final JPAQueryFactory jpaQueryFactory;
    private final QStorage storage; // QueryDSL을 위한 엔티티 Q클래스

    public StorageQueryRepository(final JPAQueryFactory jpaQueryFactory) {
        super(Account.class);
        this.jpaQueryFactory = jpaQueryFactory;
        this.storage = QStorage.storage;
    }

}