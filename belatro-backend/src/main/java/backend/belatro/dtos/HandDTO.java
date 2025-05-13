package backend.belatro.dtos;

import java.util.List;

public record HandDTO(
        int handNo,
        List<TrickDTO> tricks) {}