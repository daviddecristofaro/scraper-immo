package it.ibee.scraperimmo.model;

import org.springframework.data.annotation.Id;

import java.io.Serializable;

public class AbstractEntity implements Serializable {

    @Id
    private String id;

    /**
     *
     */
    public AbstractEntity() {
        super();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

}