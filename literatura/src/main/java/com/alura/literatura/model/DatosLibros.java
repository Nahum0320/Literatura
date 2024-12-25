package com.alura.literatura.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DatosLibros(
        @JsonAlias("title") String titulo,
        @JsonAlias("authors") List<Author> autores,
        @JsonAlias("languages") List<String> idiomas,
        @JsonAlias("download_count") int TotalDeDescargas) {
    public String getAutoresAsString() {
        if (autores == null || autores.isEmpty()) {
            return "";
        }
        return autores.stream()
                .map(author -> author.name())
                .reduce((a, b) -> a + ", " + b)
                .orElse("");
    }

    public String getIdiomasAsString() {
        if (idiomas == null || idiomas.isEmpty()) {
            return "";
        }
        return String.join(", ", idiomas);
    }
}



