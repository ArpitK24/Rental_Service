package com.rentalService.service;

import com.rentalService.model.Banner;
import com.rentalService.repository.BannerRepository;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class BannerService {

    private final BannerRepository banners;
    private final Path storageDir = Paths.get("uploads", "banners"); // Java 8

    public BannerService(BannerRepository banners) throws IOException {
        this.banners = banners;
        Files.createDirectories(storageDir);
    }

    public Banner uploadBanner(String title, MultipartFile file) throws IOException {
        String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
        Path targetPath = storageDir.resolve(fileName);

        // Optimize image (resize max 1200px, compress 0.8 quality)
        Thumbnails.of(file.getInputStream())
                .size(1200, 600)
                .outputQuality(0.8)
                .toFile(targetPath.toFile());

        Banner banner = new Banner();
        banner.setTitle(title);
        banner.setImageUrl("/api/banners/files/" + fileName);
        banner.setCreatedAt(OffsetDateTime.now());

        // get optimized dimensions
        java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(targetPath.toFile());
        banner.setWidth(img.getWidth());
        banner.setHeight(img.getHeight());

        return banners.save(banner);
    }

    public List<Banner> getAllBanners() {
        return banners.findAll();
    }

    public File getBannerFile(String filename) {
        return storageDir.resolve(filename).toFile();
    }

    public void deleteBanner(UUID id) throws IOException {
        Banner banner = banners.findById(id)
                .orElseThrow(new java.util.function.Supplier<RuntimeException>() {
                    @Override
                    public RuntimeException get() {
                        return new RuntimeException("Banner not found: " + id);
                    }
                });

        String filename = banner.getImageUrl() == null ? null :
                banner.getImageUrl().substring(banner.getImageUrl().lastIndexOf('/') + 1);
        if (filename != null && !filename.isEmpty()) {
            Path file = storageDir.resolve(filename);
            Files.deleteIfExists(file);
        }
        banners.delete(banner);
    }
}
