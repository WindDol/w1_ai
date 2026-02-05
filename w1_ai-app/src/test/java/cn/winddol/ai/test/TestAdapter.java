package cn.winddol.ai.test;

import cn.winddol.ai.domain.paperTools.adapter.ai.ISymbolExtractor;
import cn.winddol.ai.domain.paperTools.model.valobj.SymbolDefinition;
import cn.winddol.ai.domain.paperTools.service.IPaperEnrichmentService;
import dev.ai4j.openai4j.Json;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@Slf4j
@SpringBootTest
public class TestAdapter {

    @Resource
    private ISymbolExtractor extractor;
    @Resource
    private IPaperEnrichmentService enrichmentService;

    @Test
    public void testAi() throws Exception {
        String context = """
                Parametrize the one-parameter family of Möbius transformations as
                $$M_t(w) = \\frac{e^{i\\psi}w + \\alpha}{1 + \\bar{\\alpha}e^{i\\psi}w},$$
                (17)
                where $|\\alpha(t)| < 1$ and $\\psi(t) \\in \\mathbb{R}$, and let
                $$w_j = e^{i\\theta_j}.$$
                (18)
                To verify that Eq. (17) gives an exact solution of Eq. (3)—subject to the constraint that the Möbius parameters $\\alpha(t)$ and $\\psi(t)$ obey appropriate ODEs to be determined—we compute the time derivative of $\\phi_j(t) = -i \\log M_t(w_j)$, keeping in mind that $w_j$ is constant,
                $$\\dot{\\phi}_j = \\frac{i\\dot{\\psi}e^{i\\psi}w_j - i\\dot{\\alpha}}{e^{i\\psi}w_j + \\alpha} + \\frac{(i\\dot{\\bar{\\alpha}} - \\bar{\\alpha}\\dot{\\psi})e^{i\\psi}w_j}{1 + \\bar{\\alpha}e^{i\\psi}w_j}.$$
                (19)
                From Eq. (15), we get
                $$e^{i\\psi}w_j = \\frac{e^{i\\phi_j} - \\alpha}{1 - \\bar{\\alpha}e^{i\\phi_j}},$$
                (20)
                which when substituted into Eq. (19) yields
                $$\\dot{\\phi}_j = Re^{i\\phi_j} + \\frac{\\dot{\\psi} + i\\dot{\\bar{\\alpha}}\\alpha - \\alpha(i\\dot{\\bar{\\alpha}} - \\bar{\\alpha}\\dot{\\psi})}{1 - |\\alpha|^2} + \\bar{R}e^{-i\\phi_j},$$
                (21)
                where $R = (i\\dot{\\bar{\\alpha}} - \\bar{\\alpha}\\dot{\\psi})/(1 - |\\alpha|^2)$.
                Note that Eq. (21) falls precisely into the algebraic form required by Eq. (3). Thus, to derive the desired ODEs for $\\alpha(t)$ and $\\psi(t)$, we now subtract Eq. (21) from Eq. (3) to obtain $N$ equations of the form $0 = C_1e^{i\\phi_j} + C_0 + C_{-1}e^{-i\\phi_j}$ for $j=1,...,N$. If the system contains at least three distinct oscillator phases, then $C_1$, $C_0$, and $C_{-1}$ must generically be zero. Explicitly,
                $$f = \\frac{i\\dot{\\alpha} - \\bar{\\alpha}\\dot{\\psi}}{1 - |\\alpha|^2}, \\quad g = \\frac{\\dot{\\psi} + i\\dot{\\bar{\\alpha}}\\alpha - \\alpha(i\\dot{\\bar{\\alpha}} - \\bar{\\alpha}\\dot{\\psi})}{1 - |\\alpha|^2}.$$
                (22)
                The system (22) can be algebraically rearranged to give
                $$\\dot{\\alpha} = i(f\\alpha^2 + g\\alpha + \\bar{f}),$$
                (23a)
                $$\\dot{\\psi} = f\\alpha + g + \\bar{f}\\bar{\\alpha}.$$
                (23b)
                Equations (23a) and (23b) have been derived previously; they appear as Eqs. (10) and (11), respectively, in Pikovsky and Rosenblum's work,<sup>24</sup> where they were derived by applying the transformation equation (2). Both their approach and the one above are certainly quick and clean, but they require us to guess the transformation ahead of time, and reveal little about why this transformation works.
                Incidentally, observe that under the change of variables $z_j = e^{i\\phi_j}$, Eq. (3) becomes
                $$\\dot{z}_j = i(fz_j^2 + gz_j + \\bar{f}).$$
                (24)
                Equation (24) is a Riccati equation with the form of Eq. (23a)—another coincidence that seems a bit surprising when approached this way. In Sec. III B, we will see how these Riccati equations emerge naturally from the infinitesimal generators of the Möbius group.
                """;
        String title = "Identical phase oscillators with global sinusoidal coupling evolve by Möbius group action";
        try{
            List<SymbolDefinition> symbolDefinitions = extractor.extractFromSection(title,context);
            log.info(Json.toJson(symbolDefinitions));
        }catch (Exception e){
            throw new Exception(e.getMessage());
        }

    }
    @Test
    public void testEnrichment() throws Exception {
        enrichmentService.symbolExtractionAndStorage(9L);
    }


}
