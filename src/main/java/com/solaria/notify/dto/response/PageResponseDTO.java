package com.solaria.notify.dto.response;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PageResponseDTO<T> {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Itens da página")
    private List<T> content;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Número da página, começando em 0", example = "0")
    private int page;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Tamanho da página", example = "20")
    private int size;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Total de itens", example = "1")
    private long totalElements;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Total de páginas", example = "1")
    private int totalPages;
}
