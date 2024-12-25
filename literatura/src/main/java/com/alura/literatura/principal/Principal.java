package com.alura.literatura.principal;

import com.alura.literatura.model.Author;
import com.alura.literatura.model.DatosLibros;
import com.alura.literatura.model.GutendexResponse;
import com.alura.literatura.model.Libro;
import com.alura.literatura.repository.LibroRepository;
import com.alura.literatura.service.ConsumoAPI;
import com.alura.literatura.service.ConvierteDatos;

import java.util.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;


@Component
public class Principal {

    @Autowired
    private LibroRepository repositorio;

    private Scanner teclado = new Scanner(System.in);

    private ConsumoAPI consumoAPI = new ConsumoAPI();

    private final String URL_BASE = "https://gutendex.com/books/";

    private ConvierteDatos conversor = new ConvierteDatos();

    public Principal(LibroRepository repository) {

        this.repositorio = repository;

    }

    public void muestraElMenu() {

        int opcion = -1;

        while (opcion != 0) {

            var menu = """
                    1 - Buscar libro por titulo
                    2 - Listar libros registrados
                    3 - Listar autores registrados
                    4 - Listar autores vivos en un determinado año
                    5 - Listar libros por idioma
                    0 - Salir
                    """;

            System.out.println(menu);

            opcion = leerOpcion();

            switch (opcion) {
                case 1:
                    getDatosLibro();
                    break;
                case 2:
                    listarLibrosRegistrados();
                    break;
                case 3:
                    listarAutoresRegistrados();
                    break;
                case 4:
                    listarAutoresVivosEnAnio();
                    break;
                case 5:
                    listarLibrosPorIdioma();
                    break;
                case 0:
                    System.out.println("Gracias por usar el programa");
                    break;
                default:
                    System.out.println("Opción no válida");
                    break;
            }
        }
    }

    private int leerOpcion() {
        int opcion = -1;
        boolean entradaValida = false;

        while (!entradaValida) {
            try {
                System.out.print("Seleccione una opción: ");
                opcion = teclado.nextInt();
                teclado.nextLine(); // Limpia el buffer
                entradaValida = true;
            } catch (InputMismatchException e) {
                System.out.println("Entrada inválida. Por favor, ingrese un número.");
                teclado.nextLine(); // Limpia el buffer en caso de entrada inválida
            }
        }

        return opcion;
    }

    private DatosLibros getDatosLibro() {
        // Solicitar el título del libro al usuario
        System.out.println("Por favor, escribe el título del libro que deseas buscar:");
        var titulo = teclado.nextLine();

        // Codificar el título para la URL
        String tituloEncoded = URLEncoder.encode(titulo, StandardCharsets.UTF_8);

        // Construir la URL
        var url = URL_BASE + "?search=" + tituloEncoded;

        // Obtener el JSON de la API
        var json = consumoAPI.obterDados(url);

        // Convertir la respuesta a la clase DatosLibros
        DatosLibros datos = conversor.obtenerDatos(json, GutendexResponse.class)
                .results()
                .stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No se encontraron libros con ese título"));

        // Buscar si el libro ya existe en la base de datos
        List<Libro> librosExistentes = repositorio.findByTituloContainingIgnoreCase(titulo);

        if (librosExistentes.isEmpty()) {
            // Si el libro no existe, lo guardamos
            Libro nuevoLibro = new Libro(datos);
            repositorio.save(nuevoLibro);

            // Mostrar la información del libro guardado
            System.out.printf("Libro guardado exitosamente:\nTítulo: %s | Autor: %s | Idioma: %s | Total de descargas: %d\n",
                    nuevoLibro.getTitulo(), nuevoLibro.getAutores(), nuevoLibro.getIdiomas(), nuevoLibro.getTotalDeDescargas());
        } else {
            // Si el libro ya existe en la base de datos
            System.out.printf("El libro ya existe en la base de datos:\nTítulo: %s | Autor: %s | Idioma: %s | Total de descargas: %d\n",
                    librosExistentes.get(0).getTitulo(), librosExistentes.get(0).getAutores(),
                    librosExistentes.get(0).getIdiomas(), librosExistentes.get(0).getTotalDeDescargas());
        }

        return datos;
    }









    private void listarLibrosRegistrados() {
        // Obtener la lista de libros desde el repositorio
        List<Libro> librosRegistrados = repositorio.findAll();

        if (librosRegistrados.isEmpty()) {
            System.out.println("No hay libros registrados en la base de datos.");
        } else {
            System.out.println("Lista de libros registrados en la base de datos:");
            librosRegistrados.forEach(libro ->
                    System.out.printf("Título: %s | Autor: %s | Idioma: %s | Total de descargas: %d\n",
                            libro.getTitulo(), libro.getAutores(), libro.getIdiomas(), libro.getTotalDeDescargas())
            );
        }
    }

    private void listarAutoresRegistrados() {
        // Obtener la lista de libros desde el repositorio
        List<Libro> librosRegistrados = repositorio.findAll();

        if (librosRegistrados.isEmpty()) {
            System.out.println("No hay autores registrados en la base de datos.");
        } else {
            // Obtener los nombres únicos de autores
            Set<String> autoresUnicos = librosRegistrados.stream()
                    .map(Libro::getAutores) // Extraer los nombres de los autores
                    .flatMap(autores -> Arrays.stream(autores.split("\\."))) // Dividir por comas si hay múltiples autores
                    .map(String::trim) // Quitar espacios en blanco
                    .collect(Collectors.toSet()); // Crear un conjunto para eliminar duplicados

            if (autoresUnicos.isEmpty()) {
                System.out.println("No hay información de autores en los libros registrados.");
            } else {
                System.out.println("Autores registrados en la base de datos:");
                autoresUnicos.forEach(autor -> System.out.println("- " + autor));
            }
        }
    }

