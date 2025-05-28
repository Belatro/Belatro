package backend.belatro.dtos;

public record KickPlayerRequestDTO(
         String lobbyId,
         String usernameToKick,
         String requesterUsername) {
}
