package de.tum.in.www1.artemis.domain;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import javax.persistence.*;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import de.tum.in.www1.artemis.domain.enumeration.AchievementRank;
import de.tum.in.www1.artemis.domain.enumeration.AchievementType;

@Entity
@Table(name = "achievement")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class Achievement implements Serializable {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title")
    private String title;

    @Column(name = "description")
    private String description;

    @Column(name = "icon")
    private String icon;

    @Enumerated(EnumType.STRING)
    @Column(name = "rank")
    private AchievementRank rank;

    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private AchievementType type;

    @ManyToOne
    private Course course;

    @ManyToOne
    private Exercise exercise;

    @ManyToMany(mappedBy = "achievements")
    @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    @JsonIgnoreProperties({ "achievements" })
    private Set<User> users = new HashSet<>();

    public Achievement() {
    }

    public Achievement(String title, String description, String icon, AchievementRank rank, AchievementType type, Course course, Exercise exercise) {
        this.title = title;
        this.description = description;
        this.icon = icon;
        this.rank = rank;
        this.type = type;
        this.course = course;
        this.exercise = exercise;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public AchievementRank getRank() {
        return rank;
    }

    public void setRank(AchievementRank rank) {
        this.rank = rank;
    }

    public AchievementType getType() {
        return type;
    }

    public void setType(AchievementType type) {
        this.type = type;
    }

    public Set<User> getUsers() {
        return users;
    }

    public void setUsers(Set<User> users) {
        this.users = users;
    }

    public Course getCourse() {
        return course;
    }

    public void setCourse(Course course) {
        this.course = course;
    }

    public Exercise getExercise() {
        return exercise;
    }

    public void setExercise(Exercise exercise) {
        this.exercise = exercise;
    }

    @Override
    public String toString() {
        return "Achievement{" + "id=" + getId() + ", title='" + getTitle() + "'" + ", description='" + getDescription() + "'" + ", icon='" + getIcon() + "'" + ", rank='"
                + getRank() + "'" + ", type='" + getType() + "'" + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Achievement that = (Achievement) o;
        return id.equals(that.id) && title.equals(that.title) && description.equals(that.description) && icon.equals(that.icon) && rank.equals(that.rank) && type.equals(that.type)
                && course.equals(that.course);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, title, description, icon, rank, type, course, exercise);
    }
}