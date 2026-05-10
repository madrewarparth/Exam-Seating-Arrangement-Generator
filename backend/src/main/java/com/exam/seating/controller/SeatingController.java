package com.exam.seating.controller;

import com.exam.seating.model.SeatingPlan;
import com.exam.seating.service.SeatingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*") // For local development simplicity
public class SeatingController {

    @Autowired
    private SeatingService seatingService;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadStudents(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("Please select a CSV file to upload.");
        }
        try {
            seatingService.processCsv(file);
            return ResponseEntity.ok("File uploaded and processed successfully.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to process CSV file: " + e.getMessage());
        }
    }

    @GetMapping("/generate")
    public ResponseEntity<?> generatePlan(@RequestParam(value = "examDate", defaultValue = "2024-01-01") String examDate) {
        try {
            List<SeatingPlan> plan = seatingService.generatePlan(examDate);
            return ResponseEntity.ok(plan);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error generating seating plan: " + e.getMessage());
        }
    }

    @GetMapping("/export/pdf")
    public ResponseEntity<Resource> exportPdf(@RequestParam(value = "examDate", defaultValue = "2024-01-01") String examDate) {
        try {
            File file = seatingService.exportPdf(examDate);
            Resource resource = new FileSystemResource(file);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getName() + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .contentLength(file.length())
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/rooms")
    public ResponseEntity<List<String>> getRooms() {
        return ResponseEntity.ok(seatingService.getRooms());
    }
}
