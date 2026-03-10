package com.rentalService.service;

import com.rentalService.model.Card;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public interface CardService {
    Card uploadCard(String title, String description, MultipartFile file) throws IOException;
    List<Card> listCards();
    void deleteCard(UUID id) throws IOException;
}
