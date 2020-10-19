package de.tum.in.www1.artemis.domain.lecture_module;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Lob;

import com.fasterxml.jackson.annotation.JsonInclude;

@Entity
@DiscriminatorValue("V")
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class VideoModule extends LectureModule {

    @Column(name = "description")
    @Lob
    private String description;

    @Column(name = "source")
    @Lob
    private String source;

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}