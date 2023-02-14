package it.ibee.scraperimmo.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ComuneImmo {

    private String link;
    private Boolean processed;
    private Date startProcessing;
    private Date endProcessing;
    private List<SingleAd> singleAds;

    public ComuneImmo() {
        this.singleAds = new ArrayList<>();
        this.processed = false;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public List<SingleAd> getSingleAds() {
        return singleAds;
    }

    public void setSingleAds(List<SingleAd> singleAds) {
        this.singleAds = singleAds;
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
