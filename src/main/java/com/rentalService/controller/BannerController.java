package com.rentalService.controller;

import com.rentalService.model.Banner;
import com.rentalService.service.BannerService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/banners")
public class BannerController {
    private final BannerService bannerService;

    public BannerController(BannerService bannerService) {
        this.bannerService = bannerService;
    }

    @PostMapping("/upload")
    public Banner uploadBanner(@RequestParam String title,
                               @RequestParam MultipartFile image) throws IOException {
        return bannerService.uploadBanner(title, image);
    }

    @GetMapping
    public List<Banner> getAllBanners() {
        return bannerService.getAllBanners();
    }

    @GetMapping("/files/{filename}")
    public ResponseEntity<FileSystemResource> serveBanner(@PathVariable String filename) {
        File file = bannerService.getBannerFile(filename);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .cacheControl(CacheControl.maxAge(30, TimeUnit.DAYS)) // cache optimization
                .body(new FileSystemResource(file));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBanner(@PathVariable UUID id) throws IOException {
        bannerService.deleteBanner(id);
        return ResponseEntity.noContent().build();
    }
}
