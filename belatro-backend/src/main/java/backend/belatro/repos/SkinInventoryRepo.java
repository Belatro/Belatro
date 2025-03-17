package backend.belatro.repos;

import backend.belatro.models.SkinInventory;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SkinInventoryRepo extends MongoRepository<SkinInventory,String> {
}
