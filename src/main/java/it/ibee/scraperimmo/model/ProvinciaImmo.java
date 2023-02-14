package it.ibee.scraperimmo.model;

import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Document(collection = "provinciaImmo")
public class ProvinciaImmo extends AbstractEntity {

    private String link;
    private Boolean processed;
    private Date startProcessing;
    private Date endProcessing;
    private List<ComuneImmo> comuneImmos;

    @PersistenceConstructor
    public ProvinciaImmo() {
        this.comuneImmos = new ArrayList<>();
        this.processed = false;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public List<ComuneImmo> getComuneImmos() {
        return comuneImmos;
    }

    public void setComuneImmos(List<ComuneImmo> comuneImmos) {
        this.comuneImmos = comuneImmos;
    }

    public Boolean getProcessed() {
        return processed;
    }

    public void setProcessed(Boolean processed) {
        this.processed = processed;
    }

    public Date getStartProcessing() {
        return startProcessing;
    }

    public void setStartProcessing(Date startProcessing) {
        this.startProcessing = startProcessing;
    }

    public Date getEndProcessing() {
        return endProcessing;
    }

    public void setEndProcessing(Date endProcessing) {
        this.endProcessing = endProcessing;
    }
}
