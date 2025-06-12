package backend.belatro.dtos;

import java.io.Serializable;

/**
 * DTO for {@link LobbyDTO.UserSimpleDTO}
 */
public record UserSimpleDTODto(String username) implements Serializable {
}