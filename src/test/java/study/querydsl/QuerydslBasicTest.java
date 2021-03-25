package study.querydsl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberDto;
import study.querydsl.dto.QMemberDto;
import study.querydsl.dto.UserDto;
import study.querydsl.entity.*;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;

import java.util.List;
import java.util.stream.Collectors;

import static com.querydsl.jpa.JPAExpressions.*;
import static org.assertj.core.api.Assertions.*;
import static study.querydsl.entity.QMember.*;
import static study.querydsl.entity.QTeam.*;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

    @Autowired
    EntityManager em;
    JPAQueryFactory query;


    @BeforeEach
    public void before(){
        query = new JPAQueryFactory(em);
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



    @Test
    void startJPQL(){
        //member1을 찾아라
        Member findMember = em.createQuery("select m from Member m where m.username= :username", Member.class)
                .setParameter("username", "member1")
                .getSingleResult();

    }
    @Test
    void startQuerydsl(){
        Member findMember = query
                .select(member)
                .from(member)
                .where(member.username.eq("member1"))//파라미터 바인딩
                .fetchOne();
    }

    @Test
    void search(){
        Member member1 = query
                .selectFrom(member)
                .where(member.username.eq("member1").and(member.age.eq(10)))
                .fetchOne();

    }
    @Test
    void searchAndParam(){
        Member member1 = query
                .selectFrom(member)
                .where(member.username.eq("member1"),
                        (member.age.eq(10)))
                .fetchOne();//위에랑 같다.
    }
    @Test
    void resultFetch(){
        List<Member> fetch = query
                .selectFrom(member)
                .fetch();

        Member fetchOne = query
                .selectFrom(QMember.member)
                .fetchOne();
        Member fetchFirst = query
                .selectFrom(QMember.member)
                .fetchFirst();


        QueryResults<Member> results = query
                .selectFrom(member)
                .fetchResults();
        results.getTotal();
        List<Member> content = results.getResults();

        long total = query.selectFrom(member)
                .fetchCount();
    }

    @Test
    void sort(){

        em.persist(new Member(null, 100));
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));

        List<Member> result = query.selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.username.asc().nullsLast())
                .fetch();

        Member member5 = result.get(0);

        assertThat(member5.getUsername()).isEqualTo("member5");
    }

    @Test
    void paging1(){
        List<Member> result = query
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetch();//전체 조회수가 필요하다면 fetchResults()하면 된다.
        //성능상 countQuery는 분리하는게 더 좋다.

        assertThat(result.size()).isEqualTo(2);
    }

    @Test
    void aggrefation(){
        List<Tuple> result = query.select(
                member.count(), member.age.sum(), member.age.avg())
                .from(member)
                .fetch();

        Tuple tuple = result.get(0);

        assertThat(tuple.get(member.count())).isEqualTo(4);
    }
    /*
    팀의 이름과 각 팀의 평균 연령을 구해라.
     */
    @Test
    void group(){
        List<Tuple> result = query.select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
                .fetch();
        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);

        assertThat(teamA.get(team.name)).isEqualTo("teamA");
    }
    /*
    팀 A에 소속된 모든 회원
     */
    @Test
    void join(){
        List<Member> result = query
                .selectFrom(member)
                .join(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch();

        assertThat(result)
                .extracting("username")
                .containsExactly("member1", "member2");

    }
    /*
    세타조인
    회원의 이름이 팀 이름과 같은 회원 조회
     */
    @Test
    void theta_join(){
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        List<Member> result = query
                .select(member)
                .from(member, team)
                .where(member.username.eq(team.name))
                .fetch();

        assertThat(result)
                .extracting("username")
                .containsExactly("teamA", "teamB");
    }
    /*
    예) 회원과 팀을 조인하면서, 팀 이름이 teamA인 팀만 조인, 회원은 모두 조회
     jpql: select m, t from Member m left join m.team t on t.name="teamA";
     */
    @Test
    void join_on_filtering(){
        List<Tuple> result = query
                .select(member, team)
                .from(member)
                .leftJoin(member.team, team).on(team.name.eq("teamA"))
                .fetch();
        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }
    @Test
    void join_on_no_relation(){
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        List<Tuple> result = query
                .select(member, team)
                .from(member)
                .leftJoin(team).on(member.username.eq(team.name))
                .fetch();

        assertThat(result)
                .extracting("username")//username을 꺼내고
                .containsExactly("teamA", "teamB");//비교한다
    }
    @PersistenceUnit
    EntityManagerFactory emf;
    @Test
    void fetchJoin(){
        em.flush();
        em.clear();
        Member findMember = query
                .selectFrom(member)
                .join(member.team, team).fetchJoin()
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        assertThat(loaded).isFalse();
    }
    /*
    나이가 가장 많은 회원 조회
     */
    @Test
    void subQuery(){

        QMember memberSub = new QMember("memberSub");
        List<Member> result = query.selectFrom(member)
                .where(member.age.eq(
                        select(memberSub.age.max())
                                .from(memberSub)
                ))
                .fetch();

        assertThat(result).extracting("age")
                .containsExactly(40);

    }
    /*
    나이가 평균 이상인 회원
     */
    @Test
    void subQueryGoe(){

        QMember memberSub = new QMember("memberSub");
        List<Member> result = query.selectFrom(member)
                .where(member.age.goe(
                        select(memberSub.age.avg())
                                .from(memberSub)
                ))
                .fetch();

        assertThat(result).extracting("age")
                .containsExactly(30,40);
    }

    @Test
    void subQueryIn(){

        QMember memberSub = new QMember("memberSub");
        List<Member> result = query.selectFrom(member)
                .where(member.age.in(
                        select(memberSub.age)
                                .from(memberSub)
                        .where(memberSub.age.gt(10))
                ))
                .fetch();

        assertThat(result).extracting("age")
                .containsExactly(20,30,40);
    }
    @Test
    void selectSubQuery(){
        QMember memberSub = new QMember("memberSub");
        List<Tuple> result = query
                .select(member.username,
                        select(memberSub.age.avg()))
                .from((memberSub))
                .from(member)
                .fetch();
    }
    @Test
    void basicCase(){
        List<String> result = query
                .select(member.age
                        .when(10).then("열살")
                        .when(20).then("스무살")
                        .otherwise("기타"))
                .from(member)
                .fetch();
    }
    @Test
    void complexCase(){
        List<String> result = query
                .select(new CaseBuilder()
                        .when(member.age.between(0, 20)).then(("0~20살"))
                        .when(member.age.between(21, 30)).then("21~30")
                        .otherwise("기타"))
                .from(member)
                .fetch();
    }
    @Test
    void constant(){
        List<Tuple> result = query
                .select(member.username, Expressions.constant("A"))
                .from(member)
                .fetch();
    }
    @Test
    void concat(){
        //username_age
        List<String> result = query
                .select(member.username.concat("_").concat(member.age.stringValue()))
                .from(member)
                .where(member.username.eq("member1"))
                .fetch();
    }
    @Test
    void simpleProjection(){
        List<String> result = query.select(member.username)
                .from(member)
                .fetch();
    }
    @Test
    void tupleProjection(){
        List<Tuple> result = query
                .select(member.username, member.age)
                .from(member)
                .fetch();
    }
    @Test
    void findDtoJPQL(){
        List<Member> result = em.createQuery("select m from Member m", Member.class)
                .getResultList();
        List<MemberDto> dto = result.stream()
                .map(o -> new MemberDto(o.getUsername(), o.getAge()))
                .collect(Collectors.toList());
    }
    @Test
    void findDtoBySetter(){
        List<MemberDto> result = query
                .select(Projections.bean(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();
    }
    @Test
    void findDtoByField(){
        List<MemberDto> result = query
                .select(Projections.fields(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();
    }
    @Test
    void findDtoByConstructor(){
        List<MemberDto> result = query
                .select(Projections.constructor(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();
    }
    @Test
    void findUserDtd(){
        QMember memberSub = new QMember("memberSub");
        List<UserDto> result = query
                .select(Projections.fields(UserDto.class,
                        member.username.as("name"),//이 방법 추천
                        Expressions.as(JPAExpressions.
                                select(memberSub.age.max())//이 방법 별로
                        .from(memberSub),"age")))
                .from(member)
                .fetch();
    }
    @Test
    void findUserDtoByConstructor(){
        List<UserDto> result = query
                .select(Projections.constructor(UserDto.class,
                        member.username,//생성자는 변수명이 달라도 괜찮다. 단 setter과 field는 필요하다
                        member.age))
                .from(member)
                .fetch();
    }
    @Test
    void findDtoByQueryProjection(){
        List<MemberDto> result = query
                .select(new QMemberDto(member.username, member.age))
                .from(member)
                .fetch();
    }
    @Test
    void dynamicQuery_BooleanBuilder(){
        String usernameParam= "member1";
        Integer ageParam= 10;
//        Integer ageParam= 10;


        List<Member> result = searchMember1(usernameParam, ageParam);
        assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember1(String usernameCond, Integer ageCond) {
        BooleanBuilder builder = new BooleanBuilder(member.username.eq(usernameCond));
        if(usernameCond !=null){
            builder.and(member.username.eq(usernameCond));
        }
        if(ageCond!=null){
            builder.and(member.age.eq(ageCond));
        }


        return query
                .selectFrom(member)
                .where(builder)
                .fetch();
    }
    @Test
    void dynamicQuery_WhereParam(){
        String usernameParam= "member1";
        Integer ageParam= 10;
//        Integer ageParam= 10;


        List<Member> result = searchMember2(usernameParam, ageParam);
        assertThat(result.size()).isEqualTo(1);

    }

    private List<Member> searchMember2(String usernameCond, Integer ageCond) {

        return query
                .selectFrom(member)
                //.where(usernameEq(usernameCond), ageEq(ageCond))
                .where(allEq(usernameCond,ageCond))
                .fetch();

    }

    private BooleanExpression usernameEq(String usernameCond) {
        if (usernameCond == null) {
            return null;
        }
        else {
            return member.username.eq(usernameCond);
        }
    }
    private BooleanExpression ageEq(Integer ageCond) {
        if(ageCond!=null){
            return member.age.eq(ageCond);
        }
        else{
            return null;
        }
    }

    //광고상태 isValid 날짜가 IN: canService
    private BooleanExpression allEq(String usernameCond, Integer ageCond){
        return usernameEq(usernameCond).and(ageEq(ageCond));
    }
    @Test
    void bulkUpdate(){
        //member1 = 10 -> 비회원
        //member2 = 20 -> 비회원
        //member3 = 30 -> 유지
        //member4 = 40 -> 유지

        long count = query
                .update(member)
                .set(member.username, "비회원")
                .where(member.age.lt(28))
                .execute();
        em.flush();
        em.clear();
    }
    @Test
    void bulkAdd(){
        long count = query
                .update(member)
                .set(member.age, member.age.add(1))
                .execute();
    }
    @Test
    void bulkDelete(){
        query
                .delete(member)
                .where(member.age.gt(18))
                .execute();
    }
    @Test
    void sqlFunction(){
        List<String> result = query
                .select(Expressions.stringTemplate(
                        "function('replace',{0},{1},{2})"
                        , member.username, "member", "M"))
                .from(member)
                .fetch();
    }
    @Test
    void wqlFunction2(){
        List<String> result = query
                .select(member.username)
                .from(member)
                //.where(member.username.eq(
                        //Expressions.stringTemplate(
                         //       "function('lower', {0})", member.username)))
                .where(member.username.eq(member.username.lower()))
                .fetch();
    }

}
