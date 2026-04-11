package com.mundial;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.HttpServletResponse;
import java.io.*;
import java.lang.reflect.Type;
import java.net.URL;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

@SpringBootApplication(exclude = {
    org.springframework.boot.autoconfigure.r2dbc.R2dbcAutoConfiguration.class,
    org.springframework.boot.autoconfigure.data.r2dbc.R2dbcDataAutoConfiguration.class
})
@RestController
public class GeneradorCalendarioOrquesta {

    private static final ZoneId ZONA_MONTRAL = ZoneId.of("America/Toronto");
    private static final String SHEET_ID = "1qLNQ8sFIY68WNv0PAVaeQ0b0CoTa3w_ozVsNXXL0m3g";

    public static void main(String[] args) {
        SpringApplication.run(GeneradorCalendarioOrquesta.class, args);
    }

    @GetMapping("/calendario")
    public void getCalendario(HttpServletResponse response) throws Exception {
        response.setContentType("text/calendar; charset=UTF-8");
        response.setHeader("Content-Disposition", "inline; filename=Mundial_2026_Colombia.ics");

        Gson gson = new Gson();
        InputStream is = getClass().getClassLoader().getResourceAsStream("partidos.json");
        Map<String, List<Partido>> data = gson.fromJson(
            new InputStreamReader(is),
            new TypeToken<Map<String, List<Partido>>>() {}.getType()
        );

        List<Partido> partidos = data.get("partidosGrupos");
        Map<String, Resultado> resultados = cargarResultadosDesdeSheets();

        StringBuilder ics = new StringBuilder();
        ics.append("BEGIN:VCALENDAR\n");
        ics.append("VERSION:2.0\n");
        ics.append("PRODID:-//Mundial2026 Orquesta//ES\n");
        ics.append("CALSCALE:GREGORIAN\n");
        ics.append("REFRESH-INTERVAL;VALUE=DURATION:PT1H\n");
        ics.append("X-PUBLISHED-TTL:PT1H\n\n");

        for (Partido p : partidos) {
            agregarEvento(ics, p, resultados.get(p.id));
        }

        ics.append("END:VCALENDAR");
        response.getWriter().write(ics.toString());
    }

    private Map<String, Resultado> cargarResultadosDesdeSheets() {
        Map<String, Resultado> map = new HashMap<>();
        try {
            String csvUrl = "https://docs.google.com/spreadsheets/d/" + SHEET_ID + "/export?format=csv";
            String csvContent = new String(new URL(csvUrl).openStream().readAllBytes());
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
            System.out.println("No se pudo leer la Google Sheet.");
        }
        return map;
    }

    private void agregarEvento(StringBuilder ics, Partido p, Resultado res) {
        LocalDate fecha = LocalDate.parse(p.fecha);
        LocalTime hora = LocalTime.parse(p.horaEDT);
        ZonedDateTime inicio = ZonedDateTime.of(fecha, hora, ZONA_MONTRAL);
        ZonedDateTime fin = inicio.plusHours(2);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");

        String titulo = res != null
            ? p.flagLocal + " " + p.local + " " + res.golesLocal + " - " + res.golesVisitante + " " + p.flagVisitante + " " + p.visitante
            : p.flagLocal + " " + p.local + " vs " + p.flagVisitante + " " + p.visitante;

        ics.append("BEGIN:VEVENT\n");
        ics.append("UID:mundial2026-").append(p.id).append("@xai\n");
        ics.append("DTSTART:").append(inicio.format(fmt)).append("\n");
        ics.append("DTEND:").append(fin.format(fmt)).append("\n");
        ics.append("SUMMARY:").append(titulo).append("\n");
        ics.append("LOCATION:").append(p.estadio).append("\n");
        ics.append("DESCRIPTION:Grupo ").append(p.grupo)
           .append("\\nEstadio: ").append(p.estadio)
           .append("\\n").append(p.notas).append("\n");
        ics.append("END:VEVENT\n\n");
    }

    static class Partido {
        String id, fecha, horaEDT, grupo, local, flagLocal, visitante, flagVisitante, estadio, notas;
    }

    static class Resultado {
        int golesLocal, golesVisitante;
        Resultado(int l, int v) { this.golesLocal = l; this.golesVisitante = v; }
    }
}