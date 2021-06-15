package inflearn.querydsl.entity;

import lombok.*;

import javax.persistence.*;

@Entity
@Getter @Setter
// JPA 는 기본 생성자가 있어야 한다. - Lombok 은 PROTECTED 까지 허용
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(exclude = {"team"}) // 연관관계의 필드는 제외 - 무한 루프 방지
public class Member {

    @Id @GeneratedValue
    @Column(name = "member_id")
    private Long id;

    private String username;

    private int age;

    // 연관관계의 주인
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    public Member(String username) {
        this(username, 0);
    }

    public Member(String username, int age) {
        this(username, age, null);
    }

    public Member(String username, int age, Team team) {
        this.username = username;
        this.age = age;
        if (team != null) {
            changeTeam(team);
        }
    }

    public void changeTeam(Team team) {
        this.team = team;
        team.getMembers().add(this);
    }
}


