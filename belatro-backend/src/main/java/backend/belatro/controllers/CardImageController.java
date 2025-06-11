package backend.belatro.controllers;

import backend.belatro.services.CardImageService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/api/cards")
public class CardImageController {

    private final CardImageService imageService;

    public CardImageController(CardImageService imageService) {
        this.imageService = imageService;
    }

    @GetMapping("/{cardKey}/image")
    public ResponseEntity<byte[]> getCardImage(@PathVariable String cardKey) throws IOException {
        byte[] imageData = imageService.getImageBytes(cardKey);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(imageData);
    }
}
