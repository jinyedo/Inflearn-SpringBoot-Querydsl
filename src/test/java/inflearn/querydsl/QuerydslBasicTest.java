package inflearn.querydsl;

import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.JPQLQueryFactory;
import com.querydsl.jpa.impl.JPAQueryFactory;
import inflearn.querydsl.entity.Member;
import inflearn.querydsl.entity.QMember;
import inflearn.querydsl.entity.QTeam;
import inflearn.querydsl.entity.Team;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;

import java.util.List;

import static inflearn.querydsl.entity.QTeam.*;
import static org.assertj.core.api.Assertions.*;
import static inflearn.querydsl.entity.QMember.*;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

    @Autowired
    EntityManager em;

    JPQLQueryFactory queryFactory;

    @BeforeEach
    public void before() {
        queryFactory = new JPAQueryFactory(em);

        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);

        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);
        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);
    }

    @Test // JPQL 테스트
    public void startJPQL() {
        // member1을 찾아라
        Member findMember = em.createQuery("select m from Member m where m.username = :username", Member.class)
                .setParameter("username", "member1")
                .getSingleResult();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test // Querydsl 테스트
    public void startQuerydsl() {
        Member findMember = queryFactory
                .select(member)
                .from(member)
                .where(member.username.eq("member1")) // PreparedStatement 를 통한 자동 파라미터 바인딩 처리
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    /* JPQL */
    // 파라미터 값들을 문자 + 방식으로 사용하기 때문에 SQL Injection 공격을 받을 수 있음
    // 문자로 작성 하기 때문에 실행 후 메서드를 호출했을떄 오류를 확인할 수 있음

    /* Querydsl */
    // PreparedStatement 를 통한 자동 파라미터 바인딩 처리 방식을 사용하기 때문에 SQL Injection 공격을 막을 수 있음
    // 코드로 작성하기 컴파일 시점에서 오류를 확인할 수 있음 - 문법 오류 방지

    @Test // 검색 조건 쿼리 테스트1
    public void search() {
        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1")
                        .and(member.age.eq(10))) // 이름이 member1 이고 나이가 10
                // .where(QMember.member.username.eq("member1")
                        // .or(QMember.member.age.eq(10))) 이름이 member1 이거나 나이가 10
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test // 검색 조건 쿼리 테스트2
    public void searchAndParam() {
        Member findMember = queryFactory
                .selectFrom(member)
                .where(
                    member.username.eq("member1"),
                    member.age.eq(10)
                )
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test // 결과 조회 테스트
    public void resultFetch() {
        // List
        List<Member> fetch = queryFactory
                .selectFrom(member)
                .fetch();

        // 단건
        Member fetchOne = queryFactory
                .selectFrom(member)
                .fetchOne();

        // 처음 한 건 조회
        Member fetchFirst = queryFactory
                .selectFrom(member)
                .fetchFirst();

        // 페이징에서 사용
        // 쿼리 두 번 실행 - count 쿼리 실행 후 select 쿼리 실행
        QueryResults<Member> fetchResults = queryFactory
                .selectFrom(member)
                .fetchResults();
        long total = fetchResults.getTotal(); // count 개수
        List<Member> content = fetchResults.getResults(); // 데이터 꺼내오기

        // count 쿼리로 변경
        long fetchCount = queryFactory
                .selectFrom(member)
                .fetchCount();
    }

    @Test // 정렬 테스트
    public void sort() {
        em.persist(new Member(null, 100));
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));

        /* 회원 정렬 순서
         * 1. 회원 나이 내림차순(desc)
         * 2. 회원 이름 올림차순(asc)
         * 단 2에서 회원 이름이 없으면 마지막에 출력(nulls last)
         */
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.username.asc().nullsLast())
                .fetch();

        Member member5 = result.get(0);
        Member member6 = result.get(1);
        Member memberNull = result.get(2);
        assertThat(member5.getUsername()).isEqualTo("member5");
        assertThat(member6.getUsername()).isEqualTo("member6");
        assertThat(memberNull.getUsername()).isNull();
    }

    @Test // 페이징 테스트 1 - 조회 건수 제한
    public void paging1() {
        List<Member> result = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1) // 몇 번째 row 부터 시작할지 - 1이면 2번째 row 부터
                .limit(2)
                .fetch();

        assertThat(result.size()).isEqualTo(2);
    }

    @Test // 페이징 테스트 2 - 전체 조회
    public void paging2() {
        QueryResults<Member> fetchResults = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1) // 몇 번째 row 부터 시작할지 - 1이면 2번째 row 부터
                .limit(2)
                .fetchResults();

        assertThat(fetchResults.getTotal()).isEqualTo(4);
        assertThat(fetchResults.getLimit()).isEqualTo(2);
        assertThat(fetchResults.getOffset()).isEqualTo(1);
        assertThat(fetchResults.getResults().size()).isEqualTo(2);
    }

    @Test // 집합 테스트 1
    public void aggregation() {
        // Querydsl 이 제공하는 Tuple
        List<Tuple> result = queryFactory
                .select(
                        member.count(),
                        member.age.sum(),
                        member.age.avg(),
                        member.age.max(),
                        member.age.min()
                )
                .from(member)
                .fetch();

        Tuple tuple = result.get(0);
        assertThat(tuple.get(member.count())).isEqualTo(4);
        assertThat(tuple.get(member.age.sum())).isEqualTo(100);
        assertThat(tuple.get(member.age.avg())).isEqualTo(25);
        assertThat(tuple.get(member.age.max())).isEqualTo(40);
        assertThat(tuple.get(member.age.min())).isEqualTo(10);
    }

    @Test // 집합 테스트 2 - GroupBy 테스트
    public void group() {
        // 팀의 이름과 각 팀의 평균 나이를 구해라
        List<Tuple> result = queryFactory
                .select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
                .fetch();

        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);

        assertThat(teamA.get(team.name)).isEqualTo("teamA");
        assertThat(teamA.get(member.age.avg())).isEqualTo(15);

        assertThat(teamB.get(team.name)).isEqualTo("teamB");
        assertThat(teamB.get(member.age.avg())).isEqualTo(35);
    }
}
