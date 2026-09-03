package com.integrador.Turismo.Controller;

import com.integrador.Turismo.Service.BackupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/backup")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Slf4j
public class BackupController {
    private final BackupService backupService;

    // GET /api/admin/backup/descargar
    @GetMapping("/descargar")
    public ResponseEntity<byte[]> descargar() {
        try {
            byte[] backup   = backupService.generarBackup();
            String filename = backupService.generarNombreArchivo();

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .contentLength(backup.length)
                    .body(backup);

        } catch (Exception e) {
            log.error("Error generando backup: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
}
