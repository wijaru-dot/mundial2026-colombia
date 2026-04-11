package com.mundial;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.r2dbc.R2dbcAutoConfiguration;
import org.springframework.boot.autoconfigure.data.r2dbc.R2dbcDataAutoConfiguration;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URL;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@SpringBootApplication(exclude = {
    R2dbcAutoConfiguration.class,
    R2dbcDataAutoConfiguration.class
})
@RestController
public class GeneradorCalendarioOrquesta {

    private static final ZoneId ZONA_ET = ZoneId.of("America/Toronto");
    private static final String SHEET_ID = "1qLNQ8sFIY68WNv0PAVaeQ0b0CoTa3w_ozVsNXXL0m3g";

    public static void main(String[] args) {
        SpringApplication.run(GeneradorCalendarioOrquesta.class, args);
    }

    @GetMapping("/calendario")
    public void getCalendario(HttpServletResponse response) throws Exception {
        response.setContentType("text/calendar; charset=UTF-8");
        response.setHeader("Content-Disposition", "inline; filename=Mundial_2026.ics");

        Gson gson = new Gson();
        InputStream is = getClass().getClassLoader().getResourceAsStream("partidos.json");
        Map<String, List<Partido>> data = gson.fromJson(
            new InputStreamReader(is, "UTF-8"),
            new TypeToken<Map<String, List<Partido>>>() {}.getType()
        );

        List<Partido> partidos = data.get("partidosGrupos");
        Map<String, Resultado> resultados = cargarResultadosDesdeSheets();

        StringBuilder ics = new StringBuilder();
        ics.append("BEGIN:VCALENDAR\r\n");
        ics.append("VERSION:2.0\r\n");
        ics.append("PRODID:-//Mundial2026//ES\r\n");
        ics.append("CALSCALE:GREGORIAN\r\n");
        ics.append("METHOD:PUBLISH\r\n");
        ics.append("X-WR-CALNAME:⚽ Mundial FIFA 2026\r\n");
        ics.append("REFRESH-INTERVAL;VALUE=DURATION:PT1H\r\n");
        ics.append("X-PUBLISHED-TTL:PT1H\r\n");

        for (Partido p : partidos) {
            agregarEvento(ics, p, resultados.get(p.id));
        }

        ics.append("END:VCALENDAR\r\n");
        response.getWriter().write(ics.toString());
    }

    private Map<String, Resultado> cargarResultadosDesdeSheets() {
        Map<String, Resultado> map = new HashMap<>();
        try {
            String csvUrl = "https://docs.google.com/spreadsheets/d/" + SHEET_ID + "/export?format=csv";
            String csvContent = new String(new URL(csvUrl).openStream().readAllBytes(), "UTF-8");
            String[] lines = csvContent.split("\n");
            for (int i = 1; i < lines.length; i++) {
                String[] parts = lines[i].split(",");
                if (parts.length >= 3) {
                    String id = parts[0].trim();
                    String golesL = parts[1].trim();
                    String golesV = parts[2].trim();
                    if (!golesL.isEmpty() && !golesV.isEmpty()) {
                        try {
                            map.put(id, new Resultado(Integer.parseInt(golesL), Integer.parseInt(golesV)));
                        } catch (Exception ignored) {}
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("No se pudo leer Google Sheets: " + e.getMessage());
        }
        return map;
    }

    private void agregarEvento(StringBuilder ics, Partido p, Resultado res) {
        LocalDate fecha = LocalDate.parse(p.fecha);
        LocalTime hora = LocalTime.parse(p.horaEDT);
        ZonedDateTime inicio = ZonedDateTime.of(fecha, hora, ZONA_ET);
        ZonedDateTime fin = inicio.plusHours(2);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");

        boolean esColombia = p.notas != null && p.notas.contains("🇨🇴");

        String titulo;
        if (res != null) {
            titulo = p.flagLocal + " " + p.local + " " + res.golesLocal + " - " + res.golesVisitante + " " + p.flagVisitante + " " + p.visitante;
        } else {
            titulo = p.flagLocal + " " + p.local + " vs " + p.flagVisitante + " " + p.visitante;
        }

        if (esColombia) {
            titulo = "🇨🇴 " + titulo;
        }

        String descripcion = "📌 Grupo " + p.grupo
            + "\\n🏟️ " + p.estadio
            + "\\n📺 Canal: " + (p.canal != null ? p.canal : "TBD")
            + "\\n" + (p.notas != null ? p.notas : "");

        ics.append("BEGIN:VEVENT\r\n");
        ics.append("UID:mundial2026-").append(p.id).append("@mundial\r\n");
        ics.append("DTSTART;TZID=America/Toronto:").append(inicio.format(fmt)).append("\r\n");
        ics.append("DTEND;TZID=America/Toronto:").append(fin.format(fmt)).append("\r\n");
        ics.append("SUMMARY:").append(titulo).append("\r\n");
        ics.append("LOCATION:").append(p.estadio).append("\r\n");
        ics.append("DESCRIPTION:").append(descripcion).append("\r\n");
        if (esColombia) {
            ics.append("CATEGORIES:Colombia\r\n");
        }
        ics.append("CATEGORIES:Grupo ").append(p.grupo).append("\r\n");
        ics.append("BEGIN:VALARM\r\n");
        ics.append("TRIGGER:-PT60M\r\n");
        ics.append("ACTION:DISPLAY\r\n");
        ics.append("DESCRIPTION:⚽ En 1 hora: ").append(titulo).append("\r\n");
        ics.append("END:VALARM\r\n");
        ics.append("END:VEVENT\r\n");
    }

    static class Partido {
        String id, fecha, horaEDT, grupo, local, flagLocal, visitante, flagVisitante, estadio, canal, notas;
    }

    static class Resultado {
        int golesLocal, golesVisitante;
        Resultado(int l, int v) { this.golesLocal = l; this.golesVisitante = v; }
    }
}