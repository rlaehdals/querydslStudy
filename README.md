## querydslStudy

## spring setting
1. spring boot version 2.4.4
2. java 8
3. querydsl version 1.0.10

## dependecy
1. web
2. jpa
3. lombok
4. h2
6. querydsl

## query설정

```
def querydslDir = "$buildDir/generated/querydsl"


querydsl{
  jpa=true
  querydslSourceDir = queryDir
}

sourceSets{
  main.java.srcDir querydslDir
}
configurations{
  querydsl.extendsFrom compileClassPath
}
compileQuerydsl{
  options.annotationProcessorPayh = configurations.querydsl
}
```
> gradle -> tasks -> other -> javaCompile을 실행하여 Q클래스가 생기는지 확인한다.

1. querydsl 기본 사용법
```
JPAQueryFactory query;

QMember m = new QMember("m");
Member findMember = query.select(m)
                         .from(m)
                         .where(m.useranme.eq("member1")
                         .fetchOne();
```
> Q타입을 static 선언하면 m->member로 바꿔서 사용가능

2. 기본 제공 하는 것들
  * eq  -> 같은것
  * ne  -> 같지 않은 것
  * eq.not -> 같지 않은 것
  * isNotNull -> null이 아닌 것
  * in(10, 20) -> 10, 20 인 것
  * notIn(10, 20) -> 10, 20이 아닌 거
  * between(10, 30) -> 10~30인 것
  * goe(30) -> 30보다 크거나 같은 것
  * gt(30) -> 30보다 큰 것
  * loe(30) -> 30보다 작거나 같은 거
  * lt(30) -> 30보다 작은 것
  * like("member%") -> like 검색
  * contains("member") -> %member% like 검색과 같은 것
  * startsWith("member") -> member% like와 같음
  * ,는 and와 같은 역할
3. 정렬
```
query.selectFrom(member)
     .where(member.age.eq(10))
     .orderBy(member.age.desc(), member.username.asc().nullLast()) // 나이는 내림차순 이름은 오름차순이고 이름이 null인것은 가장 마지막 
     
     .fetch();
```
4. 집합 함수
  * count
  * sum
  * avg
  * max
  * min
5. groupBy
```
query.select(team.name, member.age.avg())
     .from(member)
     .join(member.team, team)
     .groupBy(team.name)
     fetch();
```
6. fetch join
```
query.selectFrom(member)
     .join(member.team, team).fetchJoin()
     .where(member.username.eq("member1"))
     .fetch();
```
7. case 문
```
 queryFactory.select(member.age
                  .when(10).then("열살")
                  .when(20).then("스무살")
                  .otherwise("기타"))
             .from(member)
             .fetch();
```
8. 상수 문자 더하기
```
 queryFactory.select(member.username.concat("_").concat(member.age.stringValue()))
             .from(member)
             .where(member.username.eq("member1"))
             .fetchOne();
```
9. 프로젝션
```
@Data
public class MemberDto{
  private String username;
  private int age;
  
  @QueryProjection
  public MemberDto(String username, int age){
    this.username=username;
    this.age=age;
  }
}
query.select(new QMemberDto(member.username, member.age))
     .from(member)
     .fetch();
```

10. 동적 쿼리 where 다중 파라미터 이용법
```
query.selectFrom(member)
     .where(usernameEq(usernameCond), ageEq(ageCond))
     .fetch();
     
private BooleanExpression usernameEq(String usernameCond){
  return usernameCond != null ? member.username.eq(usernameCond) : null;
}
private BooleanExpression ageEq(int ageCond){
  return ageCond != null ? member.age.eq(ageCond) : null;
}
```
> 파라미터가 null 경우 그 조건은 그냥 무시된다.

11. 벌크 연산
```
query.update(member)
     .set(member.age, member.age.add(1))
     .execute();
```








