package cn.winddol.ai.trigger.http;

import cn.winddol.ai.api.IScientificResearchController;
import cn.winddol.ai.api.response.Response;
import cn.winddol.ai.paper.api.IScientificResearchTools;
import cn.winddol.ai.types.enums.ResponseCode;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class ScientificResearchController implements IScientificResearchController {

    @Resource
    private IScientificResearchTools scientificResearchTools;

    @Override
    public Response<String> searchLibrary(String query, Long paperId, Double threshold) {
        try {
            String result = scientificResearchTools.searchLibrary(query, paperId, threshold);
            return Response.<String>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(result)
                    .build();
        } catch (Exception e) {
            log.error("searchLibrary failed", e);
            return Response.<String>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(ResponseCode.UN_ERROR.getInfo())
                    .data(e.getMessage())
                    .build();
        }
    }

    @Override
    public Response<String> getPaperOutline(Long paperId) {
        try {
            String result = scientificResearchTools.getPaperOutline(paperId);
            return Response.<String>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(result)
                    .build();
        } catch (Exception e) {
            log.error("getPaperOutline failed for paperId: {}", paperId, e);
            return Response.<String>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(ResponseCode.UN_ERROR.getInfo())
                    .data(e.getMessage())
                    .build();
        }
    }

    @Override
    public Response<String> readSection(String sectionUuid) {
        try {
            String result = scientificResearchTools.readSection(sectionUuid);
            return Response.<String>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(result)
                    .build();
        } catch (Exception e) {
            log.error("readSection failed for sectionUuid: {}", sectionUuid, e);
            return Response.<String>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(ResponseCode.UN_ERROR.getInfo())
                    .data(e.getMessage())
                    .build();
        }
    }

    @Override
    public Response<String> lookupReference(Long paperId, String refIndex) {
        try {
            String result = scientificResearchTools.lookupReference(paperId, refIndex);
            return Response.<String>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(result)
                    .build();
        } catch (Exception e) {
            log.error("lookupReference failed for paperId: {}, refIndex: {}", paperId, refIndex, e);
            return Response.<String>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(ResponseCode.UN_ERROR.getInfo())
                    .data(e.getMessage())
                    .build();
        }
    }

    @Override
    public Response<String> checkPaperRelations(Long paperId) {
        try {
            String result = scientificResearchTools.checkPaperRelations(paperId);
            return Response.<String>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(result)
                    .build();
        } catch (Exception e) {
            log.error("checkPaperRelations failed for paperId: {}", paperId, e);
            return Response.<String>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(ResponseCode.UN_ERROR.getInfo())
                    .data(e.getMessage())
                    .build();
        }
    }

    @Override
    public Response<String> findPapers(String query, Double threshold) {
        try {
            String result = scientificResearchTools.findPapers(query, threshold);
            return Response.<String>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(result)
                    .build();
        } catch (Exception e) {
            log.error("findPapers failed for query: {}", query, e);
            return Response.<String>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(ResponseCode.UN_ERROR.getInfo())
                    .data(e.getMessage())
                    .build();
        }
    }

    @Override
    public Response<String> getTopCitedReferences(Integer limit) {
        try {
            String result = scientificResearchTools.getTopCitedReferences(limit);
            return Response.<String>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(result)
                    .build();
        } catch (Exception e) {
            log.error("getTopCitedReferences failed for limit: {}", limit, e);
            return Response.<String>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(ResponseCode.UN_ERROR.getInfo())
                    .data(e.getMessage())
                    .build();
        }
    }
}
