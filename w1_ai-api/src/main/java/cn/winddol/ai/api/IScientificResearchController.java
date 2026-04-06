package cn.winddol.ai.api;

import cn.winddol.ai.api.response.Response;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Scientific Research Tools API", description = "Endpoints for academic data center and agent access")
@RequestMapping("/api/v1/research-tools")
public interface IScientificResearchController {

    /**
     * Search the paper library.
     * @param query the keyword/sentence
     * @param paperId optional: set specific ID to search within one paper, or set NULL to search the entire library
     * @param threshold optional (0.35-0.7)
     * @return candidate sections, symbols, and references in Markdown format
     */
    @Operation(summary = "Search library", description = "Searches the paper library and returns formatted candidate sections, symbols, and references.")
    @GetMapping("/library/search")
    Response<String> searchLibrary(
            @RequestParam("query") String query,
            @RequestParam(value = "paperId", required = false) Long paperId,
            @RequestParam(value = "threshold", required = false) Double threshold
    );

    /**
     * Get the hierarchical outline (table of contents) of a specific paper.
     * @param paperId the paperId (e.g., 7)
     * @return Outline JSON string
     */
    @Operation(summary = "Get paper outline", description = "Returns the hierarchical outline (table of contents) for a given paper.")
    @GetMapping("/papers/{paperId}/outline")
    Response<String> getPaperOutline(@PathVariable("paperId") Long paperId);

    /**
     * Read the full content of a specific section.
     * @param sectionUuid the section UUID
     * @return section content with context and local symbol definitions in Markdown format
     */
    @Operation(summary = "Read section", description = "Reads the full content of a specific section by its UUID.")
    @GetMapping("/sections/{sectionUuid}")
    Response<String> readSection(@PathVariable("sectionUuid") String sectionUuid);

    /**
     * Look up the specific details (Title and Abstract) of a reference cited in the paper.
     * @param paperId paperId
     * @param refIndex refIndex (e.g., '24')
     * @return reference details in Markdown format
     */
    @Operation(summary = "Lookup reference", description = "Look up the specific details (Title and Abstract) of a reference cited in the paper.")
    @GetMapping("/papers/{paperId}/reference/{refIndex}")
    Response<String> lookupReference(
            @PathVariable("paperId") Long paperId,
            @PathVariable("refIndex") String refIndex
    );

    /**
     * Check for inter-paper relationships, such as conflicts, supports, or extensions.
     * @param paperId paperId
     * @return relationship info in Markdown format
     */
    @Operation(summary = "Check paper relations", description = "Check for inter-paper relationships, such as conflicts, supports, or extensions.")
    @GetMapping("/papers/{paperId}/relations")
    Response<String> checkPaperRelations(@PathVariable("paperId") Long paperId);

    /**
     * Search for specific papers in the library metadata (Title, Authors, Abstract, Year).
     * @param query the keyword/sentence
     * @param threshold optional (0.35-0.7)
     * @return list of Paper IDs and Titles
     */
    @Operation(summary = "Find papers", description = "Search for specific papers in the library metadata (Title, Authors, Abstract, Year).")
    @GetMapping("/papers/find")
    Response<String> findPapers(
            @RequestParam("query") String query,
            @RequestParam(value = "threshold", required = false) Double threshold
    );

    /**
     * Identify the most influential references within the private library.
     * @param limit optional, default 5
     * @return top cited references in Markdown format
     */
    @Operation(summary = "Get top cited references", description = "Identify the most influential references within the private library.")
    @GetMapping("/references/top-cited")
    Response<String> getTopCitedReferences(
            @RequestParam(value = "limit", required = false, defaultValue = "5") Integer limit
    );
}
