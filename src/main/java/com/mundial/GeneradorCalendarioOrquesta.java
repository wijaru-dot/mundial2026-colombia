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
        Map<String, Object> data = gson.fromJson(
            new InputStreamReader(is, "UTF-8"),
            new TypeToken<Map<String, Object>>() {}.getType()
        );

        List<Map<String, String>> grupos = (List<Map<String, String>>) data.get("partidosGrupos");
        List<Map<String, String>> eliminatorios = (List<Map<String, String>>) data.get("partidosEliminatorios");

        // Cargar resultados de grupos (pestaña 1)
        Map<String, Resultado> resultadosGrupos = cargarResultados(0);
        // Cargar equipos y resultados eliminatorios (pestaña 2)
        Map<String, PartidoElim> datosElim = cargarEliminatorios();

        StringBuilder ics = new StringBuilder();
        ics.append("BEGIN:VCALENDAR\r\n");
        ics.append("VERSION:2.0\r\n");
        ics.append("PRODID:-//Mundial2026//ES\r\n");
        ics.append("CALSCALE:GREGORIAN\r\n");
        ics.append("METHOD:PUBLISH\r\n");
        ics.append("X-WR-CALNAME:⚽ Mundial FIFA 2026\r\n");
        ics.append("REFRESH-INTERVAL;VALUE=DURATION:PT1H\r\n");
        ics.append("X-PUBLISHED-TTL:PT1H\r\n");

        // Partidos de grupos
        for (Map<String, String> p : grupos) {
            agregarEventoGrupo(ics, p, resultadosGrupos.get(p.get("id")));
        }

        // Partidos eliminatorios
        for (Map<String, String> p : eliminatorios) {
            agregarEventoEliminatorio(ics, p, datosElim.get(p.get("id")));
        }

        ics.append("END:VCALENDAR\r\n");
        response.getWriter().write(ics.toString());
    }

    // Lee la pestaña de grupos (gid=0)
    private Map<String, Resultado> cargarResultados(int gid) {
        Map<String, Resultado> map = new HashMap<>();
        try {
            String csvUrl = "https://docs.google.com/spreadsheets/d/" + SHEET_ID + "/export?format=csv&gid=" + gid;
            String csv = new String(new URL(csvUrl).openStream().readAllBytes(), "UTF-8");
            for (String line : csv.split("\n")) {
                String[] p = line.split(",");
                if (p.length >= 3 && !p[0].trim().equals("id")) {
                    try {
                        String golesL = p[1].trim();
                        String golesV = p[2].trim();
                        if (!golesL.isEmpty() && !golesV.isEmpty()) {
                            map.put(p[0].trim(), new Resultado(Integer.parseInt(golesL), Integer.parseInt(golesV)));
                        }
                    } catch (Exception ignored) {}
                }
            }
        } catch (Exception e) {
            System.out.println("No se pudo leer grupos: " + e.getMessage());
        }
        return map;
    }

    // Lee la pestaña de eliminatorios (gid=1)
    // Columnas: id | equipoLocal | flagLocal | equipoVisitante | flagVisitante | golesLocal | golesVisitante
    private Map<String, PartidoElim> cargarEliminatorios() {
        Map<String, PartidoElim> map = new HashMap<>();
        try {
            String csvUrl = "https://docs.google.com/spreadsheets/d/" + SHEET_ID + "/export?format=csv&gid=1";
            String csv = new String(new URL(csvUrl).openStream().readAllBytes(), "UTF-8");
            for (String line : csv.split("\n")) {
                String[] p = line.split(",");
                if (p.length >= 5 && !p[0].trim().equals("id")) {
                    String id = p[0].trim();
                    String eqLocal = p[1].trim();
                    String flLocal = p[2].trim();
                    String eqVisit = p[3].trim();
                    String flVisit = p[4].trim();
                    Integer gL = null, gV = null;
                    if (p.length >= 7 && !p[5].trim().isEmpty() && !p[6].trim().isEmpty()) {
                        try { gL = Integer.parseInt(p[5].trim()); gV = Integer.parseInt(p[6].trim()); } catch (Exception ignored) {}
                    }
                    if (!eqLocal.isEmpty() && !eqVisit.isEmpty()) {
                        map.put(id, new PartidoElim(eqLocal, flLocal, eqVisit, flVisit, gL, gV));
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("No se pudo leer eliminatorios: " + e.getMessage());
        }
        return map;
    }

    private void agregarEventoGrupo(StringBuilder ics, Map<String, String> p, Resultado res) {
        String id = p.get("id");
        LocalDate fecha = LocalDate.parse(p.get("fecha"));
        LocalTime hora = LocalTime.parse(p.get("horaEDT"));
        ZonedDateTime inicio = ZonedDateTime.of(fecha, hora, ZONA_ET);
        ZonedDateTime fin = inicio.plusHours(2);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");

        boolean esColombia = p.getOrDefault("notas", "").contains("🇨🇴");

        String titulo;
        if (res != null) {
            titulo = p.get("flagLocal") + " " + p.get("local") + " " + res.golesLocal + " - " + res.golesVisitante + " " + p.get("flagVisitante") + " " + p.get("visitante");
        } else {
            titulo = p.get("flagLocal") + " " + p.get("local") + " vs " + p.get("flagVisitante") + " " + p.get("visitante");
        }
        if (esColombia) titulo = "🇨🇴 " + titulo;

        String desc = "📌 Grupo " + p.get("grupo") + "\\n🏟️ " + p.get("estadio") + "\\n" + p.getOrDefault("notas", "");

        ics.append("BEGIN:VEVENT\r\n");
        ics.append("UID:mundial2026-").append(id).append("@mundial\r\n");
        ics.append("DTSTART;TZID=America/Toronto:").append(inicio.format(fmt)).append("\r\n");
        ics.append("DTEND;TZID=America/Toronto:").append(fin.format(fmt)).append("\r\n");
        ics.append("SUMMARY:").append(titulo).append("\r\n");
        ics.append("LOCATION:").append(p.get("estadio")).append("\r\n");
        ics.append("DESCRIPTION:").append(desc).append("\r\n");
        ics.append("CATEGORIES:Grupo ").append(p.get("grupo")).append("\r\n");
        if (esColombia) ics.append("CATEGORIES:Colombia\r\n");
        ics.append("BEGIN:VALARM\r\n");
        ics.append("TRIGGER:-PT60M\r\n");
        ics.append("ACTION:DISPLAY\r\n");
        ics.append("DESCRIPTION:⚽ En 1 hora: ").append(titulo).append("\r\n");
        ics.append("END:VALARM\r\n");
        ics.append("END:VEVENT\r\n");
    }

    private void agregarEventoEliminatorio(StringBuilder ics, Map<String, String> p, PartidoElim elim) {
        String id = p.get("id");
        LocalDate fecha = LocalDate.parse(p.get("fecha"));
        LocalTime hora = LocalTime.parse(p.get("horaEDT"));
        ZonedDateTime inicio = ZonedDateTime.of(fecha, hora, ZONA_ET);
        ZonedDateTime fin = inicio.plusHours(2);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");

        String fase = p.get("fase");
        String estadio = p.get("estadio");

        String titulo;
        if (elim != null && elim.golesLocal != null && elim.golesVisitante != null) {
            titulo = elim.flagLocal + " " + elim.equipoLocal + " " + elim.golesLocal + " - " + elim.golesVisitante + " " + elim.flagVisitante + " " + elim.equipoVisitante;
        } else if (elim != null) {
            titulo = elim.flagLocal + " " + elim.equipoLocal + " vs " + elim.flagVisitante + " " + elim.equipoVisitante;
        } else {
            titulo = "⏳ Por definir";
        }

        boolean esColombia = elim != null && (elim.equipoLocal.contains("Colombia") || elim.equipoVisitante.contains("Colombia"));
        if (esColombia) titulo = "🇨🇴 " + titulo;

        String desc = "🏆 " + fase + "\\n🏟️ " + estadio;

        ics.append("BEGIN:VEVENT\r\n");
        ics.append("UID:mundial2026-").append(id).append("@mundial\r\n");
        ics.append("DTSTART;TZID=America/Toronto:").append(inicio.format(fmt)).append("\r\n");
        ics.append("DTEND;TZID=America/Toronto:").append(fin.format(fmt)).append("\r\n");
        ics.append("SUMMARY:").append(fase).append(" - ").append(titulo).append("\r\n");
        ics.append("LOCATION:").append(estadio).append("\r\n");
        ics.append("DESCRIPTION:").append(desc).append("\r\n");
        ics.append("CATEGORIES:").append(fase).append("\r\n");
        if (esColombia) ics.append("CATEGORIES:Colombia\r\n");
        ics.append("BEGIN:VALARM\r\n");
        ics.append("TRIGGER:-PT60M\r\n");
        ics.append("ACTION:DISPLAY\r\n");
        ics.append("DESCRIPTION:⚽ En 1 hora: ").append(titulo).append("\r\n");
        ics.append("END:VALARM\r\n");
        ics.append("END:VEVENT\r\n");
    }

    static class Resultado {
        int golesLocal, golesVisitante;
        Resultado(int l, int v) { this.golesLocal = l; this.golesVisitante = v; }
    }

    static class PartidoElim {
        String equipoLocal, flagLocal, equipoVisitante, flagVisitante;
        Integer golesLocal, golesVisitante;
        PartidoElim(String eL, String fL, String eV, String fV, Integer gL, Integer gV) {
            this.equipoLocal = eL; this.flagLocal = fL;
            this.equipoVisitante = eV; this.flagVisitante = fV;
            this.golesLocal = gL; this.golesVisitante = gV;
        }
    }
}