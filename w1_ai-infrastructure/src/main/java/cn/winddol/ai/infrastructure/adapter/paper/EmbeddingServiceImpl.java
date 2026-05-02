package cn.winddol.ai.infrastructure.adapter.paper;

import cn.winddol.ai.paper.api.IEmbeddingService;
import dev.langchain4j.model.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

@Service
public class EmbeddingServiceImpl implements IEmbeddingService {

    private final EmbeddingModel embeddingModel;

    public EmbeddingServiceImpl(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    @Override
    public float[] embed(String text) {
        return embeddingModel.embed(text).content().vector();
    }
}
