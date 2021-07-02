package inflearn.querydsl;

import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.JPQLQueryFactory;
import com.querydsl.jpa.impl.JPAQueryFactory;
import inflearn.querydsl.dto.MemberDTO;
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
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;

import java.util.List;

import static com.querydsl.jpa.JPAExpressions.*;
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
        // [ 팀의 이름과 각 팀의 평균 나이를 구해라 ]
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

    @Test // 조인 테스트 1 - 기본 조인 | join(조인 대상, 별칭으로 사용할 Q타입)
    public void join() {
        /* [ 팀 A 에 속한 모든 회원 구하기 ] */
        List<Member> result = queryFactory
                .selectFrom(member)
                .join(member.team, team) // join() = innerJoin()
                .where(team.name.eq("teamA"))
                .fetch();

        /* select
            member1
        from
            Member member1
        inner join
            member1.team as team
        where
            team.name = ?1 */

        assertThat(result)
                .extracting("username")
                .containsExactly("member1", "member2");


    }

    @Test // 조인 테스트 2 - 세타 조인 : 연관관계가 없는 필드로 조인
    public void theta_join() {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        /* [ 회원의 이름이 팀 이름과 같은 회원 조회 ] */
        List<Member> result = queryFactory
                .select(member)
                .from(member, team)
                .where(member.username.eq(team.name))
                .fetch();
        // - 일반 조인 : join(member.team, team) - member 와 연관관계가 있는 team 을 지정
        // - 세타 조인 : from(member, team) - from 절에 여러개를 나열한것
        // - 모든 회원과 모든 팀을 가져온뒤 전부 조인 시키고 where 절에서 필터링을 거침 - DB가 최적화 시킴

        assertThat(result)
                .extracting("username")
                .containsExactly("teamA", "teamB");
    }

    /** 조인 테스트 3 - on : 조인 대상 필터링
     * 예) 회원과 팀을 조인하면서, 팀 이름이 teamA 인 팀만 조인, 회원은 모두 조회
     * JPQL: SELECT m, t FROM Member m LEFT JOIN m.team t on t.name = 'teamA'
     * SQL : SELECT m.*, t.* FROM Member m LEFT JOIN Team t ON m.TEAM_ID=t.id and t.name='teamA'
     */
    @Test
    public void join_on_filtering() {
        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .join(member.team, team)
                .on(team.name.eq("teamA"))
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    /** 조인 테스트 4 - on : 연관관계 없는 엔티티 외부 조인
     * 예) 회원의 이름이 팀 이름과 같은 대상 외부 조인
     * JPQL: SELECT m, t FROM Member m LEFT JOIN Team t on m.username = t.name
     * SQL: SELECT m.*, t.* FROM Member m LEFT JOIN Team t ON m.username = t.name
     */
    @Test
    public void join_on_no_relation() {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
//                .leftJoin(member.team, team) 보통 이렇게 사용 : on member.team_id=team.id (id 끼리 매칭)
                .leftJoin(team) // member 이름과 team 이름이 같을경우 전부 join
                .on(member.username.eq(team.name))
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    @PersistenceUnit
    EntityManagerFactory emf;

    @Test // 조인 테스트 5 : 페치 조인 미적용
    public void fetchJoinNo() {
        em.flush();
        em.clear();

        Member findMember = queryFactory
                .selectFrom(QMember.member)
                .where(QMember.member.username.eq("member1"))
                .fetchOne();

        // findMember.getTeam()이 로딩된 엔티티 인지
        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());

        assertThat(loaded).as("페치 조인 미적용").isFalse();
    }

    @Test // 조인 테스트 6 : 페치 조인 적용
    public void fetchJoinUse() {
        em.flush();
        em.clear();

        Member findMember = queryFactory
                .selectFrom(QMember.member)
                .join(member.team, team).fetchJoin()
                .where(QMember.member.username.eq("member1"))
                .fetchOne();

        // findMember.getTeam()이 로딩된 엔티티 인지
        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());

        assertThat(loaded).as("페치 조인 미적용").isTrue();
    }

    /** 서브 쿼리 테스트 1
     * 나이가 가장 많은 회원 조회
     */
    @Test
    public void subQuery() {

        // 서비 쿼리를 사용할때 이름이 겹치지 않기 위해
        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(
                        select(memberSub.age.max())
                                .from(memberSub)
                ))
                .fetch();

        assertThat(result).extracting("age").containsExactly(40);
    }

    /** 서브 쿼리 테스트 2
     * 나이가 평균 이상인 회원
     */
    @Test
    public void subQueryGoe() {

        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.goe(
                        select(memberSub.age.avg())
                                .from(memberSub)
                ))
                .fetch();

        assertThat(result).extracting("age").containsExactly(30, 40);
    }

    /** 서브 쿼리 테스트 3
     * 나이가 10보다 큰 회원
     */
    @Test
    public void subQueryIn() {

        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.in(
                    select(memberSub.age)
                        .from(memberSub)
                        .where(memberSub.age.gt(10))
                ))
                .fetch();

        assertThat(result).extracting("age")
                .containsExactly(20, 30, 40);

        for (Member member : result) {
            System.out.println(member);
        }
    }

    /** 서브 쿼리 테스트 4 : select 절에 subQuery
     * 회원 이름과 회원 평균 나이 구하기
     */
    @Test
    public void selectSubQuery() {

        QMember memberSub = new QMember("memberSub");

        List<Tuple> result = queryFactory
                .select(member.username,
                        select(memberSub.age.avg())
                                .from(memberSub)
                )
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println(tuple);
        }
    }

    @Test // case 문 테스트 1 : 단순한 조건
    public void basicCase() {
        List<String> result = queryFactory
                .select(
                    member.age
                        .when(10).then("열살살")
                        .when(20).then("스무살")
                        .otherwise("기타")
                )
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println(s);
        }
    }

    @Test // case 문 테스트 2 : 복잡한 조건 - CaseBuilder 를 사용
    public void complexCase() {
        List<String> result = queryFactory
                .select(new CaseBuilder()
                        .when(member.age.between(0, 20)).then("0~20살")
                        .when(member.age.between(21, 30)).then("21~30살")
                        .otherwise("기타"))
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println(s);
        }
    }

    /** case 문 테스트 3 : orderBy 에서 Case 문 함께 사용하기
     * 다음과 같은 임의의 순으로 회원을 출력하고 싶다면?
     * 0 ~ 30 살이 아닌 회원을 가장 먼저 출력
     * 0 ~ 20 살 회원 출력
     * 21 ~ 30 살 회원 출력
     */
    @Test
    public void orderBy_Case(){
        NumberExpression<Integer> rankPath = new CaseBuilder()
                .when(member.age.between(0, 20)).then(2)
                .when(member.age.between(21, 30)).then(1)
                .otherwise(3);

        List<Tuple> result = queryFactory
                .select(member.username, member.age, rankPath)
                .from(member)
                .orderBy(rankPath.desc())
                .fetch();

        for (Tuple tuple : result) {
            String username = tuple.get(member.username);
            Integer age = tuple.get(member.age);
            Integer rank = tuple.get(rankPath);
            System.out.println("username : " + username + " | age : " + age + " | rank : " + rank);
        }
    }

    @Test // 상수 더하기 - 상수가 필요하면 Expressions.constant(xxx)를 사용
    public void constant() {
        List<Tuple> result = queryFactory
                .select(member.username, Expressions.constant("A"))
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println(tuple);
        }
    }

    @Test // 문자 더하기
    public void concat() {
        // username_age 형식으로 출력하기
        List<String> result = queryFactory
                .select(member.username.concat("_").concat(member.age.stringValue()))
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println(s);
        }
    }

    @Test // 순수 JPA 에서 DTO 조회
    public void findDtoByJPQL() {

        /* em.createQuery("select m from Member m", MemberDTO.class);
         * 이렇게 하면 Member 엔티티를 조회하기 떄문에 타입이 맞지 않아 오류가 발생
         * new operation 을 사용해야 한다.
         */
        List<MemberDTO> result = em.createQuery(
                "select new inflearn.querydsl.dto.MemberDTO(m.username, m.age) from Member m", MemberDTO.class)
                .getResultList();

        for (MemberDTO memberDTO : result) {
            System.out.println("memberDTO : " + memberDTO);
        }
    }
}
