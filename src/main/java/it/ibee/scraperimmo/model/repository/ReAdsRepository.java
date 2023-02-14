package it.ibee.scraperimmo.model.repository;

import it.ibee.scraperimmo.model.ReAds;

public interface ReAdsRepository extends AbstractRepository<ReAds> {

    public void deleteByLink(String link);

}
