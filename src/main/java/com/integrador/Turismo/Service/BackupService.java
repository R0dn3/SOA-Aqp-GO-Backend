package com.integrador.Turismo.Service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class BackupService {

    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    @Value("${spring.datasource.username}")
    private String dbUsername;

    @Value("${spring.datasource.password}")
    private String dbPassword;


    // Genera el backup y devuelve los bytes del archivo .sql
    public byte[] generarBackup() throws Exception {

        // Parsea la URL jdbc para obtener host, port y dbname
        // Formato: jdbc:postgresql://host:port/dbname
        String urlSinJdbc = datasourceUrl.replace("jdbc:postgresql://", "");
        String[] partes   = urlSinJdbc.split("/");
        String hostPort   = partes[0];
        String dbName     = partes[1].split("\\?")[0]; // quita query params si los hay

        String host = hostPort.contains(":") ? hostPort.split(":")[0] : hostPort;
        String port = hostPort.contains(":") ? hostPort.split(":")[1] : "5432";

        log.info("Generando backup de BD: {}:{}/{}", host, port, dbName);

        // Construye el comando pg_dump
        List<String> comando = new ArrayList<>();
        comando.add("pg_dump");
        comando.add("-h"); comando.add(host);
        comando.add("-p"); comando.add(port);
        comando.add("-U"); comando.add(dbUsername);
        comando.add("-d"); comando.add(dbName);
        comando.add("-F"); comando.add("p");   // formato plain SQL
        comando.add("--no-password");

        ProcessBuilder pb = new ProcessBuilder(comando);
        pb.environment().put("PGPASSWORD", dbPassword); // pasa el password sin prompt
        pb.redirectErrorStream(false);

        Process proceso = pb.start();

        // Lee el output del proceso (el SQL generado)
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (InputStream is = proceso.getInputStream()) {
            byte[] buffer = new byte[4096];
            int bytesLeidos;
            while ((bytesLeidos = is.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesLeidos);
            }
        }

        // Lee posibles errores
        String errores = new String(proceso.getErrorStream().readAllBytes());
        int exitCode = proceso.waitFor();

        if (exitCode != 0) {
            log.error("pg_dump falló con código {}: {}", exitCode, errores);
            throw new RuntimeException("Error al generar backup: " + errores);
        }

        log.info("Backup generado exitosamente ({} bytes)", baos.size());
        return baos.toByteArray();
    }

    // Nombre del archivo con timestamp
    public String generarNombreArchivo() {
        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        return "aqpgo_backup_" + timestamp + ".sql";
    }
}
