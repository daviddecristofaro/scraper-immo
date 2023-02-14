package it.ibee.scraperimmo.model.repository;

import it.ibee.scraperimmo.model.AbstractEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AbstractRepository<T extends AbstractEntity> extends MongoRepository<T, String> {
}
