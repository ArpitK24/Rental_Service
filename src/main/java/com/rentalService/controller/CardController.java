package com.rentalService.controller;

import com.rentalService.model.Card;
import com.rentalService.service.CardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/cards")
public class CardController {

    private final CardService cardService;

    public CardController(CardService cardService) {
        this.cardService = cardService;
    }

    @PostMapping
    public ResponseEntity<Card> uploadCard(
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam MultipartFile file) throws IOException {
        return ResponseEntity.ok(cardService.uploadCard(title, description, file));
    }

    @GetMapping
    public ResponseEntity<List<Card>> listCards() {
        return ResponseEntity.ok(cardService.listCards());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCard(@PathVariable UUID id) throws IOException {
        cardService.deleteCard(id);
        return ResponseEntity.noContent().build();
    }
}
