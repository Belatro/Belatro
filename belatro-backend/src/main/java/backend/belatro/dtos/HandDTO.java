package backend.belatro.dtos;

import java.util.List;


/**
 * A hand consists of
 *  • its ordinal number,
 *  • all trump bids called during the hand,
 *  • the tricks that were actually played.
 */
public record HandDTO(int handNo,
                      List<TrumpCallDTO> trumpCalls,
                      List<TrickDTO> tricks) {}
