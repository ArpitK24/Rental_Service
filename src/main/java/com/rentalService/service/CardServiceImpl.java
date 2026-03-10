package com.rentalService.service;

import com.rentalService.model.Card;
import com.rentalService.repository.CardRepository;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.*;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class CardServiceImpl implements CardService {

    private final CardRepository cards;
    private Path storageDir;

    // constructor injection (no IO in constructor)
    public CardServiceImpl(CardRepository cards, @Value("${card.storage.path}") String storagePath) {
        this.cards = cards;
        this.storageDir = Paths.get(storagePath); // don't create directories here
    }

    // initialize storage dir after bean creation; avoids throwing checked exceptions in constructor
    @PostConstruct
    public void initStorage() throws IOException {
        Files.createDirectories(storageDir);
    }

    @Override
    public Card uploadCard(String title, String description, MultipartFile file) throws IOException {
        String ext = getExtension(file.getOriginalFilename());
        String filename = UUID.randomUUID() + "." + ext;
        Path target = storageDir.resolve(filename);

        // save file
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        // get image dimensions
        BufferedImage img = ImageIO.read(target.toFile());
        int width = img.getWidth();
        int height = img.getHeight();

        Card card = new Card(
                title,
                description,
                "/cards/" + filename,
                width,
                height,
                OffsetDateTime.now()
        );
        return cards.save(card);
    }

    @Override
    public List<Card> listCards() {
        return cards.findAll();
    }

    @Override
    public void deleteCard(UUID id) throws IOException {
        Card card = cards.findById(id)
                .orElseThrow(new java.util.function.Supplier<RuntimeException>() {
                    @Override
                    public RuntimeException get() {
                        return new RuntimeException("Card not found: " + id);
                    }
                });

        Path file = storageDir.resolve(Paths.get(card.getImageUrl()).getFileName());
        Files.deleteIfExists(file);
        cards.delete(card);
    }


    private String getExtension(String filename) {
        int dot = filename == null ? -1 : filename.lastIndexOf('.');
        return (dot == -1) ? "png" : filename.substring(dot + 1);
    }
}
