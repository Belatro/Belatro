package backend.belatro.repos;

import backend.belatro.models.SkinInventory;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface SkinInventoryRepo extends MongoRepository<SkinInventory,String> {
}
