package backend.belatro.exceptions;

/**
 * Thrown when a requested domain resource cannot be found.
 */
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}