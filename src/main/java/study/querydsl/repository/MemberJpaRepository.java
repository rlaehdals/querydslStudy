package study.querydsl.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.dto.QMemberTeamDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.List;
import java.util.Optional;

import static org.springframework.util.StringUtils.*;
import static study.querydsl.entity.QMember.*;
import static study.querydsl.entity.QTeam.team;

@Repository
@RequiredArgsConstructor
public class MemberJpaRepository {

    private final EntityManager em;
    private final JPAQueryFactory query;

    public void save(Member member){
        em.persist(member);
    }
    public Optional<Member> findById(Long id){
        return Optional.ofNullable(em.find(Member.class, id));
    }
    public List<Member> findAll(){
         return em.createQuery("select m from Member m", Member.class)
                .getResultList();
    }
    public List<Member> findAll_Querydsl(){
        return query
                .selectFrom(member)
                .fetch();
    }
    public List<Member> findByUsername(String username){
        return em.createQuery("select m from Member m where m.username= :username", Member.class)
                .setParameter("username", username)
                .getResultList();
    }
    public List<Member> findByUsername_Querydsl(String username){
        return query
                .selectFrom(member)
                .where(member.username.eq((username)))
                .fetch();
    }

    public List<MemberTeamDto> searchByBuilder(MemberSearchCondition memberSearchCondition){

        BooleanBuilder builder = new BooleanBuilder();

        if(hasText(memberSearchCondition.getUsername())){
           builder.and(member.username.eq(memberSearchCondition.getUsername()));
        }
        if (hasText(memberSearchCondition.getTeamName())){
            builder.and(team.name.eq(memberSearchCondition.getTeamName()));
        }
        if(memberSearchCondition.getAgeGoe() != null){
            builder.and(member.age.goe(memberSearchCondition.getAgeGoe()));
        }
        if(memberSearchCondition.getAgeLoe() != null){
            builder.and(member.age.loe(memberSearchCondition.getAgeLoe()));
        }

        return query
                .select(new QMemberTeamDto(
                        member.id.as("memberId"),
                        member.username,
                        member.age,
                        team.id.as("teamId"),
                        team.name.as("teamName")
                ))
                .from(member)
                .leftJoin(member.team, team)
                .where(builder)
                .fetch();
    }
    public List<MemberTeamDto> search(MemberSearchCondition condition){
        return query
                .select(new QMemberTeamDto(
                        member.id.as("memberId"),
                        member.username,
                        member.age,
                        team.id.as("teamId"),
                        team.name.as("teamName")
                ))
                .from(member)
                .leftJoin(member.team, team)
                .where(usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe()))
                .fetch();
    }

    private BooleanExpression usernameEq(String username) {
        if(hasText(username)){
            return member.username.eq(username);

        }
        return null;
    }

    private BooleanExpression teamNameEq(String teamName) {
        return hasText(teamName)?team.name.eq(teamName):null;
    }

    private BooleanExpression ageGoe(Integer ageGoe) {
        return ageGoe!=null?member.age.goe(ageGoe):null;
    }

    private BooleanExpression ageLoe(Integer ageLoe) {
        return ageLoe!=null? member.age.loe(ageLoe):null;
    }
}
