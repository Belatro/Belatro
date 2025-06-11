package backend.belatro.dtos;

public record UserDto(
        String id,
        String username,
        String email,
        java.util.Set<backend.belatro.enums.Role> roles,
        boolean deletionRequested
) {}