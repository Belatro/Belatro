package backend.belatro.models;


import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Data
@Document(collation = "skinInventory")
public class SkinInventory {
    @Id
    private String id;
    @DBRef
    private User user;
    private String skinId;
    private String skinName;
    private Date acquiredDate;
}
