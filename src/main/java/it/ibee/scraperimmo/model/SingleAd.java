package it.ibee.scraperimmo.model;

import java.util.Date;

public class SingleAd {

    private String link;
    private Boolean processed;
    private Date startProcessing;
    private Date endProcessing;
    private Boolean expired;

    public SingleAd() {
        this.processed = false;
        this.expired = true;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
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

    public Boolean getExpired() {
        return expired;
    }

    public void setExpired(Boolean expired) {
        this.expired = expired;
    }
}