    private void listarAutoresVivosEnAnio() {
        System.out.println("Ingrese el año para buscar autores vivos:");
        int anioConsulta;

        try {
            anioConsulta = Integer.parseInt(teclado.nextLine());
        } catch (NumberFormatException e) {
            System.out.println("Por favor, ingrese un año válido.");
            return;
        }

        // Obtener todos los libros registrados
        List<Libro> librosRegistrados = repositorio.findAll();

        if (librosRegistrados.isEmpty()) {
            System.out.println("No hay libros registrados en la base de datos.");
            return;
        }

        // Necesitamos volver a consultar la API para obtener los datos de nacimiento y muerte
        Set<String> autoresEncontrados = new HashSet<>();

        for (Libro libro : librosRegistrados) {
            // Construir la URL de búsqueda para cada libro
            String tituloEncoded = URLEncoder.encode(libro.getTitulo(), StandardCharsets.UTF_8);
            String url = URL_BASE + "?search=" + tituloEncoded;

            // Obtener datos de la API
            try {
                String json = consumoAPI.obterDados(url);
                GutendexResponse response = conversor.obtenerDatos(json, GutendexResponse.class);

                // Procesar cada libro encontrado
                for (DatosLibros datos : response.results()) {
                    if (datos.autores() != null) {
                        for (Author autor : datos.autores()) {
                            // Verificar si el autor estaba vivo en el año consultado
                            if (estabaVivo(autor, anioConsulta)) {
                                String infoAutor = String.format("%s (Nacimiento: %s - Muerte: %s)",
                                        autor.name(),
                                        autor.birthYear() != null ? autor.birthYear().toString() : "Desconocido",
                                        autor.deathYear() != null ? autor.deathYear().toString() : "Desconocido");
                                autoresEncontrados.add(infoAutor);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                System.out.printf("Error al procesar el libro '%s': %s%n", libro.getTitulo(), e.getMessage());
            }
        }

        // Mostrar resultados
        if (autoresEncontrados.isEmpty()) {
            System.out.printf("No se encontraron autores vivos en el año %d%n", anioConsulta);
        } else {
            System.out.printf("Autores vivos en el año %d:%n", anioConsulta);
            autoresEncontrados.forEach(autor -> System.out.println("- " + autor));
        }
    }

    private boolean estabaVivo(Author autor, int anio) {
        // Si no tenemos información de nacimiento, no podemos determinar si estaba vivo
        if (autor.birthYear() == null) {
            return false;
        }

        // Si el autor nació después del año consultado, no estaba vivo
        if (autor.birthYear() > anio) {
            return false;
        }

        // Si no tenemos información de muerte o murió después del año consultado, estaba vivo
        return autor.deathYear() == null || autor.deathYear() >= anio;
    }

    private void listarLibrosPorIdioma() {
        // Obtener todos los libros registrados
        List<Libro> librosRegistrados = repositorio.findAll();

        if (librosRegistrados.isEmpty()) {
            System.out.println("No hay libros registrados en la base de datos.");
            return;
        }

        // Obtener todos los códigos de idioma disponibles
        Set<String> idiomasDisponibles = librosRegistrados.stream()
                .map(Libro::getIdiomas)
                .flatMap(idiomas -> Arrays.stream(idiomas.split(",")))
                .map(String::trim)
                .collect(Collectors.toSet());

        // Mostrar los códigos de idioma disponibles
        System.out.println("\nCódigos de idioma disponibles:");
        idiomasDisponibles.forEach(idioma -> {
            String descripcion = switch (idioma.toLowerCase()) {
                case "es" -> "Español";
                case "en" -> "Inglés";
                case "fr" -> "Francés";
                case "de" -> "Alemán";
                case "it" -> "Italiano";
                case "pt" -> "Portugués";
                default -> idioma;
            };
            System.out.println("- " + idioma + " (" + descripcion + ")");
        });

        // Solicitar al usuario que elija un código de idioma
        System.out.println("\nIngrese el código del idioma para filtrar los libros (ejemplo: es, en, fr):");
        String idiomaSeleccionado = teclado.nextLine().trim().toLowerCase();

        // Filtrar libros por el código de idioma seleccionado
        List<Libro> librosFiltrados = librosRegistrados.stream()
                .filter(libro -> Arrays.stream(libro.getIdiomas().split(","))
                        .map(String::trim)
                        .map(String::toLowerCase)
                        .anyMatch(idioma -> idioma.equals(idiomaSeleccionado)))
                .collect(Collectors.toList());

        // Mostrar resultados
        if (librosFiltrados.isEmpty()) {
            System.out.println("\nNo se encontraron libros en el idioma con código: " + idiomaSeleccionado);
        } else {
            String nombreIdioma = switch (idiomaSeleccionado) {
                case "es" -> "español";
                case "en" -> "inglés";
                case "fr" -> "francés";
                case "de" -> "alemán";
                case "it" -> "italiano";
                case "pt" -> "portugués";
                default -> idiomaSeleccionado;
            };

            System.out.println("\nLibros encontrados en " + nombreIdioma + ":");
            librosFiltrados.forEach(libro ->
                    System.out.printf("Título: %s | Autor: %s | Total de descargas: %d%n",
                            libro.getTitulo(),
                            libro.getAutores(),
                            libro.getTotalDeDescargas())
            );

            // Mostrar total de libros encontrados
            System.out.printf("%nTotal de libros en %s: %d%n",
                    nombreIdioma,
                    librosFiltrados.size());
        }
    }


}




