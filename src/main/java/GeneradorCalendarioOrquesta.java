import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.*;
import java.lang.reflect.Type;
import java.net.URL;
import java.time.*;
import java.util.*;

public class GeneradorCalendarioOrquesta {

    private static final ZoneId ZONA_MONTRAL = ZoneId.of("America/Toronto");
    private static final String SHEET_ID = "1qLNQ8sFIY68WNv0PAVaeQ0b0CoTa3w_ozVsNXXL0m3g";  // Tu hoja

    public static void main(String[] args) throws Exception {
        Gson gson = new Gson();

        // Cargar partidos desde JSON
        Map<String, List<Partido>> data = gson.fromJson(
            new FileReader("src/main/resources/partidos.json"),
            new TypeToken<Map<String, List<Partido>>>() {}.getType()
        );

        List<Partido> partidos = data.get("partidosGrupos");

        // Cargar resultados desde tu Google Sheet
        Map<String, Resultado> resultados = cargarResultadosDesdeSheets();

        StringBuilder ics = new StringBuilder();
        ics.append("BEGIN:VCALENDAR\n");
        ics.append("VERSION:2.0\n");
        ics.append("PRODID:-//Mundial2026 Orquesta//ES\n");
        ics.append("CALSCALE:GREGORIAN\n\n");

        for (Partido p : partidos) {
            agregarEvento(ics, p, resultados.get(p.id));
        }

        ics.append("END:VCALENDAR");

        try (FileWriter writer = new FileWriter("Mundial_2026_Colombia.ics")) {
            writer.write(ics.toString());
        }

        System.out.println("✅ ¡Éxito! Calendario generado desde Google Sheets.");
        System.out.println("Archivo creado: Mundial_2026_Colombia.ics");
        System.out.println("Actualiza los goles en tu Google Sheet y vuelve a ejecutar este comando.");
    }

    private static Map<String, Resultado> cargarResultadosDesdeSheets() {
        Map<String, Resultado> map = new HashMap<>();
        try {
            String csvUrl = "https://docs.google.com/spreadsheets/d/" + SHEET_ID + "/export?format=csv";
            String csvContent = new String(new URL(csvUrl).openStream().readAllBytes());

            String[] lines = csvContent.split("\n");
            for (int i = 1; i < lines.length; i++) {  // saltar encabezado
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
            System.out.println("Resultados cargados desde Google Sheets.");
        } catch (Exception e) {
            System.out.println("No se pudo leer la Google Sheet (usando solo fixtures).");
        }
        return map;
    }

    private static void agregarEvento(StringBuilder ics, Partido p, Resultado res) throws Exception {
        LocalDate fecha = LocalDate.parse(p.fecha);
        LocalTime hora = LocalTime.parse(p.horaEDT);
        ZonedDateTime inicio = ZonedDateTime.of(fecha, hora, ZONA_MONTRAL);
        ZonedDateTime fin = inicio.plusHours(2);

        String uid = "mundial2026-" + p.id + "@xai";

        String titulo;
        if (res != null) {
            titulo = p.flagLocal + " " + p.local + " " + res.golesLocal + " - " + res.golesVisitante + " " + p.flagVisitante + " " + p.visitante;
        } else {
            titulo = p.flagLocal + " " + p.local + " vs " + p.flagVisitante + " " + p.visitante;
        }

        ics.append("BEGIN:VEVENT\n");
        ics.append("UID:").append(uid).append("\n");
        ics.append("DTSTART:").append(inicio.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss"))).append("\n");
        ics.append("DTEND:").append(fin.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss"))).append("\n");
        ics.append("SUMMARY:").append(titulo).append("\n");
        ics.append("LOCATION:").append(p.estadio).append("\n");
        ics.append("DESCRIPTION:Grupo " + p.grupo 
                 + "\\nEstadio: " + p.estadio 
                 + "\\n" + p.notas).append("\n");
        ics.append("END:VEVENT\n\n");
    }

    static class Partido {
        String id;
        String fecha;
        String horaEDT;
        String grupo;
        String local;
        String flagLocal;
        String visitante;
        String flagVisitante;
        String estadio;
        String notas;
    }

    static class Resultado {
        int golesLocal;
        int golesVisitante;
        Resultado(int l, int v) {
            this.golesLocal = l;
            this.golesVisitante = v;
        }
    }
}