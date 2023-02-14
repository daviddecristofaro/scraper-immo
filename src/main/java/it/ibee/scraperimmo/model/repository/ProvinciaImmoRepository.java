package it.ibee.scraperimmo.model.repository;

import it.ibee.scraperimmo.model.ProvinciaImmo;

import java.util.List;

public interface ProvinciaImmoRepository extends AbstractRepository<ProvinciaImmo> {

    public ProvinciaImmo findFirstByLink(String link);

    public List<ProvinciaImmo> findAllByProcessed(Boolean proccessed);

}
